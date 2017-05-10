package pi.analytics.admin.serviceaddress.guice;

import com.google.inject.Provider;
import com.google.inject.Singleton;

import com.pi.common.config.PiConfig;
import com.pi.common.config.PiKubeServiceImpl;
import com.pi.common.config.PiKubeServicePort;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.practiceinsight.licensingalert.citationsearch.generated.LawFirmSearchServiceGrpc;
import io.practiceinsight.licensingalert.citationsearch.generated.LawFirmSearchServiceGrpc.LawFirmSearchServiceBlockingStub;

/**
 * @author shane.xie@practiceinsigt.io
 */
@Singleton
public class LawFirmSearchServiceBlockingStubProvider implements Provider<LawFirmSearchServiceBlockingStub> {

    @Override
    public LawFirmSearchServiceBlockingStub get() {
      final PiConfig piConfig = PiConfig.get();
      final ManagedChannel channel =
          ManagedChannelBuilder
              .forAddress(
                  piConfig.getServiceHostname(PiKubeServiceImpl.CITATION_SEARCH_SERVICE_HOST),
                  piConfig.getInteger(PiKubeServicePort.CITATION_SEARCH_SERVICE_PORT)
              )
              .build();
      return LawFirmSearchServiceGrpc.newBlockingStub(channel);
    }
  }
}
