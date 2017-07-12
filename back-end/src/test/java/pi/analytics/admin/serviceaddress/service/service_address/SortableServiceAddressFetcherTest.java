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
import org.mockito.ArgumentCaptor;

import java.util.Optional;
import java.util.UUID;

import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import pi.ip.data.relational.generated.GetNextServiceAddressForSortingRequest;
import pi.ip.data.relational.generated.ServiceAddressServiceGrpc;
import pi.ip.generated.queue.QueueOnPremGrpc;
import pi.ip.proto.generated.ServiceAddress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static pi.analytics.admin.serviceaddress.service.helpers.GrpcTestHelper.replyWith;
import static pi.analytics.admin.serviceaddress.service.helpers.GrpcTestHelper.replyWithError;

/**
 * @author shane.xie@practiceinsight.io
 */
public class SortableServiceAddressFetcherTest {

  private Faker faker = new Faker();
  private ServiceAddressServiceGrpc.ServiceAddressServiceImplBase serviceAddressService;
  private Server server;
  private ManagedChannel channel;
  private SortableServiceAddressFetcher sortableServiceAddressFetcher;

  @Before
  public void setup() throws Exception {
    serviceAddressService = mock(ServiceAddressServiceGrpc.ServiceAddressServiceImplBase.class);

    final String serverName = "unsorted-service-address-fetcher-test-".concat(UUID.randomUUID().toString());
    server =
        InProcessServerBuilder
            .forName(serverName)
            .addService(serviceAddressService.bindService())
            .directExecutor()
            .build()
            .start();
    channel =
        InProcessChannelBuilder
            .forName(serverName)
            .directExecutor()
            .build();

    sortableServiceAddressFetcher = Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        bind(QueueOnPremGrpc.QueueOnPremBlockingStub.class)
            .toInstance(QueueOnPremGrpc.newBlockingStub(channel));
        bind(ServiceAddressServiceGrpc.ServiceAddressServiceBlockingStub.class)
            .toInstance(ServiceAddressServiceGrpc.newBlockingStub(channel));
      }
    }).getInstance(SortableServiceAddressFetcher.class);
  }

  @After
  public void tearDown() throws Exception {
    channel.shutdown();
    server.shutdownNow();
  }

  @Test
  public void fetchNext_not_found() throws Exception {
    replyWithError(Status.NOT_FOUND.asRuntimeException())
        .when(serviceAddressService)
        .getNextServiceAddressForSorting(any(GetNextServiceAddressForSortingRequest.class), any(StreamObserver.class));

    final Optional<ServiceAddress> queuedServiceAddress = sortableServiceAddressFetcher.fetchNext(faker.name().username());

    assertThat(queuedServiceAddress)
        .as("No unsorted service address found. We should get an empty Optional back")
        .isEmpty();
  }

  @Test
  public void fetchNext_found() throws Exception {
    replyWith(ServiceAddress.getDefaultInstance())
        .when(serviceAddressService)
        .getNextServiceAddressForSorting(any(GetNextServiceAddressForSortingRequest.class), any(StreamObserver.class));

    final ArgumentCaptor<GetNextServiceAddressForSortingRequest> argument =
        ArgumentCaptor.forClass(GetNextServiceAddressForSortingRequest.class);
    final Optional<ServiceAddress> queuedServiceAddress = sortableServiceAddressFetcher.fetchNext(faker.name().username());

    assertThat(queuedServiceAddress)
        .as("Service address found. We should get a non-empty Optional back")
        .isPresent();

    verify(serviceAddressService).getNextServiceAddressForSorting(argument.capture(), any(StreamObserver.class));
    assertThat(argument.getValue().getRestrictToLangTypesList()).isNotEmpty();
    assertThat(argument.getValue().getRestrictToOfficeCodesList()).isNotEmpty();
  }

  @Test
  public void alreadySortedWeightedChanceForUser_staff() throws Exception {
    assertThat(sortableServiceAddressFetcher.alreadySortedWeightedChanceForUser("shane"))
        .isEqualTo(0)
        .as("User shane is a staff member and should always sort unsorted service addresses");
  }

  @Test
  public void alreadySortedWeightedChanceForUser_nonstaff() throws Exception {
    assertThat(sortableServiceAddressFetcher.alreadySortedWeightedChanceForUser("trialuser"))
        .isEqualTo(1)
        .as("User 'trialuser' is not a staff member and should always be given already-sorted service addresses");
  }
}