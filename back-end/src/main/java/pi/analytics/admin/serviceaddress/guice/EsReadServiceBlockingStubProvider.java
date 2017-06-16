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
import pi.ip.generated.es.ESReadServiceGrpc;
import pi.ip.generated.es.ESReadServiceGrpc.ESReadServiceBlockingStub;

/**
 * @author shane.xie@practiceinsight.io
 */
@Singleton
public class EsReadServiceBlockingStubProvider implements Provider<ESReadServiceBlockingStub> {

    @Override
    public ESReadServiceBlockingStub get() {
      final PiConfig piConfig = PiConfig.get();
      final ManagedChannel channel =
          ManagedChannelBuilder
              .forAddress(
                  piConfig.getServiceHostname(PiKubeServiceImpl.IP_ELASTICSEARCH_SERVICE_HOST),
                  piConfig.getInteger(PiKubeServicePort.IP_ELASTICSEARCH_SERVICE_PORT)
              )
              .usePlaintext(true)
              .build();
      return ESReadServiceGrpc.newBlockingStub(channel);
    }
}
