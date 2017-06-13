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
import pi.ip.generated.es.ESMutationServiceGrpc;
import pi.ip.generated.es.ESMutationServiceGrpc.ESMutationServiceBlockingStub;

/**
 * @author shane.xie@practiceinsight.io
 */
@Singleton
public class EsMutationServiceBlockingStubProvider implements Provider<ESMutationServiceBlockingStub> {

    @Override
    public ESMutationServiceBlockingStub get() {
      final PiConfig piConfig = PiConfig.get();
      final ManagedChannel channel =
          ManagedChannelBuilder
              .forAddress(
                  piConfig.getServiceHostname(PiKubeServiceImpl.ES_MUTATION_SERVICE_HOST),
                  piConfig.getInteger(PiKubeServicePort.ES_MUTATION_SERVICE_PORT)
              )
              .usePlaintext(true)
              .build();
      return ESMutationServiceGrpc.newBlockingStub(channel);
    }
}
