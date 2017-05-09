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
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import pi.admin.service_address_sorting.generated.ServiceAddressBundle;
import pi.analytics.admin.serviceaddress.service.QueuedServiceAddress;
import pi.ip.data.relational.generated.LawFirmDbServiceGrpc;
import pi.ip.data.relational.generated.ServiceAddressServiceGrpc;
import pi.ip.generated.datastore_sg3.DatastoreSg3ServiceGrpc;
import pi.ip.proto.generated.LangType;
import pi.ip.proto.generated.ServiceAddress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author shane.xie@practiceinsight.io
 */
public class ServiceAddressBundleFetcherTest {

  private Faker faker = new Faker();
  private LawFirmDbServiceGrpc.LawFirmDbServiceImplBase lawFirmDbService;
  private ServiceAddressServiceGrpc.ServiceAddressServiceImplBase serviceAddressService;
  private DatastoreSg3ServiceGrpc.DatastoreSg3ServiceImplBase datastoreSg3Service;
  private TranslationHelper translationHelper;
  private Server server;
  private ManagedChannel channel;
  private ServiceAddressBundleFetcher serviceAddressBundleFetcher;

  @Before
  public void setUp() throws Exception {
    lawFirmDbService = mock(LawFirmDbServiceGrpc.LawFirmDbServiceImplBase.class);
    serviceAddressService = mock(ServiceAddressServiceGrpc.ServiceAddressServiceImplBase.class);
    datastoreSg3Service = mock(DatastoreSg3ServiceGrpc.DatastoreSg3ServiceImplBase.class);
    translationHelper = mock(TranslationHelper.class);

    final String serverName = "service-address-bundle-fetcher-test-".concat(UUID.randomUUID().toString());
    server =
        InProcessServerBuilder
            .forName(serverName)
            .addService(lawFirmDbService.bindService())
            .addService(serviceAddressService.bindService())
            .addService(datastoreSg3Service.bindService())
            .directExecutor()
            .build()
            .start();
    channel =
        InProcessChannelBuilder
            .forName(serverName)
            .directExecutor()
            .build();

    serviceAddressBundleFetcher = Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        bind(LawFirmDbServiceGrpc.LawFirmDbServiceBlockingStub.class)
            .toInstance(LawFirmDbServiceGrpc.newBlockingStub(channel));
        bind(ServiceAddressServiceGrpc.ServiceAddressServiceBlockingStub.class)
            .toInstance(ServiceAddressServiceGrpc.newBlockingStub(channel));
        bind(DatastoreSg3ServiceGrpc.DatastoreSg3ServiceBlockingStub.class)
            .toInstance(DatastoreSg3ServiceGrpc.newBlockingStub(channel));
        bind(TranslationHelper.class)
            .toInstance(translationHelper);
      }
    }).getInstance(ServiceAddressBundleFetcher.class);
  }

  @After
  public void tearDown() throws Exception {
    channel.shutdown();
    server.shutdownNow();
  }

  @Test
  public void createServiceAddressBundle_fails_no_service_address() throws Exception {
    final QueuedServiceAddress queuedServiceAddress = QueuedServiceAddress
        .create(faker.numerify("#####"), Optional.empty());
    assertThatThrownBy(() -> { serviceAddressBundleFetcher.createServiceAddressBundle.apply(queuedServiceAddress); })
        .isInstanceOf(IllegalArgumentException.class)
        .as("Service address cannot be empty");
  }

  @Test
  public void createServiceAddressBundle() throws Exception {
    final String queueId = faker.numerify("#####");
    final ServiceAddress serviceAddress =
        ServiceAddress
            .newBuilder()
            .setServiceAddressId(faker.number().randomNumber())
            .build();
    final QueuedServiceAddress queuedServiceAddress = QueuedServiceAddress.create(queueId, Optional.of(serviceAddress));
    final ServiceAddressBundle serviceAddressBundle =
        ServiceAddressBundle
            .newBuilder()
            .setUnsortedServiceAddressQueueItemId(queueId)
            .setServiceAddressToSort(serviceAddress)
            .build();
    assertThat(serviceAddressBundle)
        .isEqualTo(serviceAddressBundleFetcher.createServiceAddressBundle.apply(queuedServiceAddress));
  }

  @Test
  public void addTranslationIfNecessary_skipped_no_source_language() throws Exception {
    final ServiceAddressBundle bundle =
        ServiceAddressBundle
            .newBuilder()
            .setServiceAddressToSort(ServiceAddress.getDefaultInstance())
            .build();
    assertThat(bundle)
        .isEqualTo(serviceAddressBundleFetcher.addTranslationIfNecessary.apply(bundle));
  }

  @Test
  public void addTranslationIfNecessary_skipped_western_script() throws Exception {
    final ServiceAddressBundle bundle =
        ServiceAddressBundle
            .newBuilder()
            .setServiceAddressToSort(
                ServiceAddress
                    .newBuilder()
                    .setLanguageType(LangType.WESTERN_SCRIPT)
                    .build()
            )
            .build();
    assertThat(bundle)
        .isEqualTo(serviceAddressBundleFetcher.addTranslationIfNecessary.apply(bundle));
  }

  @Test
  public void addTranslationIfNecessary_translated() throws Exception {
    final String sourceText = "source-text";
    final String translatedText = "translated-text";

    final ServiceAddress serviceAddress =
        ServiceAddress
            .newBuilder()
            .setLanguageType(LangType.JAPANESE)
            .setAddress(sourceText)
            .build();

    final ServiceAddressBundle sourceBundle =
        ServiceAddressBundle
            .newBuilder()
            .setServiceAddressToSort(serviceAddress)
            .build();

    final ServiceAddressBundle translatedBundle =
        ServiceAddressBundle
            .newBuilder(sourceBundle)
            .setEnTranslation(translatedText)
            .build();

    when(translationHelper.toEn(eq(sourceText), eq(LangType.JAPANESE)))
        .thenReturn(translatedText);

    assertThat(translatedBundle)
        .isEqualTo(serviceAddressBundleFetcher.addTranslationIfNecessary.apply(sourceBundle));
  }

  @Test
  public void addAgentSuggestions() throws Exception {
    // TODO(SX)
  }

  @Test
  public void buildAgent() throws Exception {
    // TODO(SX)
  }
}