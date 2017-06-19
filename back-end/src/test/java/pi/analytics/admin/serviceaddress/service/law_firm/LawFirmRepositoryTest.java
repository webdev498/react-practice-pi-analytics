/*
 * Copyright (c) 2017 Practice Insight Pty Ltd.
 */

package pi.analytics.admin.serviceaddress.service.law_firm;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;

import com.github.javafaker.Faker;

import org.assertj.core.util.Lists;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.UUID;

import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import io.practiceinsight.licensingalert.citationsearch.generated.LawFirmSearchRequest;
import io.practiceinsight.licensingalert.citationsearch.generated.LawFirmSearchResult;
import io.practiceinsight.licensingalert.citationsearch.generated.NakedLawFirmSearchServiceGrpc;
import pi.admin.service_address_sorting.generated.Agent;
import pi.admin.service_address_sorting.generated.CreateLawFirmRequest;
import pi.analytics.admin.serviceaddress.service.helpers.LawFirmTestHelper;
import pi.ip.data.relational.generated.AssignServiceAddressToLawFirmRequest;
import pi.ip.data.relational.generated.CreateLawFirmResponse;
import pi.ip.data.relational.generated.GetServiceAddressesForLawFirmRequest;
import pi.ip.data.relational.generated.GetServiceAddressesForLawFirmResponse;
import pi.ip.data.relational.generated.LawFirmDbServiceGrpc;
import pi.ip.data.relational.generated.ServiceAddressServiceGrpc;
import pi.ip.generated.es.ESMutationServiceGrpc;
import pi.ip.generated.es.LocationRecord;
import pi.ip.generated.es.ThinLawFirmRecord;
import pi.ip.generated.es.ThinLawFirmServiceAddressRecord;
import pi.ip.generated.es.ThinServiceAddressRecord;
import pi.ip.generated.queue.DeleteUnitRequest;
import pi.ip.generated.queue.QueueOnPremGrpc;
import pi.ip.proto.generated.AckResponse;
import pi.ip.proto.generated.LawFirm;
import pi.ip.proto.generated.ServiceAddress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static pi.analytics.admin.serviceaddress.service.helpers.GrpcTestHelper.replyWith;
import static pi.analytics.admin.serviceaddress.service.helpers.ServiceAddressTestHelper.createServiceAddressToMatchLawFirm;
import static pi.analytics.admin.serviceaddress.service.helpers.ServiceAddressTestHelper.createUnsortedServiceAddress;

/**
 * @author shane.xie@practiceinsight.io
 */
public class LawFirmRepositoryTest {

  private Faker faker = new Faker();
  private LawFirmDbServiceGrpc.LawFirmDbServiceImplBase lawFirmDbService;
  private NakedLawFirmSearchServiceGrpc.NakedLawFirmSearchServiceImplBase lawFirmSearchService;
  private ServiceAddressServiceGrpc.ServiceAddressServiceImplBase serviceAddressService;
  private ESMutationServiceGrpc.ESMutationServiceImplBase esMutationService;
  private QueueOnPremGrpc.QueueOnPremImplBase queueOnPrem;
  private Server server;
  private ManagedChannel channel;
  private LawFirmRepository lawFirmRepository;

  @Before
  public void setup() throws Exception {
    lawFirmDbService = mock(LawFirmDbServiceGrpc.LawFirmDbServiceImplBase.class);
    lawFirmSearchService = mock(NakedLawFirmSearchServiceGrpc.NakedLawFirmSearchServiceImplBase.class);
    serviceAddressService = mock(ServiceAddressServiceGrpc.ServiceAddressServiceImplBase.class);
    esMutationService = mock(ESMutationServiceGrpc.ESMutationServiceImplBase.class);
    queueOnPrem = mock(QueueOnPremGrpc.QueueOnPremImplBase.class);

    final String serverName = "law-firm-helper-test-".concat(UUID.randomUUID().toString());
    server =
        InProcessServerBuilder
            .forName(serverName)
            .addService(lawFirmDbService.bindService())
            .addService(lawFirmSearchService.bindService())
            .addService(serviceAddressService.bindService())
            .addService(esMutationService.bindService())
            .addService(queueOnPrem.bindService())
            .directExecutor()
            .build()
            .start();
    channel =
        InProcessChannelBuilder
            .forName(serverName)
            .directExecutor()
            .build();

    lawFirmRepository = Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        bind(LawFirmDbServiceGrpc.LawFirmDbServiceBlockingStub.class)
            .toInstance(LawFirmDbServiceGrpc.newBlockingStub(channel));
        bind(NakedLawFirmSearchServiceGrpc.NakedLawFirmSearchServiceBlockingStub.class)
            .toInstance(NakedLawFirmSearchServiceGrpc.newBlockingStub(channel));
        bind(ServiceAddressServiceGrpc.ServiceAddressServiceBlockingStub.class)
            .toInstance(ServiceAddressServiceGrpc.newBlockingStub(channel));
        bind(ESMutationServiceGrpc.ESMutationServiceBlockingStub.class)
            .toInstance(ESMutationServiceGrpc.newBlockingStub(channel));
        bind(QueueOnPremGrpc.QueueOnPremBlockingStub.class)
            .toInstance(QueueOnPremGrpc.newBlockingStub(channel));
      }
    }).getInstance(LawFirmRepository.class);
  }

  @After
  public void tearDown() throws Exception {
    channel.shutdown();
    server.shutdownNow();
  }

  @Test
  public void searchLawFirms() throws Exception {
    // Set up law firm search results
    final LawFirm lawFirm1 = LawFirmTestHelper.createLawFirm();
    final LawFirm lawFirm2 = LawFirmTestHelper.createLawFirm();

    replyWith(
        LawFirmSearchResult
            .newBuilder()
            .addLawFirms(lawFirm1)
            .addLawFirms(lawFirm2)
            .build()
    )
    .when(lawFirmSearchService)
    .search(any(LawFirmSearchRequest.class), any(StreamObserver.class));

    // Set up service address fetching
    final List<ServiceAddress> lawFirm1ServiceAddresses = Lists.newArrayList(createServiceAddressToMatchLawFirm(lawFirm1));
    setupGetServiceAddressesForLawFirmAnswer(lawFirm1.getLawFirmId(), lawFirm1ServiceAddresses);

    final List<ServiceAddress> lawFirm2ServiceAddresses = Lists.newArrayList(
        createServiceAddressToMatchLawFirm(lawFirm2), createServiceAddressToMatchLawFirm(lawFirm2));
    setupGetServiceAddressesForLawFirmAnswer(lawFirm2.getLawFirmId(), lawFirm2ServiceAddresses);

    assertThat(lawFirmRepository.searchLawFirms("test query")).containsExactly(
        Agent.newBuilder().setLawFirm(lawFirm1).addAllServiceAddresses(lawFirm1ServiceAddresses).build(),
        Agent.newBuilder().setLawFirm(lawFirm2).addAllServiceAddresses(lawFirm2ServiceAddresses).build()
    );
  }

  @Test
  public void createLawFirm() throws Exception {
    final CreateLawFirmRequest createLawFirmRequest =
        CreateLawFirmRequest
            .newBuilder()
            .setRequestedBy(faker.name().username())
            .setName(faker.company().name())
            .setState(faker.address().state())
            .setCountryCode(faker.address().countryCode())
            .setWebsiteUrl(faker.internet().url())
            .setServiceAddress(createUnsortedServiceAddress(faker.company().name()))
            .build();

    final long newLawFirmId = faker.number().randomDigitNotZero();

    // lawFirmDbService.createLawFirm()
    replyWith(
        CreateLawFirmResponse
            .newBuilder()
            .setLawFirmId(newLawFirmId)
            .build()
    )
    .when(lawFirmDbService)
    .createLawFirm(any(pi.ip.data.relational.generated.CreateLawFirmRequest.class), any(StreamObserver.class));

    // serviceAddressServiceBlockingStub.assignServiceAddressToLawFirm()
    replyWith(AckResponse.getDefaultInstance())
        .when(serviceAddressService)
        .assignServiceAddressToLawFirm(any(AssignServiceAddressToLawFirmRequest.class), any(StreamObserver.class));

    // datastoreSg3ServiceBlockingStub.upsertIntoLawFirmCaches()
    replyWith(AckResponse.getDefaultInstance())
        .when(esMutationService)
        .upsertLawFirm(any(LawFirm.class), any(StreamObserver.class));

    // datastoreSg3ServiceBlockingStub.upsertThinLawFirmServiceAddress()
    replyWith(AckResponse.getDefaultInstance())
        .when(esMutationService)
        .upsertThinLawFirmServiceAddressRecord(any(ThinLawFirmServiceAddressRecord.class), any(StreamObserver.class));

    // queueOnPremBlockingStub.deleteQueueUnit()
    replyWith(AckResponse.getDefaultInstance())
        .when(queueOnPrem)
        .deleteQueueUnit(any(DeleteUnitRequest.class), any(StreamObserver.class));

    // Run test
    lawFirmRepository.createLawFirm(createLawFirmRequest);

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
    verify(esMutationService, times(1))
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
  }

  private void setupGetServiceAddressesForLawFirmAnswer(final long lawFirmId, final List<ServiceAddress> serviceAddresses) {
    replyWith(
        GetServiceAddressesForLawFirmResponse
            .newBuilder()
            .addAllServiceAddresses(serviceAddresses)
            .build()
  )
  .when(serviceAddressService)
    .getServiceAddressesForLawFirm(eq(GetServiceAddressesForLawFirmRequest.newBuilder().setLawFirmId(lawFirmId).build()),
        any(StreamObserver.class));
  }
}