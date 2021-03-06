/*
 * Copyright (c) 2017 Practice Insight Pty Ltd.
 */

package pi.analytics.admin.serviceaddress.service.service_address;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.util.Providers;
import com.google.protobuf.Int64Value;

import com.github.javafaker.Faker;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.Stubber;

import java.util.Optional;
import java.util.UUID;

import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import pi.admin.service_address_sorting.generated.AssignServiceAddressRequest;
import pi.admin.service_address_sorting.generated.AssignServiceAddressResponse;
import pi.admin.service_address_sorting.generated.CreateLawFirmRequest;
import pi.admin.service_address_sorting.generated.CreateLawFirmResponse;
import pi.admin.service_address_sorting.generated.SetInsufficientInfoStatusRequest;
import pi.admin.service_address_sorting.generated.SetInsufficientInfoStatusResponse;
import pi.admin.service_address_sorting.generated.SetServiceAddressAsNonLawFirmResponse;
import pi.admin.service_address_sorting.generated.UnsortServiceAddressRequest;
import pi.analytics.admin.serviceaddress.service.helpers.LawFirmTestHelper;
import pi.analytics.admin.serviceaddress.service.law_firm.LawFirmRepository;
import pi.analytics.admin.serviceaddress.service.user.UserService;
import pi.ip.data.relational.generated.AssignServiceAddressToLawFirmRequest;
import pi.ip.data.relational.generated.DecrementSortScoreRequest;
import pi.ip.data.relational.generated.GetLawFirmByIdRequest;
import pi.ip.data.relational.generated.GetLawFirmByIdResponse;
import pi.ip.data.relational.generated.GetServiceAddressByIdRequest;
import pi.ip.data.relational.generated.IncrementSortScoreRequest;
import pi.ip.data.relational.generated.InsufficientInfoToSortRequest;
import pi.ip.data.relational.generated.LawFirmDbServiceGrpc;
import pi.ip.data.relational.generated.LogSortDecisionRequest;
import pi.ip.data.relational.generated.ServiceAddressServiceGrpc;
import pi.ip.data.relational.generated.SetServiceAddressAsNonLawFirmRequest;
import pi.ip.data.relational.generated.UnassignServiceAddressFromLawFirmRequest;
import pi.ip.generated.es.ESMutationServiceGrpc;
import pi.ip.generated.es.LawFirmServiceAddressRecord;
import pi.ip.generated.es.LawFirmServiceAddressUpdateServiceGrpc;
import pi.ip.generated.es.LocationRecord;
import pi.ip.generated.es.ServiceAddressRecord;
import pi.ip.proto.generated.AckResponse;
import pi.ip.proto.generated.LawFirm;
import pi.ip.proto.generated.ServiceAddress;
import pi.ip.proto.generated.SortEffect;
import pi.ip.proto.generated.SortResult;
import pi.ip.proto.generated.SortStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static pi.analytics.admin.serviceaddress.service.helpers.GrpcTestHelper.replyWith;
import static pi.analytics.admin.serviceaddress.service.helpers.GrpcTestHelper.replyWithError;
import static pi.analytics.admin.serviceaddress.service.helpers.ServiceAddressTestHelper.createServiceAddress;
import static pi.analytics.admin.serviceaddress.service.helpers.ServiceAddressTestHelper.createServiceAddressForNonLawFirm;
import static pi.analytics.admin.serviceaddress.service.helpers.ServiceAddressTestHelper.createServiceAddressToMatchLawFirm;
import static pi.analytics.admin.serviceaddress.service.helpers.ServiceAddressTestHelper.createUnsortedServiceAddress;

/**
 * @author shane.xie@practiceinsight.io
 */
public class ServiceAddressSorterTest {

  private Faker faker = new Faker();
  private UserService userService;
  private LawFirmRepository lawFirmRepository;
  private LawFirmDbServiceGrpc.LawFirmDbServiceImplBase lawFirmDbService;
  private ServiceAddressServiceGrpc.ServiceAddressServiceImplBase serviceAddressService;
  private ESMutationServiceGrpc.ESMutationServiceImplBase esMutationService;
  private LawFirmServiceAddressUpdateServiceGrpc.LawFirmServiceAddressUpdateServiceImplBase
      lawFirmServiceAddressUpdateService;
  private Server server;
  private ManagedChannel channel;
  private ServiceAddressSorter serviceAddressSorter;

  @Before
  public void setUp() throws Exception {
    userService = mock(UserService.class);
    lawFirmRepository = mock(LawFirmRepository.class);
    lawFirmDbService = mock(LawFirmDbServiceGrpc.LawFirmDbServiceImplBase.class);
    serviceAddressService = mock(ServiceAddressServiceGrpc.ServiceAddressServiceImplBase.class);
    esMutationService = mock(ESMutationServiceGrpc.ESMutationServiceImplBase.class);
    lawFirmServiceAddressUpdateService = mock(LawFirmServiceAddressUpdateServiceGrpc
        .LawFirmServiceAddressUpdateServiceImplBase.class);

    final String serverName = "service-address-sorter-test-".concat(UUID.randomUUID().toString());
    server =
        InProcessServerBuilder
            .forName(serverName)
            .addService(lawFirmDbService.bindService())
            .addService(serviceAddressService.bindService())
            .addService(esMutationService.bindService())
            .addService(lawFirmServiceAddressUpdateService.bindService())
            .directExecutor()
            .build()
            .start();
    channel =
        InProcessChannelBuilder
            .forName(serverName)
            .directExecutor()
            .build();

    serviceAddressSorter = Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        bind(UserService.class).toInstance(userService);
        bind(LawFirmRepository.class).toProvider(Providers.of(lawFirmRepository));
        bind(LawFirmDbServiceGrpc.LawFirmDbServiceBlockingStub.class)
            .toInstance(LawFirmDbServiceGrpc.newBlockingStub(channel));
        bind(ServiceAddressServiceGrpc.ServiceAddressServiceBlockingStub.class)
            .toInstance(ServiceAddressServiceGrpc.newBlockingStub(channel));
        bind(ESMutationServiceGrpc.ESMutationServiceBlockingStub.class)
            .toInstance(ESMutationServiceGrpc.newBlockingStub(channel));
        bind(LawFirmServiceAddressUpdateServiceGrpc.LawFirmServiceAddressUpdateServiceBlockingStub.class)
            .toInstance(LawFirmServiceAddressUpdateServiceGrpc.newBlockingStub(channel));
      }
    }).getInstance(ServiceAddressSorter.class);
  }

  @After
  public void tearDown() throws Exception {
    channel.shutdown();
    server.shutdownNow();
  }

  @Test
  public void assignServiceAddress_service_address_not_found() throws Exception {
    replyWithError(Status.NOT_FOUND.asRuntimeException())
        .when(serviceAddressService)
        .getServiceAddressById(any(GetServiceAddressByIdRequest.class), any(StreamObserver.class));
    final LawFirm lawFirm = LawFirmTestHelper.createLawFirm();
    replyWith(GetLawFirmByIdResponse.newBuilder().setLawFirm(lawFirm).build())
        .when(lawFirmDbService)
        .getLawFirmById(any(GetLawFirmByIdRequest.class), any(StreamObserver.class));
    assertThatThrownBy(() ->
        serviceAddressSorter.assignServiceAddress(
            AssignServiceAddressRequest
                .newBuilder()
                .setServiceAddressId(faker.number().randomNumber(8, true))
                .setLawFirmId(lawFirm.getLawFirmId())
                .setRequestedBy(faker.name().username())
                .build()
        )
    )
    .as("The service address does not exist")
    .isInstanceOf(Status.NOT_FOUND.asRuntimeException().getClass());
  }

  @Test
  public void assignServiceAddress_law_firm_not_found() throws Exception {
    final ServiceAddress serviceAddress = createUnsortedServiceAddress(faker.company().name());
    replyWith(serviceAddress)
        .when(serviceAddressService)
        .getServiceAddressById(any(GetServiceAddressByIdRequest.class), any(StreamObserver.class));
    final GetLawFirmByIdResponse getLawFirmResponse =
        GetLawFirmByIdResponse
            .newBuilder()
            .setResult(GetLawFirmByIdResponse.GetLawFirmByIdResult.LAW_FIRM_NOT_FOUND)
            .build();
    replyWith(getLawFirmResponse)
        .when(lawFirmDbService)
        .getLawFirmById(any(GetLawFirmByIdRequest.class), any(StreamObserver.class));
    assertThatThrownBy(() ->
        serviceAddressSorter
            .assignServiceAddress(
                AssignServiceAddressRequest
                    .newBuilder()
                    .setServiceAddressId(serviceAddress.getServiceAddressId())
                    .setLawFirmId(faker.number().randomNumber(8, true))
                    .setRequestedBy(faker.name().username())
                    .build()
            )
    )
    .as("The law firm does not exist")
    .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void assignServiceAddress_new_sort_success() throws Exception {
    final LawFirm lawFirm = LawFirmTestHelper.createLawFirm();
    final ServiceAddress serviceAddress = createUnsortedServiceAddress(lawFirm.getName());

    final String username = faker.name().username();
    when(userService.canPerformRealSort(eq(username))).thenReturn(true);

    replyWithAckResponse()
        .when(serviceAddressService)
        .assignServiceAddressToLawFirm(any(AssignServiceAddressToLawFirmRequest.class), any(StreamObserver.class));
    replyWithAckResponse()
        .when(lawFirmServiceAddressUpdateService)
        .upsertLawFirmServiceAddressRecord(any(LawFirmServiceAddressRecord.class), any(StreamObserver.class));
    replyWith(GetLawFirmByIdResponse.newBuilder().setLawFirm(lawFirm).build())
        .when(lawFirmDbService)
        .getLawFirmById(any(GetLawFirmByIdRequest.class), any(StreamObserver.class));
    replyWith(serviceAddress)
        .when(serviceAddressService)
        .getServiceAddressById(any(GetServiceAddressByIdRequest.class), any(StreamObserver.class));
    replyWithAckResponse()
        .when(serviceAddressService)
        .logSortDecision(any(LogSortDecisionRequest.class), any(StreamObserver.class));

    final AssignServiceAddressResponse response = serviceAddressSorter.assignServiceAddress(
        AssignServiceAddressRequest
            .newBuilder()
            .setLawFirmId(lawFirm.getLawFirmId())
            .setServiceAddressId(serviceAddress.getServiceAddressId())
            .setRequestedBy(username)
            .build()
    );
    assertThat(response.getSortEffect())
        .as("This is a new sort")
        .isEqualTo(SortEffect.SORT_STATUS_UPDATED);
    assertThat(response.getSortResult())
        .as("This is a new sort")
        .isEqualTo(SortResult.NEW_SORT);
    assertThat(response.hasExpectedSortAssignment())
        .as("This is a not a re-sort")
        .isFalse();

    verify(serviceAddressService).assignServiceAddressToLawFirm(
        eq(
            AssignServiceAddressToLawFirmRequest
                .newBuilder()
                .setLawFirmId(lawFirm.getLawFirmId())
                .setServiceAddressId(serviceAddress.getServiceAddressId())
                .build()
        ),
        any(StreamObserver.class)
    );

    verify(lawFirmServiceAddressUpdateService).upsertLawFirmServiceAddressRecord(
        eq(
            LawFirmServiceAddressRecord
                .newBuilder()
                .setLawFirmFlag(true)
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
                ).build()
        ),
        any(StreamObserver.class)
    );

    verify(serviceAddressService).logSortDecision(
        eq(
            LogSortDecisionRequest
                .newBuilder()
                .setUsername(username)
                .setServiceAddress(serviceAddress)
                .setAssignToLawFirm(lawFirm)
                .setSortEffect(SortEffect.SORT_STATUS_UPDATED)
                .setSortResult(SortResult.NEW_SORT)
                .build()
        ),
        any(StreamObserver.class)
    );
  }

  @Test
  public void assignServiceAddress_re_sort() throws Exception {
    final LawFirm lawFirm = LawFirmTestHelper.createLawFirm();
    final ServiceAddress serviceAddress = createServiceAddressForNonLawFirm(faker.name().fullName());

    final String username = faker.name().username();
    when(userService.canPerformRealSort(eq(username))).thenReturn(true);

    replyWith(GetLawFirmByIdResponse.newBuilder().setLawFirm(lawFirm).build())
        .when(lawFirmDbService)
        .getLawFirmById(any(GetLawFirmByIdRequest.class), any(StreamObserver.class));
    replyWith(serviceAddress)
        .when(serviceAddressService)
        .getServiceAddressById(any(GetServiceAddressByIdRequest.class), any(StreamObserver.class));
    replyWithAckResponse()
        .when(serviceAddressService)
        .decrementSortScore(any(DecrementSortScoreRequest.class), any(StreamObserver.class));
    replyWithAckResponse()
        .when(serviceAddressService)
        .logSortDecision(any(LogSortDecisionRequest.class), any(StreamObserver.class));

    final AssignServiceAddressResponse response = serviceAddressSorter.assignServiceAddress(
        AssignServiceAddressRequest
            .newBuilder()
            .setLawFirmId(lawFirm.getLawFirmId())
            .setServiceAddressId(serviceAddress.getServiceAddressId())
            .setRequestedBy(username)
            .build()
    );
    assertThat(response.getSortEffect())
        .as("This is not a new sort and the user is allowed to do real sorts")
        .isEqualTo(SortEffect.SORT_SCORE_UPDATED);
    assertThat(response.getSortResult())
        .as("The sort result is different")
        .isEqualTo(SortResult.DIFFERENT);
    assertThat(response.getExpectedSortAssignment().getServiceAddress())
        .as("Service address must match")
        .isEqualTo(serviceAddress);
    assertThat(response.getExpectedSortAssignment().hasAssignedLawFirm())
        .as("Service address is not assigned to a law firm")
        .isFalse();
  }

  @Test
  public void createLawFirm_new_sort() throws Exception {
    final String username = faker.name().username();
    when(userService.canPerformRealSort(eq(username))).thenReturn(true);

    final CreateLawFirmRequest createLawFirmRequest =
        CreateLawFirmRequest
            .newBuilder()
            .setRequestedBy(username)
            .setName(faker.company().name())
            .setState(faker.address().state())
            .setCountryCode(faker.address().countryCode())
            .setWebsiteUrl(faker.internet().url())
            .setServiceAddress(createUnsortedServiceAddress(faker.name().fullName()))
            .build();

    final long newLawFirmId = faker.number().randomNumber(8, true);

    replyWith(
        pi.ip.data.relational.generated.CreateLawFirmResponse
            .newBuilder()
            .setLawFirmId(newLawFirmId)
            .build()
    )
    .when(lawFirmDbService)
    .createLawFirm(any(pi.ip.data.relational.generated.CreateLawFirmRequest.class), any(StreamObserver.class));

    replyWith(AckResponse.getDefaultInstance())
        .when(serviceAddressService)
        .assignServiceAddressToLawFirm(any(AssignServiceAddressToLawFirmRequest.class), any(StreamObserver.class));
    replyWith(AckResponse.getDefaultInstance())
        .when(esMutationService)
        .upsertLawFirm(any(LawFirm.class), any(StreamObserver.class));
    replyWith(AckResponse.getDefaultInstance())
        .when(lawFirmServiceAddressUpdateService)
        .upsertLawFirmServiceAddressRecord(any(LawFirmServiceAddressRecord.class), any(StreamObserver.class));
    final ArgumentCaptor<LogSortDecisionRequest> logSortDecisionRequestArgument =
        ArgumentCaptor.forClass(LogSortDecisionRequest.class);
    replyWithAckResponse()
        .when(serviceAddressService)
        .logSortDecision(logSortDecisionRequestArgument.capture(), any(StreamObserver.class));

    // Run test
    final CreateLawFirmResponse response = serviceAddressSorter.createLawFirmAndAssignServiceAddress(createLawFirmRequest);
    assertThat(response.getSortEffect())
        .as("This is a new sort")
        .isEqualTo(SortEffect.SORT_STATUS_UPDATED);
    assertThat(response.getSortResult())
        .as("This is a new sort")
        .isEqualTo(SortResult.NEW_SORT);
    assertThat(response.getOutcomeCase() == CreateLawFirmResponse.OutcomeCase.NEW_LAW_FIRM_ID)
        .as("This is a not a re-sort")
        .isTrue();

    // Verify create law firm called
    verify(lawFirmDbService, times(1))
        .createLawFirm(any(pi.ip.data.relational.generated.CreateLawFirmRequest.class), any(StreamObserver.class));

    // Verify assign service address to law firm called
    verify(serviceAddressService, times(1))
        .assignServiceAddressToLawFirm(
            eq(
                AssignServiceAddressToLawFirmRequest
                    .newBuilder()
                    .setLawFirmId(newLawFirmId)
                    .setServiceAddressId(createLawFirmRequest.getServiceAddress().getServiceAddressId())
                    .build()
            ),
            any(StreamObserver.class)
        );

    final LawFirm newLawFirm =
        LawFirm
            .newBuilder()
            .setLawFirmId(newLawFirmId)
            .setName(createLawFirmRequest.getName())
            .setStateStr(createLawFirmRequest.getState())
            .setCountry(createLawFirmRequest.getCountryCode())
            .setWebsiteUrl(createLawFirmRequest.getWebsiteUrl())
            .build();

    // Verify upsert into law firm caches called
    verify(esMutationService, times(1))
        .upsertLawFirm(eq(newLawFirm), any(StreamObserver.class));

    // Verify upsert law firm service address called
    verify(lawFirmServiceAddressUpdateService, times(1))
        .upsertLawFirmServiceAddressRecord(
            eq(
                LawFirmServiceAddressRecord
                    .newBuilder()
                    .setServiceAddress(
                        ServiceAddressRecord
                            .newBuilder()
                            .setId(createLawFirmRequest.getServiceAddress().getServiceAddressId())
                            .setNameAddress(createLawFirmRequest.getServiceAddress().getName() + " "
                                + createLawFirmRequest.getServiceAddress().getAddress())
                            .setCountry(createLawFirmRequest.getServiceAddress().getCountry())
                            .setLoc(
                                LocationRecord
                                    .newBuilder()
                                    .setLon((float) createLawFirmRequest.getServiceAddress().getLongitude())
                                    .setLat((float) createLawFirmRequest.getServiceAddress().getLatitude())
                            )
                    )
                    .setLawFirmFlag(true)
                    .setLawFirm(newLawFirm)
                    .build()
            ),
            any(StreamObserver.class)
        );

    assertThat(logSortDecisionRequestArgument.getValue().getUsername())
        .as("The username must match")
        .isEqualTo(username);

    assertThat(logSortDecisionRequestArgument.getValue().getCreateLawFirm().getName())
        .as("The law firm names must match")
        .isEqualTo(createLawFirmRequest.getName());

    assertThat(logSortDecisionRequestArgument.getValue().getServiceAddress().getName())
        .as("The service address names must match")
        .isEqualTo(createLawFirmRequest.getServiceAddress().getName());

    assertThat(logSortDecisionRequestArgument.getValue().getSortEffect())
        .as("Applied sort status because the user is allowed to sort service addresses")
        .isEqualTo(SortEffect.SORT_STATUS_UPDATED);

    assertThat(logSortDecisionRequestArgument.getValue().getSortResult())
        .as("This is a new sort because the service address was unsorted")
        .isEqualTo(SortResult.NEW_SORT);
  }

  @Test
  public void createLawFirm_already_sorted_same() throws Exception {
    final String username = faker.name().username();
    when(userService.canPerformRealSort(eq(username))).thenReturn(false);

    final LawFirm lawFirm = LawFirmTestHelper.createLawFirm();
    final CreateLawFirmRequest createLawFirmRequest =
        CreateLawFirmRequest
            .newBuilder()
            .setRequestedBy(username)
            .setName(lawFirm.getName())
            .setState(lawFirm.getStateStr())
            .setCountryCode(lawFirm.getCountry())
            .setWebsiteUrl(lawFirm.getWebsiteUrl())
            .setServiceAddress(createServiceAddressToMatchLawFirm(lawFirm))
            .build();

    replyWithAckResponse()
        .when(serviceAddressService)
        .logSortDecision(any(LogSortDecisionRequest.class), any(StreamObserver.class));

    replyWith(
        GetLawFirmByIdResponse
            .newBuilder()
            .setLawFirm(lawFirm)
            .setResult(GetLawFirmByIdResponse.GetLawFirmByIdResult.LAW_FIRM_LOOKUP_OK)
            .build()
    )
    .when(lawFirmDbService).getLawFirmById(any(GetLawFirmByIdRequest.class), any(StreamObserver.class));

    // Run test
    final CreateLawFirmResponse response = serviceAddressSorter.createLawFirmAndAssignServiceAddress(createLawFirmRequest);
    assertThat(response.getSortEffect())
        .as("The user doesn't have permission to perform a real sort")
        .isEqualTo(SortEffect.NOT_UPDATED);
    assertThat(response.getSortResult())
        .as("The sort result is the same")
        .isEqualTo(SortResult.SAME);
    assertThat(response.getOutcomeCase() == CreateLawFirmResponse.OutcomeCase.EXPECTED_SORT_ASSIGNMENT)
        .as("This is a re-sort")
        .isTrue();
  }

  @Test
  public void unsortServiceAddress() throws Exception {
    replyWithAckResponse()
        .when(serviceAddressService)
        .unassignServiceAddressFromLawFirm(any(UnassignServiceAddressFromLawFirmRequest.class), any(StreamObserver.class));
    replyWithAckResponse()
        .when(lawFirmServiceAddressUpdateService)
        .deleteLawFirmServiceAddressRecord(any(Int64Value.class), any(StreamObserver.class));
    serviceAddressSorter.unsortServiceAddress(UnsortServiceAddressRequest.newBuilder().setServiceAddressId(1L).build());
    verify(serviceAddressService, times(1))
        .unassignServiceAddressFromLawFirm(
            eq(UnassignServiceAddressFromLawFirmRequest.newBuilder().setServiceAddressId(1L).build()),
            any(StreamObserver.class)
        );
    verify(lawFirmServiceAddressUpdateService, times(1))
        .deleteLawFirmServiceAddressRecord(eq(Int64Value.newBuilder().setValue(1L).build()), any(StreamObserver.class));
  }

  @Test
  public void testSetInsufficientInfoSort_new_sort() {
    final String username = faker.name().username();
    final ServiceAddress serviceAddress = createUnsortedServiceAddress(faker.company().name());

    when(userService.canPerformRealSort(eq(username))).thenReturn(true);
    replyWith(serviceAddress)
        .when(serviceAddressService)
        .getServiceAddressById(any(GetServiceAddressByIdRequest.class), any(StreamObserver.class));
    replyWithAckResponse()
        .when(serviceAddressService)
        .insufficientInfoToSort(any(InsufficientInfoToSortRequest.class), any(StreamObserver.class));
    replyWithAckResponse()
        .when(serviceAddressService)
        .logSortDecision(any(LogSortDecisionRequest.class), any(StreamObserver.class));

    SetInsufficientInfoStatusResponse response = serviceAddressSorter.setInsufficientInfoToSort(
        SetInsufficientInfoStatusRequest
            .newBuilder()
            .setServiceAddressId(1L)
            .setRequestedBy(username)
            .build()
    );

    assertThat(response.getSortEffect())
        .as("This is a new sort")
        .isEqualTo(SortEffect.SORT_STATUS_UPDATED);
    assertThat(response.getSortResult())
        .as("This is a new sort")
        .isEqualTo(SortResult.NEW_SORT);
    assertThat(response.hasExpectedSortAssignment())
        .as("This is a not a re-sort")
        .isFalse();

    verify(serviceAddressService, times(1))
        .insufficientInfoToSort(
            eq(InsufficientInfoToSortRequest.newBuilder().setServiceAddressId(1L).build()),
            any(StreamObserver.class)
        );

    verify(serviceAddressService).logSortDecision(
        eq(
            LogSortDecisionRequest
                .newBuilder()
                .setUsername(username)
                .setServiceAddress(serviceAddress)
                .setInsufficientInfoToSort(true)
                .setSortEffect(SortEffect.SORT_STATUS_UPDATED)
                .setSortResult(SortResult.NEW_SORT)
                .build()
        ),
        any(StreamObserver.class)
    );
  }

  @Test
  public void testSetInsufficientInfoSort_re_sort() {
    final String username = faker.name().username();
    final ServiceAddress serviceAddress = createServiceAddressForNonLawFirm(faker.name().fullName());

    when(userService.canPerformRealSort(eq(username))).thenReturn(true);
    replyWith(serviceAddress)
        .when(serviceAddressService)
        .getServiceAddressById(any(GetServiceAddressByIdRequest.class), any(StreamObserver.class));
    replyWithAckResponse()
        .when(serviceAddressService)
        .insufficientInfoToSort(any(InsufficientInfoToSortRequest.class), any(StreamObserver.class));
    replyWithAckResponse()
        .when(serviceAddressService)
        .logSortDecision(any(LogSortDecisionRequest.class), any(StreamObserver.class));

    SetInsufficientInfoStatusResponse response = serviceAddressSorter.setInsufficientInfoToSort(
        SetInsufficientInfoStatusRequest
            .newBuilder()
            .setServiceAddressId(1L)
            .setRequestedBy(username)
            .build()
    );

    assertThat(response.getSortEffect())
        .as("This is a not new sort and the user can do real sorts")
        .isEqualTo(SortEffect.SORT_SCORE_UPDATED);
    assertThat(response.getSortResult())
        .as("The sort result is different")
        .isEqualTo(SortResult.DIFFERENT);
    assertThat(response.hasExpectedSortAssignment())
        .as("This is a re-sort")
        .isTrue();
  }

  @Test
  public void setServiceAddressAsNonLawFirm_new_sort() throws Exception {
    final ServiceAddress serviceAddress =
        ServiceAddress
            .newBuilder(createServiceAddressForNonLawFirm(faker.company().name()))
            .setLawFirmStatusDetermined(false)  // Unsorted
            .build();

    final String username = faker.name().username();
    when(userService.canPerformRealSort(eq(username))).thenReturn(true);

    replyWithAckResponse()
        .when(serviceAddressService)
        .setServiceAddressAsNonLawFirm(any(SetServiceAddressAsNonLawFirmRequest.class), any(StreamObserver.class));
    replyWith(serviceAddress)
        .when(serviceAddressService)
        .getServiceAddressById(any(GetServiceAddressByIdRequest.class), any(StreamObserver.class));
    replyWithAckResponse()
        .when(lawFirmServiceAddressUpdateService)
        .upsertLawFirmServiceAddressRecord(any(LawFirmServiceAddressRecord.class), any(StreamObserver.class));
    replyWithAckResponse()
        .when(serviceAddressService)
        .logSortDecision(any(LogSortDecisionRequest.class), any(StreamObserver.class));

    final SetServiceAddressAsNonLawFirmResponse response = serviceAddressSorter.setServiceAddressAsNonLawFirm(
        pi.admin.service_address_sorting.generated.SetServiceAddressAsNonLawFirmRequest
            .newBuilder()
            .setServiceAddressId(serviceAddress.getServiceAddressId())
            .setRequestedBy(username)
            .build()
    );

    assertThat(response.getSortEffect())
        .as("This is a new sort")
        .isEqualTo(SortEffect.SORT_STATUS_UPDATED);
    assertThat(response.getSortResult())
        .as("This is a new sort")
        .isEqualTo(SortResult.NEW_SORT);
    assertThat(response.hasExpectedSortAssignment())
        .as("This is a not a re-sort")
        .isFalse();

    verify(serviceAddressService, times(1))
        .setServiceAddressAsNonLawFirm(
            eq(
                SetServiceAddressAsNonLawFirmRequest
                    .newBuilder()
                    .setServiceAddressId(serviceAddress.getServiceAddressId())
                    .build()
            ),
            any(StreamObserver.class)
        );

    verify(lawFirmServiceAddressUpdateService).upsertLawFirmServiceAddressRecord(
        eq(
            LawFirmServiceAddressRecord
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
                ).build()
        ),
        any(StreamObserver.class)
    );

    verify(serviceAddressService).logSortDecision(
        eq(
            LogSortDecisionRequest
                .newBuilder()
                .setUsername(username)
                .setServiceAddress(serviceAddress)
                .setNonLawFirm(true)
                .setSortEffect(SortEffect.SORT_STATUS_UPDATED)
                .setSortResult(SortResult.NEW_SORT)
                .build()
        ),
        any(StreamObserver.class)
    );
  }

  @Test
  public void setServiceAddressAsNonLawFirm_re_sort() throws Exception {
    final ServiceAddress serviceAddress =
        ServiceAddress
            .newBuilder(createServiceAddressForNonLawFirm(faker.company().name()))
            .build();

    final String username = faker.name().username();
    when(userService.canPerformRealSort(eq(username))).thenReturn(true);

    replyWith(serviceAddress)
        .when(serviceAddressService)
        .getServiceAddressById(any(GetServiceAddressByIdRequest.class), any(StreamObserver.class));
    replyWithAckResponse()
        .when(serviceAddressService)
        .incrementSortScore(any(IncrementSortScoreRequest.class), any(StreamObserver.class));
    replyWithAckResponse()
        .when(serviceAddressService)
        .logSortDecision(any(LogSortDecisionRequest.class), any(StreamObserver.class));

    final SetServiceAddressAsNonLawFirmResponse response = serviceAddressSorter.setServiceAddressAsNonLawFirm(
        pi.admin.service_address_sorting.generated.SetServiceAddressAsNonLawFirmRequest
            .newBuilder()
            .setServiceAddressId(serviceAddress.getServiceAddressId())
            .setRequestedBy(username)
            .build()
    );

    assertThat(response.getSortEffect())
        .as("This is not a new sort and the user can do real sorts")
        .isEqualTo(SortEffect.SORT_SCORE_UPDATED);
    assertThat(response.getSortResult())
        .as("This is a new sort")
        .isEqualTo(SortResult.SAME);
    assertThat(response.hasExpectedSortAssignment())
        .as("This is a not a re-sort")
        .isTrue();
  }

  @Test
  public void getDesiredSortEffect_dry_run() throws Exception {
    when(userService.canPerformRealSort(anyString())).thenReturn(false);
    assertThat(serviceAddressSorter.getDesiredSortEffect(
        createUnsortedServiceAddress(faker.company().name()), faker.name().username())
    )
    .as("User is not allowed to perform a real sort")
    .isEqualTo(SortEffect.NOT_UPDATED);
    assertThat(serviceAddressSorter.getDesiredSortEffect(
        createServiceAddressForNonLawFirm(faker.company().name()), faker.name().username())
    )
    .as("User is not allowed to perform a real sort")
    .isEqualTo(SortEffect.NOT_UPDATED);
  }

  @Test
  public void getDesiredSortEffect_score_updated() throws Exception {
    when(userService.canPerformRealSort(anyString())).thenReturn(true);
    assertThat(serviceAddressSorter.getDesiredSortEffect(
        createServiceAddressForNonLawFirm(faker.company().name()), faker.name().username())
    )
    .as("User is allowed to perform a real sort but the service address is already sorted")
    .isEqualTo(SortEffect.SORT_SCORE_UPDATED);
    assertThat(serviceAddressSorter.getDesiredSortEffect(
        createServiceAddressToMatchLawFirm(LawFirmTestHelper.createLawFirm()), faker.name().username())
    )
    .as("User is allowed to perform a real sort but the service address is already sorted")
    .isEqualTo(SortEffect.SORT_SCORE_UPDATED);
    assertThat(serviceAddressSorter.getDesiredSortEffect(
        createServiceAddress(SortStatus.INSUFFICIENT_INFO), faker.name().username())
    )
    .as("User is allowed to perform a real sort but the service address is already sorted")
    .isEqualTo(SortEffect.SORT_SCORE_UPDATED);
  }

  @Test
  public void getDesiredSortEffect_sort_applied() throws Exception {
    when(userService.canPerformRealSort(anyString())).thenReturn(true);
    assertThat(serviceAddressSorter.getDesiredSortEffect(
        createUnsortedServiceAddress(faker.company().name()), faker.name().username())
    )
    .as("User is allowed to perform a real sort and the service address is unsorted")
    .isEqualTo(SortEffect.SORT_STATUS_UPDATED);
  }

  @Test
  public void updateSortScoreIfNecessary() throws Exception {
    replyWithAckResponse()
        .when(serviceAddressService)
        .incrementSortScore(any(IncrementSortScoreRequest.class), any(StreamObserver.class));
    replyWithAckResponse()
        .when(serviceAddressService)
        .decrementSortScore(any(DecrementSortScoreRequest.class), any(StreamObserver.class));
    final long id = faker.number().randomNumber(8, true);

    // Test no update required
    assertThat(serviceAddressSorter.updateSortScoreIfNecessary(id, SortEffect.SORT_STATUS_UPDATED, SortResult.SAME))
        .isFalse()
        .as("Sort score was not updated");
    assertThat(serviceAddressSorter.updateSortScoreIfNecessary(id, SortEffect.NOT_UPDATED, SortResult.DIFFERENT))
        .isFalse()
        .as("Sort score was not updated");
    verify(serviceAddressService, never())
        .incrementSortScore(any(IncrementSortScoreRequest.class), any(StreamObserver.class));
    verify(serviceAddressService, never())
        .decrementSortScore(any(DecrementSortScoreRequest.class), any(StreamObserver.class));

    // Test score increment
    assertThat(serviceAddressSorter.updateSortScoreIfNecessary(id, SortEffect.SORT_SCORE_UPDATED, SortResult.SAME))
        .isTrue()
        .as("Sort score was updated");
    verify(serviceAddressService, times(1))
        .incrementSortScore(any(IncrementSortScoreRequest.class), any(StreamObserver.class));

    // Test score decrement
    assertThat(serviceAddressSorter.updateSortScoreIfNecessary(id, SortEffect.SORT_SCORE_UPDATED, SortResult.DIFFERENT))
        .isTrue()
        .as("Sort score was updated");
    verify(serviceAddressService, times(1))
        .decrementSortScore(any(DecrementSortScoreRequest.class), any(StreamObserver.class));
  }

  @Test
  public void getCurrentSortAssignment_non_law_firm() throws Exception {
    final ServiceAddress serviceAddress = createServiceAddressForNonLawFirm(faker.name().fullName());
    assertThat(serviceAddressSorter.getCurrentSortAssignment(serviceAddress).getServiceAddress())
        .as("We should get back the service address")
        .isEqualTo(serviceAddress);
    assertThat(serviceAddressSorter.getCurrentSortAssignment(serviceAddress).hasAssignedLawFirm())
        .as("The service address is not assigned to a law firm")
        .isFalse();
  }

  @Test
  public void getCurrentSortAssignment_law_firm_not_found() throws Exception {
    final ServiceAddress serviceAddress = createServiceAddressToMatchLawFirm(LawFirmTestHelper.createLawFirm());
    replyWith(
        GetLawFirmByIdResponse
            .newBuilder()
            .setResult(GetLawFirmByIdResponse.GetLawFirmByIdResult.LAW_FIRM_NOT_FOUND)
            .build()
    )
    .when(lawFirmDbService).getLawFirmById(any(GetLawFirmByIdRequest.class), any(StreamObserver.class));
    assertThat(serviceAddressSorter.getCurrentSortAssignment(serviceAddress).getAssignedLawFirm().getLawFirmId())
        .as("The service address is considered to be assigned to a law firm even if we couldn't fetch it")
        .isEqualTo(serviceAddress.getLawFirmId().getValue());
  }

  @Test
  public void getCurrentSortAssignment_law_firm_found() throws Exception {
    final LawFirm lawFirm = LawFirmTestHelper.createLawFirm();
    final ServiceAddress serviceAddress = createServiceAddressToMatchLawFirm(lawFirm);
    replyWith(
        GetLawFirmByIdResponse
            .newBuilder()
            .setLawFirm(lawFirm)
            .setResult(GetLawFirmByIdResponse.GetLawFirmByIdResult.LAW_FIRM_LOOKUP_OK)
            .build()
    )
    .when(lawFirmDbService).getLawFirmById(any(GetLawFirmByIdRequest.class), any(StreamObserver.class));
    assertThat(serviceAddressSorter.getCurrentSortAssignment(serviceAddress).getAssignedLawFirm())
        .as("The service address is considered to be assigned to a law firm even if we couldn't fetch it")
        .isEqualTo(lawFirm);
  }

  @Test
  public void getLawFirmById_not_found() throws Exception {
    replyWith(
        GetLawFirmByIdResponse
            .newBuilder()
            .setResult(GetLawFirmByIdResponse.GetLawFirmByIdResult.LAW_FIRM_NOT_FOUND)
            .build()
    )
    .when(lawFirmDbService).getLawFirmById(any(GetLawFirmByIdRequest.class), any(StreamObserver.class));
    assertThat(serviceAddressSorter.getLawFirmById(1L))
        .as("The law firm was not found")
        .isEmpty();
  }

  @Test
  public void getLawFirmById_found() throws Exception {
    final LawFirm lawFirm = LawFirmTestHelper.createLawFirm();
    replyWith(
        GetLawFirmByIdResponse
            .newBuilder()
            .setLawFirm(lawFirm)
            .setResult(GetLawFirmByIdResponse.GetLawFirmByIdResult.LAW_FIRM_LOOKUP_OK)
            .build()
    )
    .when(lawFirmDbService).getLawFirmById(any(GetLawFirmByIdRequest.class), any(StreamObserver.class));
    assertThat(serviceAddressSorter.getLawFirmById(lawFirm.getLawFirmId()))
        .as("The law firm was found")
        .isEqualTo(Optional.of(lawFirm));
  }

  private Stubber replyWithAckResponse() {
    return replyWith(AckResponse.getDefaultInstance());
  }
}