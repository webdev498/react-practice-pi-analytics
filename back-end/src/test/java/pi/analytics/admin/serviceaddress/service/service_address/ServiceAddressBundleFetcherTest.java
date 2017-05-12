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
import pi.admin.service_address_sorting.generated.Agent;
import pi.admin.service_address_sorting.generated.NonLawFirm;
import pi.admin.service_address_sorting.generated.ServiceAddressBundle;
import pi.analytics.admin.serviceaddress.service.QueuedServiceAddress;
import pi.ip.data.relational.generated.GetLawFirmByIdRequest;
import pi.ip.data.relational.generated.GetLawFirmByIdResponse;
import pi.ip.data.relational.generated.GetServiceAddressByIdRequest;
import pi.ip.data.relational.generated.LawFirmDbServiceGrpc;
import pi.ip.data.relational.generated.ServiceAddressServiceGrpc;
import pi.ip.generated.datastore_sg3.DatastoreSg3ServiceGrpc;
import pi.ip.generated.datastore_sg3.IpDatastoreSg3.SuggestSimilarThinServiceAddressRequest;
import pi.ip.generated.datastore_sg3.IpDatastoreSg3.SuggestSimilarThinServiceAddressResponse;
import pi.ip.generated.datastore_sg3.IpDatastoreSg3.ThinLawFirm;
import pi.ip.generated.datastore_sg3.IpDatastoreSg3.ThinLawFirmServiceAddress;
import pi.ip.generated.datastore_sg3.IpDatastoreSg3.ThinServiceAddress;
import pi.ip.proto.generated.LangType;
import pi.ip.proto.generated.LawFirm;
import pi.ip.proto.generated.ServiceAddress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static pi.analytics.admin.serviceaddress.service.helpers.GrpcTestHelper.replyWith;
import static pi.analytics.admin.serviceaddress.service.helpers.GrpcTestHelper.replyWithError;
import static pi.analytics.admin.serviceaddress.service.helpers.LawFirmTestHelper.createLawFirm;
import static pi.analytics.admin.serviceaddress.service.helpers.ServiceAddressTestHelper.createServiceAddressForNonLawFirm;
import static pi.analytics.admin.serviceaddress.service.helpers.ServiceAddressTestHelper.createServiceAddressToMatchLawFirm;
import static pi.analytics.admin.serviceaddress.service.helpers.ServiceAddressTestHelper.createUnsortedServiceAddress;

/**
 * @author shane.xie@practiceinsight.io
 */
public class ServiceAddressBundleFetcherTest {

  private Faker faker = new Faker();
  private LawFirmDbServiceGrpc.LawFirmDbServiceImplBase lawFirmDbService;
  private ServiceAddressServiceGrpc.ServiceAddressServiceImplBase serviceAddressService;
  private DatastoreSg3ServiceGrpc.DatastoreSg3ServiceImplBase datastoreSg3Service;
  private Translator translator;
  private Server server;
  private ManagedChannel channel;
  private ServiceAddressBundleFetcher serviceAddressBundleFetcher;

  @Before
  public void setUp() throws Exception {
    lawFirmDbService = mock(LawFirmDbServiceGrpc.LawFirmDbServiceImplBase.class);
    serviceAddressService = mock(ServiceAddressServiceGrpc.ServiceAddressServiceImplBase.class);
    datastoreSg3Service = mock(DatastoreSg3ServiceGrpc.DatastoreSg3ServiceImplBase.class);
    translator = mock(Translator.class);

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
        bind(Translator.class)
            .toInstance(translator);
      }
    }).getInstance(ServiceAddressBundleFetcher.class);
  }

  @After
  public void tearDown() throws Exception {
    channel.shutdown();
    server.shutdownNow();
  }

  @Test
  public void fetch_fails_no_service_address() throws Exception {
    final QueuedServiceAddress queuedServiceAddress = QueuedServiceAddress
        .create(faker.numerify("#####"), Optional.empty());
    assertThatThrownBy(() -> { serviceAddressBundleFetcher.fetch(queuedServiceAddress); })
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
  public void addTranslationIfNecessary_skip_no_source_language() throws Exception {
    final ServiceAddressBundle bundle =
        ServiceAddressBundle
            .newBuilder()
            .setServiceAddressToSort(ServiceAddress.getDefaultInstance())
            .build();
    assertThat(bundle)
        .isEqualTo(serviceAddressBundleFetcher.addTranslationIfNecessary.apply(bundle))
        .as("Bundle passed through unchanged since translation was skipped");
  }

  @Test
  public void addTranslationIfNecessary_skip_western_script() throws Exception {
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
        .isEqualTo(serviceAddressBundleFetcher.addTranslationIfNecessary.apply(bundle))
        .as("Bundle passed through unchanged since translation was skipped");
  }

  @Test
  public void addTranslationIfNecessary_skip_translation_error() throws Exception {
    final ServiceAddressBundle bundle =
        ServiceAddressBundle
            .newBuilder()
            .setServiceAddressToSort(
                ServiceAddress
                    .newBuilder()
                    .setLanguageType(LangType.CYRILLIC)
                    .build()
            )
            .build();
    when(translator.toEn(anyString(), any(LangType.class)))
        .thenThrow(new RuntimeException());
    assertThat(bundle)
        .isEqualTo(serviceAddressBundleFetcher.addTranslationIfNecessary.apply(bundle))
        .as("Bundle passed through unchanged since translation service failed");
  }

  @Test
  public void addTranslationIfNecessary_translated() throws Exception {
    final String sourceText = "source-text";
    final String translatedText = "translated-text";

    final ServiceAddress serviceAddress =
        ServiceAddress
            .newBuilder()
            .setLanguageType(LangType.CHINESE)
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

    when(translator.toEn(eq(sourceText), eq(LangType.CHINESE)))
        .thenReturn(translatedText);

    assertThat(translatedBundle)
        .isEqualTo(serviceAddressBundleFetcher.addTranslationIfNecessary.apply(sourceBundle))
        .as("Translation was added to bundle since translation was required");
  }

  @Test
  public void addAgentSuggestions_no_suggestions_found() throws Exception {
    replyWith(SuggestSimilarThinServiceAddressResponse.getDefaultInstance())
        .when(datastoreSg3Service)
        .suggestSimilarThinServiceAddress(any(SuggestSimilarThinServiceAddressRequest.class), any(StreamObserver.class));

    final ServiceAddressBundle originalBundle =
        ServiceAddressBundle
            .newBuilder()
            .setUnsortedServiceAddressQueueItemId(faker.numerify("#####"))
            .setServiceAddressToSort(createServiceAddressForNonLawFirm(faker.company().name()))
            .build();

    assertThat(originalBundle)
        .isEqualTo(serviceAddressBundleFetcher.addAgentSuggestions.apply(originalBundle))
        .as("Bundle is unchanged since no suggestions were found");
  }

  @Test
  public void addAgentSuggestions_suggestions_found() throws Exception {

    // Set up a law firm suggestion with two service addresses
    final LawFirm lawFirm1 = createLawFirm();
    setupGetLawFirmByIdAnswer(lawFirm1);
    final ServiceAddress lawFirm1ServiceAddress1 = createServiceAddressToMatchLawFirm(lawFirm1);
    final ServiceAddress lawFirm1ServiceAddress2 = createServiceAddressToMatchLawFirm(lawFirm1);
    setupGetServiceAddressByIdAnswer(lawFirm1ServiceAddress1);
    setupGetServiceAddressByIdAnswer(lawFirm1ServiceAddress2);
    final ThinLawFirmServiceAddress lawFirm1ThinAddress1 =
        createThinLawFirmServiceAddressMatchingServiceAddress(lawFirm1ServiceAddress1);
    final ThinLawFirmServiceAddress lawFirm1ThinAddress2 =
        createThinLawFirmServiceAddressMatchingServiceAddress(lawFirm1ServiceAddress2);

    // Set up a service address that is unsorted and hence skipped
    final ServiceAddress unsortedServiceAddress = createUnsortedServiceAddress(faker.company().name());
    setupGetServiceAddressByIdAnswer(unsortedServiceAddress);
    final ThinLawFirmServiceAddress unsortedThinAddress =
        createThinLawFirmServiceAddressMatchingServiceAddress(unsortedServiceAddress);

    // Set up a couple of non-law firm suggestion
    final ServiceAddress nonLawFirmServiceAddress1 = createServiceAddressForNonLawFirm(faker.company().name());
    setupGetServiceAddressByIdAnswer(nonLawFirmServiceAddress1);
    final ThinLawFirmServiceAddress nonLawFirmThinAddress1 =
        createThinLawFirmServiceAddressMatchingServiceAddress(nonLawFirmServiceAddress1);

    final ServiceAddress nonLawFirmServiceAddress2 = createServiceAddressForNonLawFirm(faker.company().name());
    setupGetServiceAddressByIdAnswer(nonLawFirmServiceAddress2);
    final ThinLawFirmServiceAddress nonLawFirmThinAddress2 =
        createThinLawFirmServiceAddressMatchingServiceAddress(nonLawFirmServiceAddress2);

    // Set up a service address that is not found and hence skipped
    final ServiceAddress notFoundServiceAddress = createServiceAddressForNonLawFirm(faker.company().name());
    final ThinLawFirmServiceAddress notFoundThinAddress =
        createThinLawFirmServiceAddressMatchingServiceAddress(notFoundServiceAddress);
    setupGetServiceAddressByIdNotFoundAnswer(notFoundServiceAddress.getServiceAddressId());

    // Set up a law firm suggestion with one service address
    final LawFirm lawFirm2 = createLawFirm();
    setupGetLawFirmByIdAnswer(lawFirm2);
    final ServiceAddress lawFirm2ServiceAddress = createServiceAddressToMatchLawFirm(lawFirm2);
    setupGetServiceAddressByIdAnswer(lawFirm2ServiceAddress);
    final ThinLawFirmServiceAddress lawFirm2ThinAddress =
        createThinLawFirmServiceAddressMatchingServiceAddress(lawFirm2ServiceAddress);

    // Prepare suggestions
    final SuggestSimilarThinServiceAddressResponse suggestSimilarThinServiceAddressResponse =
        SuggestSimilarThinServiceAddressResponse
            .newBuilder()
            .addSuggestions(lawFirm1ThinAddress1)
            .addSuggestions(unsortedThinAddress)
            .addSuggestions(lawFirm1ThinAddress2)
            .addSuggestions(nonLawFirmThinAddress1)
            .addSuggestions(nonLawFirmThinAddress2)
            .addSuggestions(notFoundThinAddress)
            .addSuggestions(lawFirm2ThinAddress)
            .build();

    replyWith(suggestSimilarThinServiceAddressResponse)
        .when(datastoreSg3Service)
        .suggestSimilarThinServiceAddress(any(SuggestSimilarThinServiceAddressRequest.class), any(StreamObserver.class));

    final ServiceAddressBundle originalBundle = ServiceAddressBundle.getDefaultInstance();
    final ServiceAddressBundle bundleWithSuggestions = serviceAddressBundleFetcher.addAgentSuggestions.apply(originalBundle);

    assertThat(bundleWithSuggestions.getSuggestedAgentsList())
        .containsExactly(
            Agent
                .newBuilder()
                .setLawFirm(lawFirm1)
                .addServiceAddresses(lawFirm1ServiceAddress1)
                .addServiceAddresses(lawFirm1ServiceAddress2)
                .build(),
            Agent
                .newBuilder()
                .setNonLawFirm(NonLawFirm.newBuilder().setName(nonLawFirmServiceAddress1.getName()))
                .addServiceAddresses(nonLawFirmServiceAddress1)
                .build(),
            Agent
                .newBuilder()
                .setNonLawFirm(NonLawFirm.newBuilder().setName(nonLawFirmServiceAddress2.getName()))
                .addServiceAddresses(nonLawFirmServiceAddress2)
                .build(),
            Agent
                .newBuilder()
                .setLawFirm(lawFirm2)
                .addServiceAddresses(lawFirm2ServiceAddress)
                .build()
        );
  }


  // Test Helpers

  private ThinLawFirmServiceAddress createThinLawFirmServiceAddressMatchingServiceAddress(
      final ServiceAddress serviceAddress) {
    ThinLawFirmServiceAddress.Builder builder =
        ThinLawFirmServiceAddress
            .newBuilder()
            .setThinServiceAddress(
                ThinServiceAddress.
                    newBuilder()
                    .setServiceAddressId(serviceAddress.getServiceAddressId())
                    .setNameAddress(serviceAddress.getName() + " " + serviceAddress.getAddress())
                .setCountry(serviceAddress.getCountry())
                .setLongitude(serviceAddress.getLongitude())
                .setLatitude(serviceAddress.getLatitude())
            );
    if (serviceAddress.hasLawFirmId()) {
      builder
          .setNotALawFirm(false)
          .setThinLawFirm(
              ThinLawFirm.
                  newBuilder()
                  .setId(serviceAddress.getLawFirmId().getValue())
                  .setName(serviceAddress.getName())
          );
    } else {
      builder.setNotALawFirm(true);
    }
    return builder.build();
  }

  private void setupGetLawFirmByIdAnswer(final LawFirm lawFirm) {
    replyWith(GetLawFirmByIdResponse.newBuilder().setLawFirm(lawFirm).build())
        .when(lawFirmDbService)
        .getLawFirmById(eq(GetLawFirmByIdRequest.newBuilder().setLawFirmId(lawFirm.getLawFirmId()).build()),
            any(StreamObserver.class));
  }

  private void setupGetServiceAddressByIdAnswer(final ServiceAddress serviceAddress) {
    replyWith(serviceAddress)
        .when(serviceAddressService)
        .getServiceAddressById(
            eq(GetServiceAddressByIdRequest.newBuilder().setServiceAddressId(serviceAddress.getServiceAddressId()).build()),
            any(StreamObserver.class));
  }

  private void setupGetServiceAddressByIdNotFoundAnswer(final long serviceAddressId) {
    replyWithError(Status.NOT_FOUND.asRuntimeException())
        .when(serviceAddressService)
        .getServiceAddressById(
            eq(GetServiceAddressByIdRequest.newBuilder().setServiceAddressId(serviceAddressId).build()),
            any(StreamObserver.class));
  }
}