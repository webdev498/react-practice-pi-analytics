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

import java.util.UUID;

import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import pi.admin.service_address_sorting.generated.AssignServiceAddressRequest;
import pi.admin.service_address_sorting.generated.CreateLawFirmRequest;
import pi.admin.service_address_sorting.generated.SetInsufficientInfoStatusRequest;
import pi.admin.service_address_sorting.generated.UnsortServiceAddressRequest;
import pi.analytics.admin.serviceaddress.service.helpers.LawFirmTestHelper;
import pi.analytics.admin.serviceaddress.service.law_firm.LawFirmRepository;
import pi.analytics.admin.serviceaddress.service.user.UserService;
import pi.ip.data.relational.generated.AssignServiceAddressToLawFirmRequest;
import pi.ip.data.relational.generated.CreateLawFirmResponse;
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
import pi.ip.data.relational.generated.SortEffect;
import pi.ip.data.relational.generated.SortResult;
import pi.ip.data.relational.generated.UnassignServiceAddressFromLawFirmRequest;
import pi.ip.generated.es.ESMutationServiceGrpc;
import pi.ip.generated.es.LocationRecord;
import pi.ip.generated.es.ThinLawFirmRecord;
import pi.ip.generated.es.ThinLawFirmServiceAddressRecord;
import pi.ip.generated.es.ThinLawFirmServiceAddressUpdateServiceGrpc;
import pi.ip.generated.es.ThinServiceAddressRecord;
import pi.ip.proto.generated.AckResponse;
import pi.ip.proto.generated.LawFirm;
import pi.ip.proto.generated.ServiceAddress;
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
  private ThinLawFirmServiceAddressUpdateServiceGrpc.ThinLawFirmServiceAddressUpdateServiceImplBase
      thinLawFirmServiceAddressUpdateService;
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
    thinLawFirmServiceAddressUpdateService = mock(ThinLawFirmServiceAddressUpdateServiceGrpc
        .ThinLawFirmServiceAddressUpdateServiceImplBase.class);

    final String serverName = "service-address-sorter-test-".concat(UUID.randomUUID().toString());
    server =
        InProcessServerBuilder
            .forName(serverName)
            .addService(lawFirmDbService.bindService())
            .addService(serviceAddressService.bindService())
            .addService(esMutationService.bindService())
            .addService(thinLawFirmServiceAddressUpdateService.bindService())
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
        bind(ThinLawFirmServiceAddressUpdateServiceGrpc.ThinLawFirmServiceAddressUpdateServiceBlockingStub.class)
            .toInstance(ThinLawFirmServiceAddressUpdateServiceGrpc.newBlockingStub(channel));
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
  public void assignServiceAddress_success() throws Exception {
    final LawFirm lawFirm = LawFirmTestHelper.createLawFirm();
    final ServiceAddress serviceAddress = createUnsortedServiceAddress(lawFirm.getName());

    final String username = faker.name().username();
    when(userService.canPerformRealSort(eq(username))).thenReturn(true);

    replyWithAckResponse()
        .when(serviceAddressService)
        .assignServiceAddressToLawFirm(any(AssignServiceAddressToLawFirmRequest.class), any(StreamObserver.class));
    replyWithAckResponse()
        .when(thinLawFirmServiceAddressUpdateService)
        .upsertThinLawFirmServiceAddressRecord(any(ThinLawFirmServiceAddressRecord.class), any(StreamObserver.class));
    replyWith(GetLawFirmByIdResponse.newBuilder().setLawFirm(lawFirm).build())
        .when(lawFirmDbService)
        .getLawFirmById(any(GetLawFirmByIdRequest.class), any(StreamObserver.class));
    replyWith(serviceAddress)
        .when(serviceAddressService)
        .getServiceAddressById(any(GetServiceAddressByIdRequest.class), any(StreamObserver.class));
    replyWithAckResponse()
        .when(serviceAddressService)
        .logSortDecision(any(LogSortDecisionRequest.class), any(StreamObserver.class));

    serviceAddressSorter.assignServiceAddress(
        AssignServiceAddressRequest
            .newBuilder()
            .setLawFirmId(lawFirm.getLawFirmId())
            .setServiceAddressId(serviceAddress.getServiceAddressId())
            .setRequestedBy(username)
            .build()
    );

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

    verify(thinLawFirmServiceAddressUpdateService).upsertThinLawFirmServiceAddressRecord(
        eq(
            ThinLawFirmServiceAddressRecord
                .newBuilder()
                .setLawFirmFlag(true)
                .setLawFirm(
                    ThinLawFirmRecord
                        .newBuilder()
                        .setId(lawFirm.getLawFirmId())
                        .setName(lawFirm.getName())
                )
                .setServiceAddress(
                    ThinServiceAddressRecord
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
  public void createLawFirm() throws Exception {
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
        CreateLawFirmResponse
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
        .when(thinLawFirmServiceAddressUpdateService)
        .upsertThinLawFirmServiceAddressRecord(any(ThinLawFirmServiceAddressRecord.class), any(StreamObserver.class));
    final ArgumentCaptor<LogSortDecisionRequest> logSortDecisionRequestArgument =
        ArgumentCaptor.forClass(LogSortDecisionRequest.class);
    replyWithAckResponse()
        .when(serviceAddressService)
        .logSortDecision(logSortDecisionRequestArgument.capture(), any(StreamObserver.class));

    // Run test
    serviceAddressSorter.createLawFirmAndAssignServiceAddress(createLawFirmRequest);

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

    // Verify upsert into law firm caches called
    verify(esMutationService, times(1))
        .upsertLawFirm(
            eq(
                LawFirm
                    .newBuilder()
                    .setLawFirmId(newLawFirmId)
                    .setName(createLawFirmRequest.getName())
                    .setStateStr(createLawFirmRequest.getState())
                    .setCountry(createLawFirmRequest.getCountryCode())
                    .setWebsiteUrl(createLawFirmRequest.getWebsiteUrl())
                    .build()
            ),
            any(StreamObserver.class)
        );

    // Verify upsert thin law firm service address called
    verify(thinLawFirmServiceAddressUpdateService, times(1))
        .upsertThinLawFirmServiceAddressRecord(
            eq(
                ThinLawFirmServiceAddressRecord
                    .newBuilder()
                    .setServiceAddress(
                        ThinServiceAddressRecord
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
                    .setLawFirm(
                        ThinLawFirmRecord
                            .newBuilder()
                            .setId(newLawFirmId)
                            .setName(createLawFirmRequest.getName())
                    )
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
  public void unsortServiceAddress() throws Exception {
    replyWithAckResponse()
        .when(serviceAddressService)
        .unassignServiceAddressFromLawFirm(any(UnassignServiceAddressFromLawFirmRequest.class), any(StreamObserver.class));
    replyWithAckResponse()
        .when(thinLawFirmServiceAddressUpdateService)
        .deleteThinLawFirmServiceAddressRecord(any(Int64Value.class), any(StreamObserver.class));
    serviceAddressSorter.unsortServiceAddress(UnsortServiceAddressRequest.newBuilder().setServiceAddressId(1L).build());
    verify(serviceAddressService, times(1))
        .unassignServiceAddressFromLawFirm(
            eq(UnassignServiceAddressFromLawFirmRequest.newBuilder().setServiceAddressId(1L).build()),
            any(StreamObserver.class)
        );
    verify(thinLawFirmServiceAddressUpdateService, times(1))
        .deleteThinLawFirmServiceAddressRecord(eq(Int64Value.newBuilder().setValue(1L).build()), any(StreamObserver.class));
  }

  @Test
  public void testSetInsufficientInfoSort() {
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

    serviceAddressSorter.setInsufficientInfoToSort(
        SetInsufficientInfoStatusRequest
            .newBuilder()
            .setServiceAddressId(1L)
            .setRequestedBy(username)
            .build()
    );

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
  public void setServiceAddressAsNonLawFirm() throws Exception {
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
        .when(thinLawFirmServiceAddressUpdateService)
        .upsertThinLawFirmServiceAddressRecord(any(ThinLawFirmServiceAddressRecord.class), any(StreamObserver.class));
    replyWithAckResponse()
        .when(serviceAddressService)
        .logSortDecision(any(LogSortDecisionRequest.class), any(StreamObserver.class));

    serviceAddressSorter.setServiceAddressAsNonLawFirm(
        pi.admin.service_address_sorting.generated.SetServiceAddressAsNonLawFirmRequest
            .newBuilder()
            .setServiceAddressId(serviceAddress.getServiceAddressId())
            .setRequestedBy(username)
            .build()
    );

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

    verify(thinLawFirmServiceAddressUpdateService).upsertThinLawFirmServiceAddressRecord(
        eq(
            ThinLawFirmServiceAddressRecord
                .newBuilder()
                .setLawFirmFlag(false)
                .setServiceAddress(
                    ThinServiceAddressRecord
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

  private Stubber replyWithAckResponse() {
    return replyWith(AckResponse.getDefaultInstance());
  }
}