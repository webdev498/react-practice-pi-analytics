/*
 * Copyright (c) 2017 Practice Insight Pty Ltd.
 */

package pi.analytics.admin.serviceaddress.service.unsorted_service_address;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import pi.admin.service_address_sorting.generated.Agent;
import pi.admin.service_address_sorting.generated.NonLawFirm;
import pi.admin.service_address_sorting.generated.ServiceAddressBundle;
import pi.analytics.admin.serviceaddress.service.QueuedServiceAddress;
import pi.ip.data.relational.generated.GetLawFirmByIdRequest;
import pi.ip.data.relational.generated.GetServiceAddressByIdRequest;
import pi.ip.data.relational.generated.LawFirmDbServiceGrpc.LawFirmDbServiceBlockingStub;
import pi.ip.data.relational.generated.ServiceAddressServiceGrpc.ServiceAddressServiceBlockingStub;
import pi.ip.generated.datastore_sg3.DatastoreSg3ServiceGrpc.DatastoreSg3ServiceBlockingStub;
import pi.ip.generated.datastore_sg3.IpDatastoreSg3.SuggestSimilarThinServiceAddressRequest;
import pi.ip.generated.datastore_sg3.IpDatastoreSg3.ThinServiceAddress;
import pi.ip.proto.generated.LangType;
import pi.ip.proto.generated.LawFirm;
import pi.ip.proto.generated.ServiceAddress;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

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
  DatastoreSg3ServiceBlockingStub datastoreSg3ServiceBlockingStub;

  @Inject
  TranslationHelper translationHelper;

  public ServiceAddressBundle fetch(final QueuedServiceAddress queuedServiceAddress) {
    return createServiceAddressBundle
        .andThen(addTranslationIfNecessary)
        .andThen(addAgentSuggestions)
        .apply(queuedServiceAddress);
  }

  @VisibleForTesting
  final Function<QueuedServiceAddress, ServiceAddressBundle> createServiceAddressBundle = queuedServiceAddress -> {
    Preconditions.checkArgument(queuedServiceAddress.serviceAddress().isPresent());
    return ServiceAddressBundle
        .newBuilder()
        .setServiceAddressToSort(queuedServiceAddress.serviceAddress().get())
        .setUnsortedServiceAddressQueueItemId(queuedServiceAddress.queueId())
        .build();
  };

  @VisibleForTesting
  final Function<ServiceAddressBundle, ServiceAddressBundle> addTranslationIfNecessary = bundle -> {
    final LangType sourceLanguage = bundle.getServiceAddressToSort().getLanguageType();
    if (sourceLanguage == null || sourceLanguage == LangType.WESTERN_SCRIPT) {
      // We can't translate or no translation required
      return bundle;
    }
    final String textToTranslate = bundle.getServiceAddressToSort().getAddress();
    try {
      return ServiceAddressBundle
          .newBuilder(bundle)
          .setEnTranslation(translationHelper.toEn(textToTranslate, sourceLanguage))
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

    final SuggestSimilarThinServiceAddressRequest getSuggestionsRequest =
        SuggestSimilarThinServiceAddressRequest
            .newBuilder()
            .setNameAddress(serviceAddress.getName() + " " + serviceAddress.getAddress())
            .setCountry(serviceAddress.getCountry())
            .setLongitude(serviceAddress.getLongitude())
            .setLatitude(serviceAddress.getLatitude())
            .build();

    final List<Agent> suggestedAgents =
        datastoreSg3ServiceBlockingStub
            .suggestSimilarThinServiceAddress(getSuggestionsRequest)
            .getSuggestionsList()
            .stream()
            // Group by agent name, preserving suggestion list order
            .collect(groupingBy(ThinServiceAddress::getName, LinkedHashMap::new, toList()))
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
  Agent buildAgent(final Map.Entry<String, List<ThinServiceAddress>> entry) {
    final Agent.Builder agentBuilder = Agent.newBuilder();

    // If there are multiple service addresses, they will all refer to the same law firm.
    // Inspecting the first one will do.
    if (entry.getValue().get(0).getNotALawFirm()) {
      // Not a law firm
      agentBuilder.setNonLawFirm(NonLawFirm.newBuilder().setName(entry.getKey()));
    } else {
      // This is a law firm. Fetch its details.
      final long lawFirmId = entry.getValue().get(0).getLawFirmId();
      final GetLawFirmByIdRequest getLawFirmRequest = GetLawFirmByIdRequest.newBuilder().setLawFirmId(lawFirmId).build();
      final LawFirm lawFirm = lawFirmDbServiceBlockingStub.getLawFirmById(getLawFirmRequest).getLawFirm();
      agentBuilder.setLawFirm(lawFirm);
    }
    // Fetch service addresses
    final List<ServiceAddress> serviceAddresses =
        entry
            .getValue()
            .stream()
            .map(thinServiceAddress -> {
              final GetServiceAddressByIdRequest getServiceAddressRequest =
                  GetServiceAddressByIdRequest
                      .newBuilder()
                      .setServiceAddressId(thinServiceAddress.getServiceAddressId())
                      .build();
              return serviceAddressServiceBlockingStub.getServiceAddressById(getServiceAddressRequest);
            })
            .collect(toList());

    agentBuilder.addAllServiceAddresses(serviceAddresses);

    return agentBuilder.build();
  }
}
