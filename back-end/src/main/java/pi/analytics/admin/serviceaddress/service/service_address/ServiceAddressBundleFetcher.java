/*
 * Copyright (c) 2017 Practice Insight Pty Ltd.
 */

package pi.analytics.admin.serviceaddress.service.service_address;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import pi.admin.service_address_sorting.generated.Agent;
import pi.admin.service_address_sorting.generated.NonLawFirm;
import pi.admin.service_address_sorting.generated.SamplePatentApp;
import pi.admin.service_address_sorting.generated.ServiceAddressBundle;
import pi.ip.data.relational.generated.GetLawFirmByIdRequest;
import pi.ip.data.relational.generated.GetSamplePatentAppsForServiceAddressRequest;
import pi.ip.data.relational.generated.GetServiceAddressByIdRequest;
import pi.ip.data.relational.generated.LawFirmDbServiceGrpc.LawFirmDbServiceBlockingStub;
import pi.ip.data.relational.generated.ServiceAddressServiceGrpc.ServiceAddressServiceBlockingStub;
import pi.ip.generated.es.LocationRecord;
import pi.ip.generated.es.ThinLawFirmServiceAddressReadServiceGrpc.ThinLawFirmServiceAddressReadServiceBlockingStub;
import pi.ip.generated.es.ThinLawFirmServiceAddressRecord;
import pi.ip.generated.es.ThinServiceAddressRecord;
import pi.ip.proto.generated.LangType;
import pi.ip.proto.generated.LawFirm;
import pi.ip.proto.generated.ServiceAddress;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static pi.analytics.admin.serviceaddress.service.service_address.ServiceAddressUtils.isAssignedToLawFirm;
import static pi.analytics.admin.serviceaddress.service.service_address.ServiceAddressUtils.needsSorting;

/**
 * @author shane.xie@practiceinsight.io
 */
@Singleton
public class ServiceAddressBundleFetcher {

  private static final Logger log = LoggerFactory.getLogger(ServiceAddressBundleFetcher.class);

  @Inject
  LawFirmDbServiceBlockingStub lawFirmDbServiceBlockingStub;

  @Inject
  ServiceAddressServiceBlockingStub serviceAddressServiceBlockingStub;

  @Inject
  ThinLawFirmServiceAddressReadServiceBlockingStub thinLawFirmServiceAddressReadServiceBlockingStub;

  @Inject
  Translator translator;

  public ServiceAddressBundle fetch(final ServiceAddress serviceAddress) {
    return createServiceAddressBundle
        .andThen(addTranslationIfNecessary)
        .andThen(addAgentSuggestions)
        .andThen(addSamplePatentApplications)
        .apply(serviceAddress);
  }

  @VisibleForTesting
  final Function<ServiceAddress, ServiceAddressBundle> createServiceAddressBundle = serviceAddress ->
    ServiceAddressBundle
        .newBuilder()
        .setServiceAddressToSort(serviceAddress)
        .build();

  @VisibleForTesting
  final Function<ServiceAddressBundle, ServiceAddressBundle> addTranslationIfNecessary = bundle -> {
    final Optional<LangType> sourceLanguage = Optional.ofNullable(bundle.getServiceAddressToSort().getLanguageType());
    if (!sourceLanguage.isPresent() || sourceLanguage.get() == LangType.WESTERN_SCRIPT) {
      // We can't translate or no translation required
      return bundle;
    }
    final String textToTranslate = bundle.getServiceAddressToSort().getAddress();
    try {
      return ServiceAddressBundle
          .newBuilder(bundle)
          .setEnTranslation(translator.toEn(textToTranslate, sourceLanguage.get()))
          .build();
    } catch (Exception e) {
      // Don't fail if the translation service is not available
      log.error("Unable to translate service address {}", textToTranslate, e);
      return bundle;
    }
  };

  @VisibleForTesting
  final Function<ServiceAddressBundle, ServiceAddressBundle> addAgentSuggestions = bundle -> {
    final ServiceAddress serviceAddress = bundle.getServiceAddressToSort();

    final ThinServiceAddressRecord getSuggestionsRequest =
        ThinServiceAddressRecord
            .newBuilder()
            .setNameAddress(serviceAddress.getName() + " " + serviceAddress.getAddress())
            .setCountry(serviceAddress.getCountry())
            .setLoc(
                LocationRecord
                    .newBuilder()
                    .setLon((float) serviceAddress.getLongitude())
                    .setLat((float) serviceAddress.getLatitude())
            )
            .build();

    final List<Agent> suggestedAgents =
        thinLawFirmServiceAddressReadServiceBlockingStub
            // Fetch suggestions from Elasticsearch
            .suggestSimilarThinLawFirmServiceAddressRecord(getSuggestionsRequest)
            .getSuggestionsList()
            .stream()

            // Convert to service address from primary source and sanity check. The ES index might not be in sync with db.
            .map(this::fetchServiceAddress)
            .map(this::pruneUnsortedServiceAddresses)
            // When sorting an already sorted service address, we don't want to show it in the suggestions.
            .map(s -> pruneSuggestionsForSameServiceAddress(s, bundle.getServiceAddressToSort()))
            .filter(Optional::isPresent)
            .map(Optional::get)

            // Group by law firm, preserving suggestion list order
            // Service addresses remain ungrouped as long as the suggestions list has no duplicates
            .collect(groupingBy(this::getAgentGroupingKey, LinkedHashMap::new, toList()))
            .entrySet()
            .stream()

            // Build Agent details
            .map(this::buildAgent)
            .collect(toList());

    return ServiceAddressBundle
        .newBuilder(bundle)
        .addAllSuggestedAgents(suggestedAgents)
        .build();
  };

  @VisibleForTesting
  final Function<ServiceAddressBundle, ServiceAddressBundle> addSamplePatentApplications = bundle -> {
    final List<SamplePatentApp> samplePatentApps =
        serviceAddressServiceBlockingStub
            .getSamplePatentAppsForServiceAddress(
                GetSamplePatentAppsForServiceAddressRequest
                    .newBuilder()
                    .setServiceAddressId(bundle.getServiceAddressToSort().getServiceAddressId())
                    .setLimit(5)
                    .build()
            )
            .getSamplePatentAppsList()
            .stream()
            .map(p -> SamplePatentApp
                .newBuilder()
                .setAppNum(p.getAppNum())
                .addAllApplicants(p.getApplicantsList())
                .build()
            )
            .collect(toList());

    return ServiceAddressBundle
        .newBuilder(bundle)
        .addAllSamplePatentApps(samplePatentApps)
        .build();
  };

  private Optional<ServiceAddress> fetchServiceAddress(final ThinLawFirmServiceAddressRecord thinLawFirmServiceAddress) {
    try {
    return Optional.of(
        serviceAddressServiceBlockingStub
            .getServiceAddressById(
                GetServiceAddressByIdRequest
                    .newBuilder()
                    .setServiceAddressId(thinLawFirmServiceAddress.getServiceAddress().getId())
                    .build()
            )
        );
    } catch (StatusRuntimeException sre) {
      if (sre.getStatus().equals(Status.NOT_FOUND)) {
        return Optional.empty();
      }
      // Any other status is an error
      throw sre;
    }
  }

  private Optional<ServiceAddress> pruneUnsortedServiceAddresses(final Optional<ServiceAddress> serviceAddress) {
    return serviceAddress.flatMap(sa -> needsSorting(sa) ? Optional.empty() : Optional.of(sa));
  }

  private Optional<ServiceAddress> pruneSuggestionsForSameServiceAddress(final Optional<ServiceAddress> suggestion,
                                                                         final ServiceAddress serviceAddressToBeSorted) {
    return suggestion.flatMap(s ->
        s.getServiceAddressId() == serviceAddressToBeSorted.getServiceAddressId() ? Optional.empty() : Optional.of(s)
    );
  }

  private String getAgentGroupingKey(final ServiceAddress serviceAddress) {
    if (isAssignedToLawFirm(serviceAddress)) {
      return "lf_" + String.valueOf(serviceAddress.getLawFirmId().getValue());
    } else {
      return "sa_" + String.valueOf(serviceAddress.getServiceAddressId());
    }
  }

  @VisibleForTesting
  Agent buildAgent(final Map.Entry<String, List<ServiceAddress>> entry) {
    final Agent.Builder agentBuilder = Agent.newBuilder();
    final List<ServiceAddress> serviceAddresses = entry.getValue();

    // If there are multiple service addresses, they will all refer to the same law firm.
    // Inspecting the first one will do.
    if (!isAssignedToLawFirm(serviceAddresses.get(0))) {
      // Not a law firm. There will only be one service address in this list
      agentBuilder.setNonLawFirm(NonLawFirm.newBuilder().setName(serviceAddresses.get(0).getName()));
    } else {
      // This is a law firm. Fetch its details.
      final long lawFirmId = serviceAddresses.get(0).getLawFirmId().getValue();
      final GetLawFirmByIdRequest getLawFirmRequest = GetLawFirmByIdRequest.newBuilder().setLawFirmId(lawFirmId).build();
      final LawFirm lawFirm = lawFirmDbServiceBlockingStub.getLawFirmById(getLawFirmRequest).getLawFirm();
      agentBuilder.setLawFirm(lawFirm);
    }
    agentBuilder.addAllServiceAddresses(serviceAddresses);
    return agentBuilder.build();
  }
}
