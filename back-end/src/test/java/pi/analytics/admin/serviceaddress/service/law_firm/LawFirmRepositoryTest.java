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
import io.practiceinsight.licensingalert.citationsearch.generated.LawFirmSearchServiceGrpc;
import pi.admin.service_address_sorting.generated.Agent;
import pi.analytics.admin.serviceaddress.service.helpers.LawFirmTestHelper;
import pi.ip.data.relational.generated.GetServiceAddressesForLawFirmRequest;
import pi.ip.data.relational.generated.GetServiceAddressesForLawFirmResponse;
import pi.ip.data.relational.generated.LawFirmDbServiceGrpc;
import pi.ip.data.relational.generated.ServiceAddressServiceGrpc;
import pi.ip.generated.datastore_sg3.DatastoreSg3ServiceGrpc;
import pi.ip.generated.queue.QueueOnPremGrpc;
import pi.ip.proto.generated.LawFirm;
import pi.ip.proto.generated.ServiceAddress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static pi.analytics.admin.serviceaddress.service.helpers.ServiceAddressTestHelper.createServiceAddressToMatchLawFirm;

/**
 * @author shane.xie@practiceinsight.io
 */
public class LawFirmRepositoryTest {

  private Faker faker = new Faker();
  private LawFirmDbServiceGrpc.LawFirmDbServiceImplBase lawFirmDbService;
  private LawFirmSearchServiceGrpc.LawFirmSearchServiceImplBase lawFirmSearchService;
  private ServiceAddressServiceGrpc.ServiceAddressServiceImplBase serviceAddressService;
  private DatastoreSg3ServiceGrpc.DatastoreSg3ServiceImplBase datastoreSg3Service;
  private QueueOnPremGrpc.QueueOnPremImplBase queueOnPrem;
  private Server server;
  private ManagedChannel channel;
  private LawFirmRepository lawFirmRepository;

  @Before
  public void setup() throws Exception {
    lawFirmDbService = mock(LawFirmDbServiceGrpc.LawFirmDbServiceImplBase.class);
    lawFirmSearchService = mock(LawFirmSearchServiceGrpc.LawFirmSearchServiceImplBase.class);
    serviceAddressService = mock(ServiceAddressServiceGrpc.ServiceAddressServiceImplBase.class);
    datastoreSg3Service = mock(DatastoreSg3ServiceGrpc.DatastoreSg3ServiceImplBase.class);
    queueOnPrem = mock(QueueOnPremGrpc.QueueOnPremImplBase.class);

    final String serverName = "law-firm-helper-test-".concat(UUID.randomUUID().toString());
    server =
        InProcessServerBuilder
            .forName(serverName)
            .addService(lawFirmDbService.bindService())
            .addService(lawFirmSearchService.bindService())
            .addService(serviceAddressService.bindService())
            .addService(datastoreSg3Service.bindService())
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
        bind(LawFirmSearchServiceGrpc.LawFirmSearchServiceBlockingStub.class)
            .toInstance(LawFirmSearchServiceGrpc.newBlockingStub(channel));
        bind(ServiceAddressServiceGrpc.ServiceAddressServiceBlockingStub.class)
            .toInstance(ServiceAddressServiceGrpc.newBlockingStub(channel));
        bind(DatastoreSg3ServiceGrpc.DatastoreSg3ServiceBlockingStub.class)
            .toInstance(DatastoreSg3ServiceGrpc.newBlockingStub(channel));
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

    doAnswer(invocation -> {
      StreamObserver<LawFirmSearchResult> responseObserver =
          (StreamObserver<LawFirmSearchResult>) invocation.getArguments()[1];
      responseObserver.onNext(
          LawFirmSearchResult
              .newBuilder()
              .addLawFirms(lawFirm1)
              .addLawFirms(lawFirm2)
              .build()
      );
      responseObserver.onCompleted();
      return null;
    })
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
    // TODO
  }

  private void setupGetServiceAddressesForLawFirmAnswer(final long lawFirmId, final List<ServiceAddress> serviceAddresses) {
    doAnswer(invocation -> {
      StreamObserver<GetServiceAddressesForLawFirmResponse> responseObserver =
          (StreamObserver<GetServiceAddressesForLawFirmResponse>) invocation.getArguments()[1];
      responseObserver.onNext(
          GetServiceAddressesForLawFirmResponse
              .newBuilder()
              .addAllServiceAddresses(serviceAddresses)
              .build()
      );
      responseObserver.onCompleted();
      return null;
    })
    .when(serviceAddressService)
    .getServiceAddressesForLawFirm(eq(GetServiceAddressesForLawFirmRequest.newBuilder().setLawFirmId(lawFirmId).build()),
        any(StreamObserver.class));
  }
}