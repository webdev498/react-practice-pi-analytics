/*
 * Copyright (c) 2017 Practice Insight Pty Ltd.
 */

package pi.analytics.admin.serviceaddress.service.unsorted_service_address;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;

import com.github.javafaker.Faker;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;
import java.util.UUID;

import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import pi.analytics.admin.serviceaddress.service.QueuedServiceAddress;
import pi.ip.data.relational.generated.GetServiceAddressByIdRequest;
import pi.ip.data.relational.generated.ServiceAddressServiceGrpc;
import pi.ip.generated.queue.DeleteUnitRequest;
import pi.ip.generated.queue.MsgUnit;
import pi.ip.generated.queue.QueueOnPremGrpc;
import pi.ip.generated.queue.StoredMsgUnit;
import pi.ip.generated.queue.UnitRequestOnPrem;
import pi.ip.proto.generated.AckResponse;
import pi.ip.proto.generated.ServiceAddress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author shane.xie@practiceinsight.io
 */
public class UnsortedServiceAddressFetcherTest {

  private Faker faker = new Faker();
  private QueueOnPremGrpc.QueueOnPremImplBase queueOnPrem;
  private ServiceAddressServiceGrpc.ServiceAddressServiceImplBase serviceAddressService;
  private Server server;
  private ManagedChannel channel;
  private UnsortedServiceAddressFetcher unsortedServiceAddressFetcher;

  @Before
  public void setup() throws Exception {
    queueOnPrem = mock(QueueOnPremGrpc.QueueOnPremImplBase.class);
    serviceAddressService = mock(ServiceAddressServiceGrpc.ServiceAddressServiceImplBase.class);

    final String serverName = "unsorted-service-address-fetcher-test-".concat(UUID.randomUUID().toString());
    server =
        InProcessServerBuilder
            .forName(serverName)
            .addService(queueOnPrem.bindService())
            .addService(serviceAddressService.bindService())
            .directExecutor()
            .build()
            .start();
    channel =
        InProcessChannelBuilder
            .forName(serverName)
            .directExecutor()
            .build();

    unsortedServiceAddressFetcher = Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        bind(QueueOnPremGrpc.QueueOnPremBlockingStub.class)
            .toInstance(QueueOnPremGrpc.newBlockingStub(channel));
        bind(ServiceAddressServiceGrpc.ServiceAddressServiceBlockingStub.class)
            .toInstance(ServiceAddressServiceGrpc.newBlockingStub(channel));
      }
    }).getInstance(UnsortedServiceAddressFetcher.class);
  }

  @After
  public void tearDown() throws Exception {
    channel.shutdown();
    server.shutdownNow();
  }

  @Test
  public void fetchNextQueueItem_empty_queue() throws Exception {
    doAnswer(invocation -> {
      StreamObserver<StoredMsgUnit> responseObserver = (StreamObserver<StoredMsgUnit>) invocation.getArguments()[1];
      responseObserver.onNext(StoredMsgUnit.getDefaultInstance());
      responseObserver.onCompleted();
      return null;
    })
    .when(queueOnPrem)
    .getNextQueueUnit(any(UnitRequestOnPrem.class), any(StreamObserver.class));

    final Optional<QueuedServiceAddress> queuedServiceAddress =
        unsortedServiceAddressFetcher.fetchNext(faker.name().username());
    assertThat(queuedServiceAddress)
        .as("Queues are empty. We should get an empty Optional back")
        .isEmpty();
  }

  @Test
  public void fetchNextQueueItem_queue_fetch_error_results_in_exception() throws Exception {
    doAnswer(invocation -> {
      StreamObserver<StoredMsgUnit> responseObserver = (StreamObserver<StoredMsgUnit>) invocation.getArguments()[1];
      responseObserver.onError(Status.INTERNAL.asRuntimeException());
      return null;
    })
    .when(queueOnPrem)
    .getNextQueueUnit(any(UnitRequestOnPrem.class), any(StreamObserver.class));

    assertThatThrownBy(() -> { unsortedServiceAddressFetcher.fetchNext(faker.name().username()); })
        .as("Queue fetch error results in an exception being thrown");
  }

  @Test
  public void fetchNextQueueItem_service_address_fetch_error_results_in_exception() throws Exception {
    final StoredMsgUnit storedMsgUnit =
        StoredMsgUnit
            .newBuilder()
            .setDbId("111")
            .setMsgUnit(MsgUnit.newBuilder().setUniqueMsgKey("222"))
            .build();

    doAnswer(invocation -> {
      StreamObserver<StoredMsgUnit> responseObserver = (StreamObserver<StoredMsgUnit>) invocation.getArguments()[1];
      responseObserver.onNext(storedMsgUnit);
      responseObserver.onCompleted();
      return null;
    })
    .when(queueOnPrem)
    .getNextQueueUnit(any(UnitRequestOnPrem.class), any(StreamObserver.class));

    doAnswer(invocation -> {
      StreamObserver<ServiceAddress> responseObserver = (StreamObserver<ServiceAddress>) invocation.getArguments()[1];
      responseObserver.onError(new Exception());
      return null;
    })
    .when(serviceAddressService)
    .getServiceAddressById(any(GetServiceAddressByIdRequest.class), any(StreamObserver.class));

    assertThatThrownBy(() -> { unsortedServiceAddressFetcher.fetchNext(faker.name().username()); })
        .as("Service address fetch error results in an exception being thrown");
  }

  @Test
  public void fetchNextQueueItem_service_address_not_found() throws Exception {
    final StoredMsgUnit storedMsgUnit =
        StoredMsgUnit
            .newBuilder()
            .setDbId("111")
            .setMsgUnit(MsgUnit.newBuilder().setUniqueMsgKey("222"))
            .build();

    doAnswer(invocation -> {
      StreamObserver<StoredMsgUnit> responseObserver = (StreamObserver<StoredMsgUnit>) invocation.getArguments()[1];
      responseObserver.onNext(storedMsgUnit);
      responseObserver.onCompleted();
      return null;
    })
    .when(queueOnPrem)
    .getNextQueueUnit(any(UnitRequestOnPrem.class), any(StreamObserver.class));

    doAnswer(invocation -> {
      StreamObserver<AckResponse> responseObserver = (StreamObserver<AckResponse>) invocation.getArguments()[1];
      responseObserver.onNext(AckResponse.getDefaultInstance());
      responseObserver.onCompleted();
      return null;
    })
    .when(queueOnPrem)
    .deleteQueueUnit(any(DeleteUnitRequest.class), any(StreamObserver.class));

    doAnswer(invocation -> {
      StreamObserver<ServiceAddress> responseObserver = (StreamObserver<ServiceAddress>) invocation.getArguments()[1];
      responseObserver.onError(Status.NOT_FOUND.asRuntimeException());
      return null;
    })
    .when(serviceAddressService)
    .getServiceAddressById(any(GetServiceAddressByIdRequest.class), any(StreamObserver.class));

    final Optional<QueuedServiceAddress> queuedServiceAddress =
        unsortedServiceAddressFetcher.fetchNext("hellen");  // User has 5 queues

    assertThat(queuedServiceAddress)
        .as("Queues return an item whose service address cannot be found")
        .isEmpty();

    // Check that we delete the queue item for a service address that we can't find
    final DeleteUnitRequest deleteUnitRequest = DeleteUnitRequest.newBuilder().setDbId(storedMsgUnit.getDbId()).build();
    verify(queueOnPrem, times(5))  // Once per queue since we made all queues return the same item
        .deleteQueueUnit(eq(deleteUnitRequest), any(StreamObserver.class));
  }

  @Test
  public void fetchNextQueueItem_service_address_ignored_office_code() throws Exception {
    final StoredMsgUnit storedMsgUnit =
        StoredMsgUnit
            .newBuilder()
            .setDbId("111")
            .setMsgUnit(MsgUnit.newBuilder().setUniqueMsgKey("222"))
            .build();

    doAnswer(invocation -> {
      StreamObserver<StoredMsgUnit> responseObserver = (StreamObserver<StoredMsgUnit>) invocation.getArguments()[1];
      responseObserver.onNext(storedMsgUnit);
      responseObserver.onCompleted();
      return null;
    })
    .when(queueOnPrem)
    .getNextQueueUnit(any(UnitRequestOnPrem.class), any(StreamObserver.class));

    doAnswer(invocation -> {
      StreamObserver<AckResponse> responseObserver = (StreamObserver<AckResponse>) invocation.getArguments()[1];
      responseObserver.onNext(AckResponse.getDefaultInstance());
      responseObserver.onCompleted();
      return null;
    })
    .when(queueOnPrem)
    .deleteQueueUnit(any(DeleteUnitRequest.class), any(StreamObserver.class));

    final ServiceAddress serviceAddress =
        ServiceAddress
            .newBuilder()
            .setServiceAddressId(222L)
            .setCountry("OFFICE_CODE_OF_NON_INTEREST")
            .build();

    doAnswer(invocation -> {
      StreamObserver<ServiceAddress> responseObserver = (StreamObserver<ServiceAddress>) invocation.getArguments()[1];
      responseObserver.onNext(serviceAddress);
      responseObserver.onCompleted();
      return null;
    })
    .when(serviceAddressService)
    .getServiceAddressById(any(GetServiceAddressByIdRequest.class), any(StreamObserver.class));

    final Optional<QueuedServiceAddress> queuedServiceAddress =
        unsortedServiceAddressFetcher.fetchNext("user.with.4.queues");

    assertThat(queuedServiceAddress)
        .as("Queues return an item whose service address has an office code we are not interested in")
        .isEmpty();

    // Check that we delete the queue item for a service address that we don't care about
    final DeleteUnitRequest deleteUnitRequest = DeleteUnitRequest.newBuilder().setDbId(storedMsgUnit.getDbId()).build();
    verify(queueOnPrem, times(4))  // Once per queue since we made all queues return the same item
        .deleteQueueUnit(eq(deleteUnitRequest), any(StreamObserver.class));
  }

  @Test
  public void fetchNextQueueItem_valid_service_address() throws Exception {
    final StoredMsgUnit storedMsgUnit =
        StoredMsgUnit
            .newBuilder()
            .setDbId("111")
            .setMsgUnit(MsgUnit.newBuilder().setUniqueMsgKey("222"))
            .build();

    doAnswer(invocation -> {
      StreamObserver<StoredMsgUnit> responseObserver = (StreamObserver<StoredMsgUnit>) invocation.getArguments()[1];
      responseObserver.onNext(storedMsgUnit);
      responseObserver.onCompleted();
      return null;
    })
    .when(queueOnPrem)
    .getNextQueueUnit(any(UnitRequestOnPrem.class), any(StreamObserver.class));

    final ServiceAddress serviceAddress =
        ServiceAddress
            .newBuilder()
            .setServiceAddressId(222L)
            .setCountry("AU")
            .build();

    doAnswer(invocation -> {
      StreamObserver<ServiceAddress> responseObserver = (StreamObserver<ServiceAddress>) invocation.getArguments()[1];
      responseObserver.onNext(serviceAddress);
      responseObserver.onCompleted();
      return null;
    })
    .when(serviceAddressService)
    .getServiceAddressById(any(GetServiceAddressByIdRequest.class), any(StreamObserver.class));

    final Optional<QueuedServiceAddress> queuedServiceAddress =
        unsortedServiceAddressFetcher.fetchNext(faker.name().username());
    final QueuedServiceAddress expectedResult = QueuedServiceAddress.create("111", Optional.of(serviceAddress));

    assertThat(queuedServiceAddress)
        .as("Queues returns an item whose service address we manage to fetch")
        .isEqualTo(Optional.of(expectedResult));

    verify(queueOnPrem, never()).deleteQueueUnit(any(DeleteUnitRequest.class), any(StreamObserver.class));
  }
}