/*
 * Copyright (c) 2017 Practice Insight Pty Ltd.
 */

package pi.analytics.admin.serviceaddress.service.service_address;

import com.google.common.collect.ImmutableSet;
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
import pi.analytics.admin.serviceaddress.service.user.UserService;
import pi.ip.data.relational.generated.GetNextServiceAddressForSortingRequest;
import pi.ip.data.relational.generated.GetNextServiceAddressFromPlaylistRequest;
import pi.ip.data.relational.generated.ServiceAddressServiceGrpc;
import pi.ip.proto.generated.LangType;
import pi.ip.proto.generated.ServiceAddress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static pi.analytics.admin.serviceaddress.service.helpers.GrpcTestHelper.replyWith;
import static pi.analytics.admin.serviceaddress.service.helpers.GrpcTestHelper.replyWithError;

/**
 * @author shane.xie@practiceinsight.io
 */
public class SortableServiceAddressFetcherTest {

  private Faker faker = new Faker();
  private UserService userService;
  private ServiceAddressServiceGrpc.ServiceAddressServiceImplBase serviceAddressService;
  private Server server;
  private ManagedChannel channel;
  private SortableServiceAddressFetcher sortableServiceAddressFetcher;

  @Before
  public void setup() throws Exception {
    userService = mock(UserService.class);
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
        bind(UserService.class).toInstance(userService);
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
  public void fetchNext_for_user_not_found() throws Exception {
    when(userService.hasPlaylist(anyString())).thenReturn(false);
    replyWithError(Status.NOT_FOUND.asRuntimeException())
        .when(serviceAddressService)
        .getNextServiceAddressForSorting(any(GetNextServiceAddressForSortingRequest.class), any(StreamObserver.class));

    final Optional<ServiceAddress> queuedServiceAddress = sortableServiceAddressFetcher.fetchNext(faker.name().username());

    assertThat(queuedServiceAddress)
        .as("No service address found. We should get an empty Optional back")
        .isEmpty();
  }

  @Test
  public void fetchNext_from_playlist_not_found() throws Exception {
    when(userService.hasPlaylist(anyString())).thenReturn(true);
    when(userService.getPlaylist(anyString())).thenReturn(Optional.of("test-playlist"));
    replyWithError(Status.NOT_FOUND.asRuntimeException())
        .when(serviceAddressService)
        .getNextServiceAddressFromPlaylist(any(GetNextServiceAddressFromPlaylistRequest.class), any(StreamObserver.class));

    final Optional<ServiceAddress> queuedServiceAddress = sortableServiceAddressFetcher.fetchNext(faker.name().username());

    assertThat(queuedServiceAddress)
        .as("No service address found. We should get an empty Optional back")
        .isEmpty();
  }

  @Test
  public void fetchNext_for_user_found() throws Exception {
    when(userService.hasPlaylist(anyString())).thenReturn(false);
    when(userService.getAlreadySortedWeightedChance(anyString())).thenReturn(1f);
    when(userService.getLangTypes(anyString())).thenReturn(ImmutableSet.of(LangType.WESTERN_SCRIPT));

    replyWith(ServiceAddress.getDefaultInstance())
        .when(serviceAddressService)
        .getNextServiceAddressForSorting(any(GetNextServiceAddressForSortingRequest.class), any(StreamObserver.class));

    final ArgumentCaptor<GetNextServiceAddressForSortingRequest> argument =
        ArgumentCaptor.forClass(GetNextServiceAddressForSortingRequest.class);
    final Optional<ServiceAddress> serviceAddress = sortableServiceAddressFetcher.fetchNext(faker.name().username());

    assertThat(serviceAddress)
        .as("Service address found. We should get a non-empty Optional back")
        .isPresent();

    verify(serviceAddressService).getNextServiceAddressForSorting(argument.capture(), any(StreamObserver.class));
    assertThat(argument.getValue().getAlreadySortedWeightedChance())
        .as("The already sorted weighted chance should be what the user service gave us")
        .isEqualTo(1f);
    assertThat(argument.getValue().getRestrictToLangTypesList())
        .as("The language types should be what the user service gave us")
        .containsExactly(LangType.WESTERN_SCRIPT);
    assertThat(argument.getValue().getRestrictToOfficeCodesList())
        .as("The list of countries to sort should never be empty")
        .isNotEmpty();
  }

  @Test
  public void fetchNext_from_playlist_found() throws Exception {
    final String username = faker.name().username();
    final String playlist = faker.gameOfThrones().house();

    when(userService.hasPlaylist(anyString())).thenReturn(true);
    when(userService.getPlaylist(anyString())).thenReturn(Optional.of(playlist));

    replyWith(ServiceAddress.getDefaultInstance())
        .when(serviceAddressService)
        .getNextServiceAddressFromPlaylist(any(GetNextServiceAddressFromPlaylistRequest.class), any(StreamObserver.class));

    final ArgumentCaptor<GetNextServiceAddressFromPlaylistRequest> argument =
        ArgumentCaptor.forClass(GetNextServiceAddressFromPlaylistRequest.class);
    final Optional<ServiceAddress> serviceAddress = sortableServiceAddressFetcher.fetchNext(username);

    assertThat(serviceAddress)
        .as("Service address found. We should get a non-empty Optional back")
        .isPresent();

    verify(serviceAddressService).getNextServiceAddressFromPlaylist(argument.capture(), any(StreamObserver.class));
    assertThat(argument.getValue().getPlaylist())
        .as("The playlist should be what the user service gave us")
        .isEqualTo(playlist);
    assertThat(argument.getValue().getUsername())
        .as("The playlist should be what the user service gave us")
        .isEqualTo(username);
  }
}