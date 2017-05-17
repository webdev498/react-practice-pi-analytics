package pi.analytics.admin.serviceaddress.guice;

import com.google.inject.Provider;
import com.google.inject.Singleton;

import com.pi.common.config.PiConfig;
import com.pi.common.config.PiKubeServiceImpl;
import com.pi.common.config.PiKubeServicePort;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.practiceinsight.licensingalert.citationsearch.generated.NakedLawFirmSearchServiceGrpc;
import io.practiceinsight.licensingalert.citationsearch.generated.NakedLawFirmSearchServiceGrpc
    .NakedLawFirmSearchServiceBlockingStub;

/**
 * @author shane.xie@practiceinsigt.io
 */
@Singleton
public class LawFirmSearchServiceBlockingStubProvider implements Provider<NakedLawFirmSearchServiceBlockingStub> {

  @Override
  public NakedLawFirmSearchServiceBlockingStub get() {
    final PiConfig piConfig = PiConfig.get();
    final ManagedChannel channel =
        ManagedChannelBuilder
            .forAddress(
                piConfig.getServiceHostname(PiKubeServiceImpl.CITATION_SEARCH_SERVICE_HOST),
                piConfig.getInteger(PiKubeServicePort.CITATION_SEARCH_SERVICE_PORT)
            )
            .usePlaintext(true)
            .build();
    return NakedLawFirmSearchServiceGrpc.newBlockingStub(channel);
  }
}
