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
import pi.ip.data.relational.generated.GetNextUnsortedServiceAddressRequest;
import pi.ip.data.relational.generated.ServiceAddressServiceGrpc;
import pi.ip.generated.queue.QueueOnPremGrpc;
import pi.ip.proto.generated.ServiceAddress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static pi.analytics.admin.serviceaddress.service.helpers.GrpcTestHelper.replyWith;
import static pi.analytics.admin.serviceaddress.service.helpers.GrpcTestHelper.replyWithError;

/**
 * @author shane.xie@practiceinsight.io
 */
public class UnsortedServiceAddressFetcherTest {

  private Faker faker = new Faker();
  private ServiceAddressServiceGrpc.ServiceAddressServiceImplBase serviceAddressService;
  private Server server;
  private ManagedChannel channel;
  private UnsortedServiceAddressFetcher unsortedServiceAddressFetcher;

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
  public void fetchNext_not_found() throws Exception {
    replyWithError(Status.NOT_FOUND.asRuntimeException())
        .when(serviceAddressService)
        .getNextUnsortedServiceAddress(any(GetNextUnsortedServiceAddressRequest.class), any(StreamObserver.class));

    final Optional<ServiceAddress> queuedServiceAddress = unsortedServiceAddressFetcher.fetchNext(faker.name().username());

    assertThat(queuedServiceAddress)
        .as("No unsorted service address found. We should get an empty Optional back")
        .isEmpty();
  }

  @Test
  public void fetchNext_found() throws Exception {
    replyWith(ServiceAddress.newBuilder().build())
        .when(serviceAddressService)
        .getNextUnsortedServiceAddress(any(GetNextUnsortedServiceAddressRequest.class), any(StreamObserver.class));

    final Optional<ServiceAddress> queuedServiceAddress = unsortedServiceAddressFetcher.fetchNext(faker.name().username());

    assertThat(queuedServiceAddress)
        .as("Service address found. We should get a non-empty Optional back")
        .isPresent();
  }
}