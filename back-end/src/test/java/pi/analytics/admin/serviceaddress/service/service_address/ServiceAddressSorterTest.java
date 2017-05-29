/*
 * Copyright (c) 2017 Practice Insight Pty Ltd.
 */

package pi.analytics.admin.serviceaddress.service.service_address;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.protobuf.Int64Value;

import com.github.javafaker.Faker;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.stubbing.Stubber;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import pi.admin.service_address_sorting.generated.AssignServiceAddressRequest;
import pi.admin.service_address_sorting.generated.SetSortingImpossibleRequest;
import pi.admin.service_address_sorting.generated.SkipServiceAddressRequest;
import pi.admin.service_address_sorting.generated.UnsortServiceAddressRequest;
import pi.ip.data.relational.generated.AssignServiceAddressToLawFirmRequest;
import pi.ip.data.relational.generated.GetLawFirmByIdRequest;
import pi.ip.data.relational.generated.GetLawFirmByIdResponse;
import pi.ip.data.relational.generated.GetServiceAddressByIdRequest;
import pi.ip.data.relational.generated.LawFirmDbServiceGrpc;
import pi.ip.data.relational.generated.ServiceAddressServiceGrpc;
import pi.ip.data.relational.generated.SetServiceAddressAsNonLawFirmRequest;
import pi.ip.data.relational.generated.UnassignServiceAddressFromLawFirmRequest;
import pi.ip.generated.datastore_sg3.DatastoreSg3ServiceGrpc;
import pi.ip.generated.datastore_sg3.IpDatastoreSg3.ThinLawFirm;
import pi.ip.generated.datastore_sg3.IpDatastoreSg3.ThinLawFirmServiceAddress;
import pi.ip.generated.datastore_sg3.IpDatastoreSg3.ThinServiceAddress;
import pi.ip.generated.queue.AddUnitRequestOnPrem;
import pi.ip.generated.queue.DelayUnitRequest;
import pi.ip.generated.queue.DeleteUnitRequest;
import pi.ip.generated.queue.MsgUnit;
import pi.ip.generated.queue.QueueNameOnPrem;
import pi.ip.generated.queue.QueueOnPremGrpc;
import pi.ip.proto.generated.AckResponse;
import pi.ip.proto.generated.LawFirm;
import pi.ip.proto.generated.ServiceAddress;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static pi.analytics.admin.serviceaddress.service.helpers.GrpcTestHelper.replyWith;
import static pi.analytics.admin.serviceaddress.service.helpers.LawFirmTestHelper.createLawFirm;
import static pi.analytics.admin.serviceaddress.service.helpers.ServiceAddressTestHelper.createServiceAddressForNonLawFirm;
import static pi.analytics.admin.serviceaddress.service.helpers.ServiceAddressTestHelper.createServiceAddressToMatchLawFirm;

//import pi.ip.data.relational.generated.SetServiceAddressAsNonLawFirmRequest;

/**
 * @author shane.xie@practiceinsight.io
 */
public class ServiceAddressSorterTest {

  private Faker faker = new Faker();
  private LawFirmDbServiceGrpc.LawFirmDbServiceImplBase lawFirmDbService;
  private ServiceAddressServiceGrpc.ServiceAddressServiceImplBase serviceAddressService;
  private DatastoreSg3ServiceGrpc.DatastoreSg3ServiceImplBase datastoreSg3Service;
  private QueueOnPremGrpc.QueueOnPremImplBase queueOnPrem;
  private Server server;
  private ManagedChannel channel;
  private ServiceAddressSorter serviceAddressSorter;

  @Before
  public void setUp() throws Exception {
    lawFirmDbService = mock(LawFirmDbServiceGrpc.LawFirmDbServiceImplBase.class);
    serviceAddressService = mock(ServiceAddressServiceGrpc.ServiceAddressServiceImplBase.class);
    datastoreSg3Service = mock(DatastoreSg3ServiceGrpc.DatastoreSg3ServiceImplBase.class);
    queueOnPrem = mock(QueueOnPremGrpc.QueueOnPremImplBase.class);

    final String serverName = "service-address-sorter-test-".concat(UUID.randomUUID().toString());
    server =
        InProcessServerBuilder
            .forName(serverName)
            .addService(lawFirmDbService.bindService())
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

    serviceAddressSorter = Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        bind(LawFirmDbServiceGrpc.LawFirmDbServiceBlockingStub.class)
            .toInstance(LawFirmDbServiceGrpc.newBlockingStub(channel));
        bind(ServiceAddressServiceGrpc.ServiceAddressServiceBlockingStub.class)
            .toInstance(ServiceAddressServiceGrpc.newBlockingStub(channel));
        bind(DatastoreSg3ServiceGrpc.DatastoreSg3ServiceBlockingStub.class)
            .toInstance(DatastoreSg3ServiceGrpc.newBlockingStub(channel));
        bind(QueueOnPremGrpc.QueueOnPremBlockingStub.class)
            .toInstance(QueueOnPremGrpc.newBlockingStub(channel));
      }
    }).getInstance(ServiceAddressSorter.class);
  }

  @After
  public void tearDown() throws Exception {
    channel.shutdown();
    server.shutdownNow();
  }

  @Test
  public void assignServiceAddress() throws Exception {
    final LawFirm lawFirm = createLawFirm();
    final ServiceAddress serviceAddress = createServiceAddressToMatchLawFirm(lawFirm);

    replyWithAckResponse()
        .when(serviceAddressService)
        .assignServiceAddressToLawFirm(any(AssignServiceAddressToLawFirmRequest.class), any(StreamObserver.class));

    replyWithAckResponse()
        .when(datastoreSg3Service)
        .upsertThinLawFirmServiceAddress(any(ThinLawFirmServiceAddress.class), any(StreamObserver.class));

    replyWithAckResponse()
        .when(queueOnPrem)
        .deleteQueueUnit(any(DeleteUnitRequest.class), any(StreamObserver.class));

    replyWith(GetLawFirmByIdResponse.newBuilder().setLawFirm(lawFirm).build())
        .when(lawFirmDbService)
        .getLawFirmById(any(GetLawFirmByIdRequest.class), any(StreamObserver.class));

    replyWith(serviceAddress)
        .when(serviceAddressService)
        .getServiceAddressById(any(GetServiceAddressByIdRequest.class), any(StreamObserver.class));

    serviceAddressSorter.assignServiceAddress(
        AssignServiceAddressRequest
            .newBuilder()
            .setUnsortedServiceAddressQueueItemId("123")
            .setLawFirmId(lawFirm.getLawFirmId())
            .setServiceAddressId(serviceAddress.getServiceAddressId())
            .build()
    );

    verify(datastoreSg3Service).upsertThinLawFirmServiceAddress(
        eq(
            ThinLawFirmServiceAddress
                .newBuilder()
                .setNotALawFirm(false)
                .setThinLawFirm(
                    ThinLawFirm.newBuilder()
                        .setId(lawFirm.getLawFirmId())
                        .setName(lawFirm.getName())
                )
                .setThinServiceAddress(
                    ThinServiceAddress
                        .newBuilder()
                        .setServiceAddressId(serviceAddress.getServiceAddressId())
                        .setNameAddress(serviceAddress.getName() + " " + serviceAddress.getAddress())
                        .setCountry(serviceAddress.getCountry())
                        .setLongitude(serviceAddress.getLongitude())
                        .setLatitude(serviceAddress.getLatitude())
                ).build()
        ),
        any(StreamObserver.class)
    );
    verify(queueOnPrem).deleteQueueUnit(
        eq(DeleteUnitRequest.newBuilder().setDbId("123").build()),
        any(StreamObserver.class)
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
  }

  @Test
  public void unsortServiceAddress() throws Exception {
    replyWithAckResponse()
        .when(serviceAddressService)
        .unassignServiceAddressFromLawFirm(any(UnassignServiceAddressFromLawFirmRequest.class), any(StreamObserver.class));
    replyWithAckResponse()
        .when(datastoreSg3Service).deleteThinLawFirmServiceAddress(any(Int64Value.class), any(StreamObserver.class));
    replyWithAckResponse()
        .when(queueOnPrem).addUnit(any(AddUnitRequestOnPrem.class), any(StreamObserver.class));
    serviceAddressSorter.unsortServiceAddress(UnsortServiceAddressRequest.newBuilder().setServiceAddressId(1L).build());
    verify(serviceAddressService, times(1))
        .unassignServiceAddressFromLawFirm(
            eq(UnassignServiceAddressFromLawFirmRequest.newBuilder().setServiceAddressId(1L).build()),
            any(StreamObserver.class)
        );
    verify(datastoreSg3Service, times(1))
        .deleteThinLawFirmServiceAddress(eq(Int64Value.newBuilder().setValue(1L).build()), any(StreamObserver.class));
    final AddUnitRequestOnPrem addUnitRequestOnPrem =
        AddUnitRequestOnPrem
            .newBuilder()
            .setQueueNameOnPrem(QueueNameOnPrem.ServiceAddrSort_en)
            .setDelaySeconds((int) TimeUnit.DAYS.toSeconds(1))
            .addMsgUnit(MsgUnit.newBuilder().setUniqueMsgKey(String.valueOf(1L)).build())
            .build();
    verify(queueOnPrem, times(1))
        .addUnit(eq(addUnitRequestOnPrem), any(StreamObserver.class));
  }

  @Test
  public void setServiceAddressAsNonLawFirm() throws Exception {
    final ServiceAddress serviceAddress = createServiceAddressForNonLawFirm(faker.company().name());

    replyWithAckResponse()
        .when(serviceAddressService)
        .setServiceAddressAsNonLawFirm(any(SetServiceAddressAsNonLawFirmRequest.class), any(StreamObserver.class));
    replyWith(serviceAddress)
        .when(serviceAddressService)
        .getServiceAddressById(any(GetServiceAddressByIdRequest.class), any(StreamObserver.class));
    replyWithAckResponse()
        .when(datastoreSg3Service)
        .upsertThinLawFirmServiceAddress(any(ThinLawFirmServiceAddress.class), any(StreamObserver.class));
    replyWithAckResponse()
        .when(queueOnPrem).deleteQueueUnit(any(DeleteUnitRequest.class), any(StreamObserver.class));

    serviceAddressSorter.setServiceAddressAsNonLawFirm(
        pi.admin.service_address_sorting.generated.SetServiceAddressAsNonLawFirmRequest
            .newBuilder().setServiceAddressId(1L).build());

    verify(datastoreSg3Service).upsertThinLawFirmServiceAddress(
        eq(
            ThinLawFirmServiceAddress
                .newBuilder()
                .setNotALawFirm(true)
                .setThinServiceAddress(
                    ThinServiceAddress
                        .newBuilder()
                        .setServiceAddressId(serviceAddress.getServiceAddressId())
                        .setNameAddress(serviceAddress.getName() + " " + serviceAddress.getAddress())
                        .setCountry(serviceAddress.getCountry())
                        .setLongitude(serviceAddress.getLongitude())
                        .setLatitude(serviceAddress.getLatitude())
                ).build()
        ),
        any(StreamObserver.class)
    );
    verify(queueOnPrem, times(1))
        .deleteQueueUnit(any(DeleteUnitRequest.class), any(StreamObserver.class));
  }

  @Test
  public void skipServiceAddress() throws Exception {
    replyWithAckResponse()
        .when(queueOnPrem)
        .delayQueueUnit(any(DelayUnitRequest.class), any(StreamObserver.class));

    serviceAddressSorter.skipServiceAddress(
        SkipServiceAddressRequest
            .newBuilder()
            .setUnsortedServiceAddressQueueItemId("123")
            .setDelayMinutes(5)
            .setRequestedBy("shane")
            .build()
    );

    verify(queueOnPrem, times(1))
        .delayQueueUnit(
            eq(DelayUnitRequest.newBuilder().setDbId("123").setDelaySeconds(5 * 60).build()),
            any(StreamObserver.class)
        );
  }

  @Test
  public void setSortingImpossible() throws Exception {
    replyWithAckResponse()
        .when(queueOnPrem)
        .delayQueueUnit(any(DelayUnitRequest.class), any(StreamObserver.class));

    serviceAddressSorter.setSortingImpossible(
        SetSortingImpossibleRequest
            .newBuilder()
            .setUnsortedServiceAddressQueueItemId("123")
            .setServiceAddressId(123)
            .setRequestedBy("shane")
            .build()
    );

    verify(queueOnPrem, times(1))
        .delayQueueUnit(
            eq(DelayUnitRequest.newBuilder().setDbId("123").setDelaySeconds(30 * 24 * 60 * 60).build()),
            any(StreamObserver.class)
        );

  }

  private Stubber replyWithAckResponse() {
    return replyWith(AckResponse.getDefaultInstance());
  }
}