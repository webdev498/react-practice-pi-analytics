/*
 * Copyright (c) 2017 Practice Insight Pty Ltd.
 */

package pi.analytics.admin.serviceaddress.service.service_address;

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
import pi.ip.data.relational.generated.IsHighValueServiceAddressRequest;
import pi.ip.data.relational.generated.IsHighValueServiceAddressResponse;
import pi.ip.data.relational.generated.ServiceAddressServiceGrpc;
import pi.ip.generated.queue.DelayUnitRequest;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static pi.analytics.admin.serviceaddress.service.helpers.GrpcTestHelper.replyWith;
import static pi.analytics.admin.serviceaddress.service.helpers.GrpcTestHelper.replyWithError;

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
    replyWith(StoredMsgUnit.getDefaultInstance())
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
    replyWithError(Status.INTERNAL.asRuntimeException())
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

    replyWith(storedMsgUnit)
        .when(queueOnPrem)
        .getNextQueueUnit(any(UnitRequestOnPrem.class), any(StreamObserver.class));

    replyWithError(new Exception())
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

    // Queue returns one item
    replyWith(storedMsgUnit)
        // End of the queue
        .replyWith(StoredMsgUnit.getDefaultInstance())
        .when(queueOnPrem)
        .getNextQueueUnit(any(UnitRequestOnPrem.class), any(StreamObserver.class));

    replyWith(AckResponse.getDefaultInstance())
        .when(queueOnPrem)
        .deleteQueueUnit(any(DeleteUnitRequest.class), any(StreamObserver.class));

    replyWithError(Status.NOT_FOUND.asRuntimeException())
        .when(serviceAddressService)
        .getServiceAddressById(any(GetServiceAddressByIdRequest.class), any(StreamObserver.class));

    final Optional<QueuedServiceAddress> queuedServiceAddress =
        unsortedServiceAddressFetcher.fetchNext("hellen");  // User has 5 queues

    assertThat(queuedServiceAddress)
        .as("Queues return an item whose service address cannot be found")
        .isEmpty();

    // Check that we delete the queue item for a service address that we can't find
    final DeleteUnitRequest deleteUnitRequest = DeleteUnitRequest.newBuilder().setDbId(storedMsgUnit.getDbId()).build();
    verify(queueOnPrem, times(1))
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

    // Queue returns one item
    replyWith(storedMsgUnit)
        // End of the queue
        .replyWith(StoredMsgUnit.getDefaultInstance())
        .when(queueOnPrem)
        .getNextQueueUnit(any(UnitRequestOnPrem.class), any(StreamObserver.class));

    replyWith(AckResponse.getDefaultInstance())
        .when(queueOnPrem)
        .deleteQueueUnit(any(DeleteUnitRequest.class), any(StreamObserver.class));

    final ServiceAddress serviceAddress =
        ServiceAddress
            .newBuilder()
            .setServiceAddressId(222L)
            .setCountry("OFFICE_CODE_OF_NON_INTEREST")
            .build();

    replyWith(serviceAddress)
        .when(serviceAddressService)
        .getServiceAddressById(any(GetServiceAddressByIdRequest.class), any(StreamObserver.class));

    final Optional<QueuedServiceAddress> queuedServiceAddress =
        unsortedServiceAddressFetcher.fetchNext("user.with.4.queues");

    assertThat(queuedServiceAddress)
        .as("Queues return an item whose service address has an office code we are not interested in")
        .isEmpty();

    // Check that we delete the queue item for a service address that we don't care about
    final DeleteUnitRequest deleteUnitRequest = DeleteUnitRequest.newBuilder().setDbId(storedMsgUnit.getDbId()).build();
    verify(queueOnPrem, times(1))
        .deleteQueueUnit(eq(deleteUnitRequest), any(StreamObserver.class));
  }

  @Test
  public void fetchNextQueueItem_service_address_already_sorted() throws Exception {
    final StoredMsgUnit storedMsgUnit =
        StoredMsgUnit
            .newBuilder()
            .setDbId("111")
            .setMsgUnit(MsgUnit.newBuilder().setUniqueMsgKey("222"))
            .build();

    // Queue returns one item
    replyWith(storedMsgUnit)
        // End of the queue
        .replyWith(StoredMsgUnit.getDefaultInstance())
        .when(queueOnPrem)
        .getNextQueueUnit(any(UnitRequestOnPrem.class), any(StreamObserver.class));

    replyWith(AckResponse.getDefaultInstance())
        .when(queueOnPrem)
        .deleteQueueUnit(any(DeleteUnitRequest.class), any(StreamObserver.class));

    final ServiceAddress serviceAddress =
        ServiceAddress
            .newBuilder()
            .setServiceAddressId(222L)
            .setLawFirmStatusDetermined(true)
            .setCountry("AU")
            .build();

    replyWith(serviceAddress)
        .when(serviceAddressService)
        .getServiceAddressById(any(GetServiceAddressByIdRequest.class), any(StreamObserver.class));

    final Optional<QueuedServiceAddress> queuedServiceAddress =
        unsortedServiceAddressFetcher.fetchNext("user.with.4.queues");

    assertThat(queuedServiceAddress)
        .as("Queues return an item whose service address has already been sorted")
        .isEmpty();

    // Check that we delete the queue item for a service address that we don't care about
    final DeleteUnitRequest deleteUnitRequest = DeleteUnitRequest.newBuilder().setDbId(storedMsgUnit.getDbId()).build();
    verify(queueOnPrem, times(1))
        .deleteQueueUnit(eq(deleteUnitRequest), any(StreamObserver.class));
  }

  @Test
  public void fetchNextQueueItem_dont_skip_if_cannot_determine_high_value() throws Exception {
    final StoredMsgUnit storedMsgUnit =
        StoredMsgUnit
            .newBuilder()
            .setDbId("111")
            .setMsgUnit(MsgUnit.newBuilder().setUniqueMsgKey("222"))
            .build();

    // Queue returns one item
    replyWith(storedMsgUnit)
        // End of the queue
        .replyWith(StoredMsgUnit.getDefaultInstance())
        .when(queueOnPrem)
        .getNextQueueUnit(any(UnitRequestOnPrem.class), any(StreamObserver.class));

    final ServiceAddress serviceAddress =
        ServiceAddress
            .newBuilder()
            .setServiceAddressId(222L)
            .setCountry("DE")
            .build();

    replyWith(serviceAddress)
        .when(serviceAddressService)
        .getServiceAddressById(any(GetServiceAddressByIdRequest.class), any(StreamObserver.class));

    replyWith(AckResponse.getDefaultInstance())
        .when(queueOnPrem)
        .delayQueueUnit(any(DelayUnitRequest.class), any(StreamObserver.class));

    replyWithError(Status.NOT_FOUND.asRuntimeException())
        .when(serviceAddressService)
        .isHighValueServiceAddress(any(IsHighValueServiceAddressRequest.class), any(StreamObserver.class));

    final Optional<QueuedServiceAddress> queuedServiceAddress =
        unsortedServiceAddressFetcher.fetchNext(faker.name().username());
    final QueuedServiceAddress expectedResult = QueuedServiceAddress.create("111", Optional.of(serviceAddress));

    assertThat(queuedServiceAddress)
        .as("Queues return an item for which we can't determine whether the service address is of high value")
        .isEqualTo(Optional.of(expectedResult));

    final DelayUnitRequest delayUnitRequest =
        DelayUnitRequest
            .newBuilder()
            .setDbId(storedMsgUnit.getDbId())
            .setDelaySeconds(14400)
            .build();

    verify(queueOnPrem, never()).delayQueueUnit(eq(delayUnitRequest), any(StreamObserver.class));
    verify(queueOnPrem, never()).deleteQueueUnit(any(DeleteUnitRequest.class), any(StreamObserver.class));
  }

  @Test
  public void fetchNextQueueItem_skip_taiwanese_service_address() throws Exception {
    final StoredMsgUnit storedMsgUnit =
        StoredMsgUnit
            .newBuilder()
            .setDbId("111")
            .setMsgUnit(MsgUnit.newBuilder().setUniqueMsgKey("222"))
            .build();

    // Queue returns one item
    replyWith(storedMsgUnit)
        // End of the queue
        .replyWith(StoredMsgUnit.getDefaultInstance())
        .when(queueOnPrem)
        .getNextQueueUnit(any(UnitRequestOnPrem.class), any(StreamObserver.class));

    final ServiceAddress serviceAddress =
        ServiceAddress
            .newBuilder()
            .setServiceAddressId(222L)
            .setCountry("TW")
            .build();

    replyWith(serviceAddress)
        .when(serviceAddressService)
        .getServiceAddressById(any(GetServiceAddressByIdRequest.class), any(StreamObserver.class));

    replyWith(AckResponse.getDefaultInstance())
        .when(queueOnPrem)
        .delayQueueUnit(any(DelayUnitRequest.class), any(StreamObserver.class));

    replyWith(IsHighValueServiceAddressResponse.newBuilder().setIsHighValue(true).build())
        .when(serviceAddressService)
        .isHighValueServiceAddress(any(IsHighValueServiceAddressRequest.class), any(StreamObserver.class));

    final Optional<QueuedServiceAddress> queuedServiceAddress =
        unsortedServiceAddressFetcher.fetchNext(faker.name().username());

    assertThat(queuedServiceAddress)
        .as("Queues return an item whose service address is from Taiwan, and thus skipped")
        .isEmpty();

    final DelayUnitRequest delayUnitRequest =
        DelayUnitRequest
            .newBuilder()
            .setDbId(storedMsgUnit.getDbId())
            .setDelaySeconds(14400)
            .build();

    verify(queueOnPrem, times(1)).delayQueueUnit(eq(delayUnitRequest), any(StreamObserver.class));
    verify(queueOnPrem, never()).deleteQueueUnit(any(DeleteUnitRequest.class), any(StreamObserver.class));
  }

  @Test
  public void fetchNextQueueItem_skip_low_value_service_address() throws Exception {
    final StoredMsgUnit storedMsgUnit =
        StoredMsgUnit
            .newBuilder()
            .setDbId("111")
            .setMsgUnit(MsgUnit.newBuilder().setUniqueMsgKey("222"))
            .build();

    // Queue returns one item
    replyWith(storedMsgUnit)
        // End of the queue
        .replyWith(StoredMsgUnit.getDefaultInstance())
        .when(queueOnPrem)
        .getNextQueueUnit(any(UnitRequestOnPrem.class), any(StreamObserver.class));

    final ServiceAddress serviceAddress =
        ServiceAddress
            .newBuilder()
            .setServiceAddressId(222L)
            .setCountry("AU")
            .build();

    replyWith(serviceAddress)
        .when(serviceAddressService)
        .getServiceAddressById(any(GetServiceAddressByIdRequest.class), any(StreamObserver.class));

    replyWith(AckResponse.getDefaultInstance())
        .when(queueOnPrem)
        .delayQueueUnit(any(DelayUnitRequest.class), any(StreamObserver.class));

    replyWith(IsHighValueServiceAddressResponse.newBuilder().setIsHighValue(false).build())
        .when(serviceAddressService)
        .isHighValueServiceAddress(any(IsHighValueServiceAddressRequest.class), any(StreamObserver.class));

    final Optional<QueuedServiceAddress> queuedServiceAddress =
        unsortedServiceAddressFetcher.fetchNext(faker.name().username());

    assertThat(queuedServiceAddress)
        .as("Queues return an item whose service address is of low priority, and thus skipped")
        .isEmpty();

    final DelayUnitRequest delayUnitRequest =
        DelayUnitRequest
            .newBuilder()
            .setDbId(storedMsgUnit.getDbId())
            .setDelaySeconds(14400)
            .build();

    verify(queueOnPrem, times(1)).delayQueueUnit(eq(delayUnitRequest), any(StreamObserver.class));
    verify(queueOnPrem, never()).deleteQueueUnit(any(DeleteUnitRequest.class), any(StreamObserver.class));
  }

  @Test
  public void fetchNextQueueItem_valid_service_address() throws Exception {
    final StoredMsgUnit storedMsgUnit =
        StoredMsgUnit
            .newBuilder()
            .setDbId("111")
            .setMsgUnit(MsgUnit.newBuilder().setUniqueMsgKey("222"))
            .build();

    // Queue returns one item
    replyWith(storedMsgUnit)
        // End of the queue
        .replyWith(StoredMsgUnit.getDefaultInstance())
        .when(queueOnPrem)
        .getNextQueueUnit(any(UnitRequestOnPrem.class), any(StreamObserver.class));

    replyWith(IsHighValueServiceAddressResponse.newBuilder().setIsHighValue(true).build())
        .when(serviceAddressService)
        .isHighValueServiceAddress(any(IsHighValueServiceAddressRequest.class), any(StreamObserver.class));

    final ServiceAddress serviceAddress =
        ServiceAddress
            .newBuilder()
            .setServiceAddressId(222L)
            .setCountry("AU")
            .build();

    replyWith(serviceAddress)
        .when(serviceAddressService)
        .getServiceAddressById(any(GetServiceAddressByIdRequest.class), any(StreamObserver.class));

    final Optional<QueuedServiceAddress> queuedServiceAddress =
        unsortedServiceAddressFetcher.fetchNext(faker.name().username());
    final QueuedServiceAddress expectedResult = QueuedServiceAddress.create("111", Optional.of(serviceAddress));

    assertThat(queuedServiceAddress)
        .as("Queues returns an item whose service address we manage to fetch")
        .isEqualTo(Optional.of(expectedResult));

    // Verify not skipped
    verify(queueOnPrem, never()).delayQueueUnit(any(DelayUnitRequest.class), any(StreamObserver.class));
    // Verify valid and handled
    verify(queueOnPrem, never()).deleteQueueUnit(any(DeleteUnitRequest.class), any(StreamObserver.class));
  }

  @Test
  public void fetchNextQueueItem_fetch_only_sg_service_address_skipped() throws Exception {
    final StoredMsgUnit storedMsgUnit =
        StoredMsgUnit
            .newBuilder()
            .setDbId("111")
            .setMsgUnit(MsgUnit.newBuilder().setUniqueMsgKey("222"))
            .build();

    // Queue returns one item
    replyWith(storedMsgUnit)
        // End of the queue
        .replyWith(StoredMsgUnit.getDefaultInstance())
        .when(queueOnPrem)
        .getNextQueueUnit(any(UnitRequestOnPrem.class), any(StreamObserver.class));

    replyWith(IsHighValueServiceAddressResponse.newBuilder().setIsHighValue(true).build())
        .when(serviceAddressService)
        .isHighValueServiceAddress(any(IsHighValueServiceAddressRequest.class), any(StreamObserver.class));

    final ServiceAddress serviceAddress =
        ServiceAddress
            .newBuilder()
            .setServiceAddressId(222L)
            .setCountry("AU")
            .build();

    replyWith(serviceAddress)
        .when(serviceAddressService)
        .getServiceAddressById(any(GetServiceAddressByIdRequest.class), any(StreamObserver.class));

    final Optional<QueuedServiceAddress> queuedServiceAddress =
        unsortedServiceAddressFetcher.fetchNext("sgonly");

    assertThat(queuedServiceAddress)
        .as("Queues returns an item whose service address is not from SG")
        .isEmpty();

    // Verify not skipped
    verify(queueOnPrem, never()).delayQueueUnit(any(DelayUnitRequest.class), any(StreamObserver.class));
    // Verify valid and handled
    verify(queueOnPrem, never()).deleteQueueUnit(any(DeleteUnitRequest.class), any(StreamObserver.class));
  }

  @Test
  public void fetchNextQueueItem_fetch_only_sg_service_address() throws Exception {
    final StoredMsgUnit storedMsgUnit =
        StoredMsgUnit
            .newBuilder()
            .setDbId("111")
            .setMsgUnit(MsgUnit.newBuilder().setUniqueMsgKey("222"))
            .build();

    // Queue returns one item
    replyWith(storedMsgUnit)
        // End of the queue
        .replyWith(StoredMsgUnit.getDefaultInstance())
        .when(queueOnPrem)
        .getNextQueueUnit(any(UnitRequestOnPrem.class), any(StreamObserver.class));

    replyWith(IsHighValueServiceAddressResponse.newBuilder().setIsHighValue(true).build())
        .when(serviceAddressService)
        .isHighValueServiceAddress(any(IsHighValueServiceAddressRequest.class), any(StreamObserver.class));

    final ServiceAddress serviceAddress =
        ServiceAddress
            .newBuilder()
            .setServiceAddressId(222L)
            .setCountry("SG")
            .build();

    replyWith(serviceAddress)
        .when(serviceAddressService)
        .getServiceAddressById(any(GetServiceAddressByIdRequest.class), any(StreamObserver.class));

    final Optional<QueuedServiceAddress> queuedServiceAddress =
        unsortedServiceAddressFetcher.fetchNext("sgonly");
    final QueuedServiceAddress expectedResult = QueuedServiceAddress.create("111", Optional.of(serviceAddress));

    assertThat(queuedServiceAddress)
        .as("Queues returns an item whose service address is from SG")
        .isEqualTo(Optional.of(expectedResult));

    // Verify not skipped
    verify(queueOnPrem, never()).delayQueueUnit(any(DelayUnitRequest.class), any(StreamObserver.class));
    // Verify valid and handled
    verify(queueOnPrem, never()).deleteQueueUnit(any(DeleteUnitRequest.class), any(StreamObserver.class));
  }
}