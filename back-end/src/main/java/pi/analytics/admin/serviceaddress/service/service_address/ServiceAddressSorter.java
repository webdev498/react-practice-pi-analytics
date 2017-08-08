/*
 * Copyright (c) 2017 Practice Insight Pty Ltd.
 */

package pi.analytics.admin.serviceaddress.service.service_address;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.protobuf.Int64Value;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import pi.admin.service_address_sorting.generated.AssignServiceAddressRequest;
import pi.admin.service_address_sorting.generated.AssignServiceAddressResponse;
import pi.admin.service_address_sorting.generated.CreateLawFirmRequest;
import pi.admin.service_address_sorting.generated.CreateLawFirmResponse;
import pi.admin.service_address_sorting.generated.SetInsufficientInfoStatusRequest;
import pi.admin.service_address_sorting.generated.SetInsufficientInfoStatusResponse;
import pi.admin.service_address_sorting.generated.SetServiceAddressAsNonLawFirmRequest;
import pi.admin.service_address_sorting.generated.SetServiceAddressAsNonLawFirmResponse;
import pi.admin.service_address_sorting.generated.SortAssignment;
import pi.admin.service_address_sorting.generated.UnsortServiceAddressRequest;
import pi.analytics.admin.serviceaddress.metrics.ImmutableMetricSpec;
import pi.analytics.admin.serviceaddress.metrics.MetricSpec;
import pi.analytics.admin.serviceaddress.metrics.MetricsAccessor;
import pi.analytics.admin.serviceaddress.service.law_firm.LawFirmRepository;
import pi.analytics.admin.serviceaddress.service.user.UserService;
import pi.ip.data.relational.generated.AssignServiceAddressToLawFirmRequest;
import pi.ip.data.relational.generated.DecrementSortScoreRequest;
import pi.ip.data.relational.generated.GetLawFirmByIdRequest;
import pi.ip.data.relational.generated.GetLawFirmByIdResponse;
import pi.ip.data.relational.generated.GetServiceAddressByIdRequest;
import pi.ip.data.relational.generated.IncrementSortScoreRequest;
import pi.ip.data.relational.generated.LawFirmDbServiceGrpc.LawFirmDbServiceBlockingStub;
import pi.ip.data.relational.generated.LogSortDecisionRequest;
import pi.ip.data.relational.generated.ServiceAddressServiceGrpc.ServiceAddressServiceBlockingStub;
import pi.ip.data.relational.generated.SortEffect;
import pi.ip.data.relational.generated.SortResult;
import pi.ip.data.relational.generated.UnassignServiceAddressFromLawFirmRequest;
import pi.ip.generated.es.ESMutationServiceGrpc.ESMutationServiceBlockingStub;
import pi.ip.generated.es.LawFirmServiceAddressRecord;
import pi.ip.generated.es.LawFirmServiceAddressUpdateServiceGrpc.LawFirmServiceAddressUpdateServiceBlockingStub;
import pi.ip.generated.es.LocationRecord;
import pi.ip.generated.es.ServiceAddressRecord;
import pi.ip.proto.generated.LawFirm;
import pi.ip.proto.generated.ServiceAddress;

import static pi.analytics.admin.serviceaddress.service.service_address.ServiceAddressUtils.isAssignedToLawFirm;
import static pi.analytics.admin.serviceaddress.service.service_address.ServiceAddressUtils.needsSorting;
import static pi.analytics.admin.serviceaddress.service.service_address.SortResultUtils.resultOfAssignToLawFirm;
import static pi.analytics.admin.serviceaddress.service.service_address.SortResultUtils.resultOfCreateLawFirmAndAssign;
import static pi.analytics.admin.serviceaddress.service.service_address.SortResultUtils.resultOfSetAsNonLawFirm;
import static pi.analytics.admin.serviceaddress.service.service_address.SortResultUtils.resultOfSetInsufficientInfoToSort;

/**
 * @author shane.xie@practiceinsight.io
 * @author pavel.sitnikov
 */
public class ServiceAddressSorter {

  private static final Logger log = LoggerFactory.getLogger(ServiceAddressSorter.class);

  @Inject
  UserService userService;

  @Inject
  private LawFirmRepository lawFirmRepository;

  @Inject
  LawFirmDbServiceBlockingStub lawFirmDbServiceBlockingStub;

  @Inject
  private ServiceAddressServiceBlockingStub serviceAddressServiceBlockingStub;

  @Inject
  private ESMutationServiceBlockingStub esMutationServiceBlockingStub;

  @Inject
  private LawFirmServiceAddressUpdateServiceBlockingStub lawFirmServiceAddressUpdateServiceBlockingStub;

  @Inject
  private MetricsAccessor metricsAccessor;

  private final MetricSpec sortOutcomeMetricSpec =
      ImmutableMetricSpec
          .builder()
          .action("sort_outcome")
          .addLabels("user", "action", "status", "result")
          .build();

  public AssignServiceAddressResponse assignServiceAddress(final AssignServiceAddressRequest request) {
    Preconditions.checkArgument(request.getLawFirmId() != 0, "Law firm ID is required");
    Preconditions.checkArgument(request.getServiceAddressId() != 0, "Service address ID is required");
    Preconditions.checkArgument(StringUtils.isNotBlank(request.getRequestedBy()), "Requestee is required");

    final ServiceAddress preSortServiceAddress = serviceAddressServiceBlockingStub.getServiceAddressById(
        GetServiceAddressByIdRequest
            .newBuilder()
            .setServiceAddressId(request.getServiceAddressId())
            .build()
    );

    final LawFirm lawFirm = getLawFirmById(request.getLawFirmId())
        .orElseThrow(() -> new IllegalArgumentException("Cannot assign service address to a non-existent law firm"));

    final SortEffect desiredSortEffect = getDesiredSortEffect(preSortServiceAddress, request.getRequestedBy());

    if (desiredSortEffect == SortEffect.SORT_STATUS_UPDATED) {
      // Perform an actual sort
      serviceAddressServiceBlockingStub.assignServiceAddressToLawFirm(
          AssignServiceAddressToLawFirmRequest
              .newBuilder()
              .setLawFirmId(request.getLawFirmId())
              .setServiceAddressId(request.getServiceAddressId())
              .build()
      );
      // Also update Elasticsearch index
      final LawFirmServiceAddressRecord lawFirmServiceAddressRecord =
          buildLawFirmServiceAddressRecordForLawFirmAssignment(preSortServiceAddress, lawFirm);
      lawFirmServiceAddressUpdateServiceBlockingStub
          .upsertLawFirmServiceAddressRecord(lawFirmServiceAddressRecord);
    }

    final SortResult sortResult = resultOfAssignToLawFirm(preSortServiceAddress, lawFirm.getLawFirmId());
    updateSortScoreIfNecessary(preSortServiceAddress.getServiceAddressId(), desiredSortEffect, sortResult);

    // Log sort decision/outcome
    final LogSortDecisionRequest logSortDecisionRequest =
        LogSortDecisionRequest
            .newBuilder()
            .setUsername(request.getRequestedBy())
            .setServiceAddress(preSortServiceAddress)
            .setAssignToLawFirm(lawFirm)
            .setSortEffect(desiredSortEffect)
            .setSortResult(sortResult)
            .build();
    serviceAddressServiceBlockingStub.logSortDecision(logSortDecisionRequest);
    metricsAccessor
        .getCounter(sortOutcomeMetricSpec)
        .inc(request.getRequestedBy(), "assign_to_law_firm", desiredSortEffect.name(), sortResult.name());

    final AssignServiceAddressResponse.Builder response =
        AssignServiceAddressResponse
            .newBuilder()
            .setSortEffect(desiredSortEffect)
            .setSortResult(sortResult);
    if (!needsSorting(preSortServiceAddress)) {
      // This service address has previously been sorted
      response.setExpectedSortAssignment(getCurrentSortAssignment(preSortServiceAddress));
    }
    return response.build();
  }

  // The returned law firm id may be 0 if an actual sort was not carried out e.g. because we're trialling the user
  public CreateLawFirmResponse createLawFirmAndAssignServiceAddress(final CreateLawFirmRequest request) {
    Preconditions.checkArgument(StringUtils.isNotBlank(request.getName()), "Law firm name is required");
    Preconditions.checkArgument(StringUtils.isNotBlank(request.getCountryCode()), "Country code is required");
    Preconditions.checkArgument(request.hasServiceAddress(), "Service address is required");

    final LawFirm lawFirmToBeCreated =
        LawFirm
            .newBuilder()
            .setName(request.getName())
            .setStateStr(request.getState())
            .setCountry(request.getCountryCode())
            .setWebsiteUrl(request.getWebsiteUrl())
            .build();

    final SortEffect desiredSortEffect = getDesiredSortEffect(request.getServiceAddress(), request.getRequestedBy());

    long lawFirmId = 0;

    if (desiredSortEffect == SortEffect.SORT_STATUS_UPDATED) {
      // Perform an actual sort
      // Create the new law firm record in MySQL
      lawFirmId = lawFirmDbServiceBlockingStub.createLawFirm(
          pi.ip.data.relational.generated.CreateLawFirmRequest
              .newBuilder()
              .setLawFirm(lawFirmToBeCreated)
              .setCreatedBy(request.getRequestedBy())
              .build()
      ).getLawFirmId();

      final LawFirm newLawFirm =
          LawFirm
              .newBuilder(lawFirmToBeCreated)
              .setLawFirmId(lawFirmId)
              .build();

      // Assign the service address to the new law firm (MySQL)
      serviceAddressServiceBlockingStub.assignServiceAddressToLawFirm(
          AssignServiceAddressToLawFirmRequest
              .newBuilder()
              .setServiceAddressId(request.getServiceAddress().getServiceAddressId())
              .setLawFirmId(lawFirmId)
              .build()
      );

      // Update Elasticsearch caches
      esMutationServiceBlockingStub.upsertLawFirm(newLawFirm);  // Used by law firm search by name
      lawFirmServiceAddressUpdateServiceBlockingStub.upsertLawFirmServiceAddressRecord(
          buildLawFirmServiceAddressRecordForLawFirm(request.getServiceAddress(), newLawFirm)
      );
    }

    final SortResult sortResult = resultOfCreateLawFirmAndAssign(request.getServiceAddress(), lawFirmToBeCreated,
        (final LawFirm lawFirm) ->
          // Does a law firm exist that has the same name and country as the one that we want to create,
          // and is assigned the service address that is being sorted?
          getLawFirmById(request.getServiceAddress().getLawFirmId().getValue())
              .filter(found -> found.getName() == lawFirm.getName() && found.getCountry() == lawFirm.getCountry())
              .isPresent()
    );

    updateSortScoreIfNecessary(request.getServiceAddress().getServiceAddressId(), desiredSortEffect, sortResult);

    // Log sort decision/outcome
    final LogSortDecisionRequest logSortDecisionRequest =
        LogSortDecisionRequest
            .newBuilder()
            .setUsername(request.getRequestedBy())
            .setServiceAddress(request.getServiceAddress())
            .setCreateLawFirm(lawFirmToBeCreated)
            .setSortEffect(desiredSortEffect)
            .setSortResult(sortResult)
            .build();
    serviceAddressServiceBlockingStub.logSortDecision(logSortDecisionRequest);
    metricsAccessor
        .getCounter(sortOutcomeMetricSpec)
        .inc(request.getRequestedBy(), "create_law_firm", desiredSortEffect.name(), sortResult.name());

    final CreateLawFirmResponse.Builder response =
        CreateLawFirmResponse
            .newBuilder()
            .setSortEffect(desiredSortEffect)
            .setSortResult(sortResult);
    if (needsSorting(request.getServiceAddress())) {
      response.setNewLawFirmId(lawFirmId);
    } else {
      // This service address has previously been sorted
      response.setExpectedSortAssignment(getCurrentSortAssignment(request.getServiceAddress()));
    }
    return response.build();
  }

  public SetServiceAddressAsNonLawFirmResponse setServiceAddressAsNonLawFirm(
      final SetServiceAddressAsNonLawFirmRequest request) {
    Preconditions.checkArgument(request.getServiceAddressId() != 0, "Service address ID is required");

    final ServiceAddress preSortServiceAddress = serviceAddressServiceBlockingStub.getServiceAddressById(
        GetServiceAddressByIdRequest
            .newBuilder()
            .setServiceAddressId(request.getServiceAddressId())
            .build()
    );

    final SortEffect desiredSortEffect = getDesiredSortEffect(preSortServiceAddress, request.getRequestedBy());

    if (desiredSortEffect == SortEffect.SORT_STATUS_UPDATED) {
      // Perform an actual sort
      serviceAddressServiceBlockingStub.setServiceAddressAsNonLawFirm(
          pi.ip.data.relational.generated.SetServiceAddressAsNonLawFirmRequest
              .newBuilder()
              .setServiceAddressId(request.getServiceAddressId())
              .build()
      );

      // Also update Elasticsearch index
      final ServiceAddress serviceAddress = serviceAddressServiceBlockingStub.getServiceAddressById(
          GetServiceAddressByIdRequest
              .newBuilder()
              .setServiceAddressId(request.getServiceAddressId())
              .build()
      );
      final LawFirmServiceAddressRecord lawFirmServiceAddressRecord =
          buildLawFirmServiceAddressRecordForNonLawFirm(serviceAddress);
      lawFirmServiceAddressUpdateServiceBlockingStub.upsertLawFirmServiceAddressRecord(lawFirmServiceAddressRecord);
    }

    final SortResult sortResult = resultOfSetAsNonLawFirm(preSortServiceAddress);
    updateSortScoreIfNecessary(preSortServiceAddress.getServiceAddressId(), desiredSortEffect, sortResult);

    // Log sort decision/outcome
    final LogSortDecisionRequest logSortDecisionRequest =
        LogSortDecisionRequest
            .newBuilder()
            .setUsername(request.getRequestedBy())
            .setServiceAddress(preSortServiceAddress)
            .setNonLawFirm(true)
            .setSortEffect(desiredSortEffect)
            .setSortResult(sortResult)
            .build();
    serviceAddressServiceBlockingStub.logSortDecision(logSortDecisionRequest);
    metricsAccessor
        .getCounter(sortOutcomeMetricSpec
        )
        .inc(request.getRequestedBy(), "non_law_firm", desiredSortEffect.name(), sortResult.name());

    final SetServiceAddressAsNonLawFirmResponse.Builder response =
        SetServiceAddressAsNonLawFirmResponse
            .newBuilder()
            .setSortEffect(desiredSortEffect)
            .setSortResult(sortResult);
    if (!needsSorting(preSortServiceAddress)) {
      response.setExpectedSortAssignment(getCurrentSortAssignment(preSortServiceAddress));
    }
    return response.build();
  }

  public void unsortServiceAddress(final UnsortServiceAddressRequest request) {
    Preconditions.checkArgument(request.getServiceAddressId() != 0, "Service address ID is required");
    serviceAddressServiceBlockingStub.unassignServiceAddressFromLawFirm(
        UnassignServiceAddressFromLawFirmRequest
            .newBuilder()
            .setServiceAddressId(request.getServiceAddressId())
            .build()
    );
    lawFirmServiceAddressUpdateServiceBlockingStub.deleteLawFirmServiceAddressRecord(
        Int64Value
            .newBuilder()
            .setValue(request.getServiceAddressId())
            .build()
    );
  }

  public SetInsufficientInfoStatusResponse setInsufficientInfoToSort(final SetInsufficientInfoStatusRequest request) {
    Preconditions.checkArgument(request.getServiceAddressId() != 0, "Service address ID is required");

    final ServiceAddress preSortServiceAddress = serviceAddressServiceBlockingStub.getServiceAddressById(
        GetServiceAddressByIdRequest
            .newBuilder()
            .setServiceAddressId(request.getServiceAddressId())
            .build()
    );

    serviceAddressServiceBlockingStub.insufficientInfoToSort(
        pi.ip.data.relational.generated.InsufficientInfoToSortRequest.newBuilder()
            .setServiceAddressId(request.getServiceAddressId())
            .build()
    );

    // Log sort decision/outcome
    final SortResult sortResult = resultOfSetInsufficientInfoToSort(preSortServiceAddress);
    final SortEffect sortEffect = getDesiredSortEffect(preSortServiceAddress, request.getRequestedBy());
    final LogSortDecisionRequest logSortDecisionRequest =
        LogSortDecisionRequest
            .newBuilder()
            .setUsername(request.getRequestedBy())
            .setServiceAddress(preSortServiceAddress)
            .setInsufficientInfoToSort(true)
            .setSortEffect(sortEffect)
            .setSortResult(sortResult)
            .build();
    serviceAddressServiceBlockingStub.logSortDecision(logSortDecisionRequest);
    metricsAccessor
        .getCounter(sortOutcomeMetricSpec)
        .inc(request.getRequestedBy(), "sorting_impossible", sortEffect.name(), sortResult.name());

    final SetInsufficientInfoStatusResponse.Builder response =
        SetInsufficientInfoStatusResponse
            .newBuilder()
            .setSortEffect(sortEffect)
            .setSortResult(sortResult);
    if (!needsSorting(preSortServiceAddress)) {
      response.setExpectedSortAssignment(getCurrentSortAssignment(preSortServiceAddress));
    }
    return response.build();
  }

  @VisibleForTesting
  SortEffect getDesiredSortEffect(final ServiceAddress preSort, final String username) {
    if (userService.canPerformRealSort(username)) {
      if (needsSorting(preSort)) {
        return SortEffect.SORT_STATUS_UPDATED;
      }
      return SortEffect.SORT_SCORE_UPDATED;
    }
    return SortEffect.NOT_UPDATED;
  }

  @VisibleForTesting
  boolean updateSortScoreIfNecessary(final long serviceAddressId, final SortEffect sortEffect, final SortResult sortResult) {
    if (sortEffect != SortEffect.SORT_SCORE_UPDATED) {
      return false;
    }
    switch (sortResult) {
      case SAME:
        serviceAddressServiceBlockingStub.incrementSortScore(
            IncrementSortScoreRequest
                .newBuilder()
                .setServiceAddressId(serviceAddressId)
                .build()
        );
        return true;
      case DIFFERENT:
        serviceAddressServiceBlockingStub.decrementSortScore(
            DecrementSortScoreRequest
                .newBuilder()
                .setServiceAddressId(serviceAddressId)
                .build()
        );
        return true;
      default:
        return false;
    }
  }

  @VisibleForTesting
  SortAssignment getCurrentSortAssignment(final ServiceAddress serviceAddress) {
    final SortAssignment.Builder sortAssignment = SortAssignment.newBuilder().setServiceAddress(serviceAddress);
    if (isAssignedToLawFirm(serviceAddress)) {
      final Optional<LawFirm> assignedLawFirm = getLawFirmById(serviceAddress.getLawFirmId().getValue());
      if (assignedLawFirm.isPresent()) {
        sortAssignment.setAssignedLawFirm(assignedLawFirm.get());
      } else {
        // While we couldn't get the law firm details, the service address is still considered to be assigned
        sortAssignment.setAssignedLawFirm(
            LawFirm
                .newBuilder()
                .setLawFirmId(serviceAddress.getLawFirmId().getValue())
        );
      }
    }
    return sortAssignment.build();
  }

  @VisibleForTesting
  Optional<LawFirm> getLawFirmById(final long lawFirmId) {
    final GetLawFirmByIdResponse response = lawFirmDbServiceBlockingStub.getLawFirmById(
        GetLawFirmByIdRequest
            .newBuilder()
            .setLawFirmId(lawFirmId)
            .build()
    );
    if (response.getResult() == GetLawFirmByIdResponse.GetLawFirmByIdResult.LAW_FIRM_NOT_FOUND) {
      return Optional.empty();
    }
    return Optional.of(response.getLawFirm());
  }

  private LawFirmServiceAddressRecord buildLawFirmServiceAddressRecordForLawFirmAssignment(
      final ServiceAddress serviceAddress, final LawFirm lawFirm) {
    return LawFirmServiceAddressRecord
        .newBuilder()
        .setLawFirm(lawFirm)
        .setServiceAddress(
            ServiceAddressRecord
                .newBuilder()
                .setId(serviceAddress.getServiceAddressId())
                .setNameAddress(serviceAddress.getName() + " " + serviceAddress.getAddress())
                .setCountry(serviceAddress.getCountry())
                .setLoc(
                    LocationRecord
                        .newBuilder()
                        .setLon((float) serviceAddress.getLongitude())
                        .setLat((float) serviceAddress.getLatitude())
                )
        )
        .setLawFirmFlag(true)
        .build();
  }

  private LawFirmServiceAddressRecord buildLawFirmServiceAddressRecordForNonLawFirm(final ServiceAddress serviceAddress) {
    return LawFirmServiceAddressRecord
        .newBuilder()
        .setLawFirmFlag(false)
        .setServiceAddress(
            ServiceAddressRecord
                .newBuilder()
                .setId(serviceAddress.getServiceAddressId())
                .setNameAddress(serviceAddress.getName() + " " + serviceAddress.getAddress())
                .setCountry(serviceAddress.getCountry())
                .setLoc(
                    LocationRecord
                        .newBuilder()
                        .setLon((float) serviceAddress.getLongitude())
                        .setLat((float) serviceAddress.getLatitude())
                )
        )
        .build();
  }

  private LawFirmServiceAddressRecord buildLawFirmServiceAddressRecordForLawFirm(final ServiceAddress serviceAddress,
                                                                                 final LawFirm lawFirm) {
    return LawFirmServiceAddressRecord
        .newBuilder()
        .setLawFirm(lawFirm)
        .setLawFirmFlag(true)
        .setServiceAddress(
            ServiceAddressRecord
                .newBuilder()
                .setId(serviceAddress.getServiceAddressId())
                .setNameAddress(serviceAddress.getName() + " " + serviceAddress.getAddress())
                .setCountry(serviceAddress.getCountry())
                .setLoc(
                    LocationRecord
                        .newBuilder()
                        .setLat((float) serviceAddress.getLatitude())
                        .setLon((float) serviceAddress.getLongitude())
                )
        )
        .build();
  }
}
