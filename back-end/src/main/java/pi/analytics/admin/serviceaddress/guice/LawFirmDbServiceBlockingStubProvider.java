/*
 * Copyright (c) 2017 Practice Insight Pty Ltd.
 */

package pi.analytics.admin.serviceaddress.guice;

import com.google.inject.Provider;
import com.google.inject.Singleton;

import com.pi.common.config.PiConfig;
import com.pi.common.config.PiKubeServiceImpl;
import com.pi.common.config.PiKubeServicePort;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import pi.ip.data.relational.generated.LawFirmDbServiceGrpc;

/**
 * @author shane.xie@practiceinsight.io
 */
@Singleton
public class LawFirmDbServiceBlockingStubProvider implements Provider<LawFirmDbServiceGrpc.LawFirmDbServiceBlockingStub> {

  @Override
  public LawFirmDbServiceGrpc.LawFirmDbServiceBlockingStub get() {
    final PiConfig piConfig = PiConfig.get();
    final ManagedChannel channel =
        ManagedChannelBuilder
            .forAddress(
                piConfig.getServiceHostname(PiKubeServiceImpl.IP_DATA_RELATIONAL_SERVICE_HOST),
                piConfig.getInteger(PiKubeServicePort.IP_DATA_RELATIONAL_SERVICE_PORT)
            )
            .usePlaintext(true)
            .build();
    return LawFirmDbServiceGrpc.newBlockingStub(channel);
  }
}
