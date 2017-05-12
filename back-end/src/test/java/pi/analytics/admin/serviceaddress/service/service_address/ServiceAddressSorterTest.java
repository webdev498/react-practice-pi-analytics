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

import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import pi.admin.service_address_sorting.generated.AssignServiceAddressRequest;
import pi.admin.service_address_sorting.generated.SkipServiceAddressRequest;
import pi.ip.data.relational.generated.AssignServiceAddressToLawFirmRequest;
import pi.ip.data.relational.generated.GetServiceAddressByIdRequest;
import pi.ip.data.relational.generated.ServiceAddressServiceGrpc;
import pi.ip.generated.datastore_sg3.DatastoreSg3ServiceGrpc;
import pi.ip.generated.datastore_sg3.IpDatastoreSg3;
import pi.ip.generated.queue.DelayUnitRequest;
import pi.ip.generated.queue.DeleteUnitRequest;
import pi.ip.generated.queue.QueueOnPremGrpc;
import pi.ip.proto.generated.AckResponse;
import pi.ip.proto.generated.ServiceAddress;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static pi.analytics.admin.serviceaddress.service.helpers.GrpcTestHelper.replyWith;

/**
 * @author shane.xie@practiceinsight.io
 */
public class ServiceAddressSorterTest {

  private ServiceAddressServiceGrpc.ServiceAddressServiceImplBase serviceAddressService;
  private DatastoreSg3ServiceGrpc.DatastoreSg3ServiceImplBase datastoreSg3Service;
  private QueueOnPremGrpc.QueueOnPremImplBase queueOnPrem;
  private Server server;
  private ManagedChannel channel;
  private ServiceAddressSorter serviceAddressSorter;
  private Faker faker;

  @Before
  public void setUp() throws Exception {
    serviceAddressService = mock(ServiceAddressServiceGrpc.ServiceAddressServiceImplBase.class);
    datastoreSg3Service = mock(DatastoreSg3ServiceGrpc.DatastoreSg3ServiceImplBase.class);
    queueOnPrem = mock(QueueOnPremGrpc.QueueOnPremImplBase.class);
    faker = new Faker();

    final String serverName = "service-address-sorter-test-".concat(UUID.randomUUID().toString());
    server =
        InProcessServerBuilder
            .forName(serverName)
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
    replyWithAckResponse()
        .when(serviceAddressService)
        .assignServiceAddressToLawFirm(any(AssignServiceAddressToLawFirmRequest.class), any(StreamObserver.class));

    replyWithAckResponse()
        .when(datastoreSg3Service)
        .upsertThinLawFirmServiceAddress(any(IpDatastoreSg3.ThinServiceAddress.class), any(StreamObserver.class));

    replyWithAckResponse()
        .when(queueOnPrem)
        .deleteQueueUnit(any(DeleteUnitRequest.class), any(StreamObserver.class));

    replyWith(
        ServiceAddress.newBuilder()
            .setServiceAddressId(1L)
            .setLawFirmId(Int64Value.newBuilder().setValue(1L).build())
            .build()
    )
    .when(serviceAddressService)
    .getServiceAddressById(any(GetServiceAddressByIdRequest.class), any(StreamObserver.class));

    serviceAddressSorter.assignServiceAddress(
        AssignServiceAddressRequest
            .newBuilder()
            .setUnsortedServiceAddressQueueItemId("123")
            .setLawFirmId(1L)
            .setServiceAddressId(1L)
            .build()
    );

    verify(queueOnPrem).deleteQueueUnit(
        eq(DeleteUnitRequest.newBuilder().setDbId("123").build()),
        any(StreamObserver.class)
    );
    verify(serviceAddressService).assignServiceAddressToLawFirm(
        eq(AssignServiceAddressToLawFirmRequest.newBuilder().setLawFirmId(1L).setServiceAddressId(1L).build()),
        any(StreamObserver.class)
    );
  }

  @Test
  public void unsortServiceAddress() throws Exception {
    // TODO
  }

  @Test
  public void setServiceAddressAsNonLawFirm() throws Exception {
    // TODO
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

  private Stubber replyWithAckResponse() {
    return replyWith(AckResponse.getDefaultInstance());
  }
}