/*
 * Copyright (c) 2017 Practice Insight Pty Ltd.
 */

package pi.analytics.admin.serviceaddress.guice;

import com.pi.common.config.PiConfig;
import com.pi.common.config.PiKubeServiceImpl;
import com.pi.common.config.PiKubeServicePort;

import javax.inject.Provider;
import javax.inject.Singleton;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import pi.ip.generated.queue.QueueOnPremGrpc;

/**
 * @author shane.xie
 */
@Singleton
public class QueueOnPremBlockingStubProvider implements Provider<QueueOnPremGrpc.QueueOnPremBlockingStub> {

  @Override
  public QueueOnPremGrpc.QueueOnPremBlockingStub get() {
    final PiConfig piConfig = PiConfig.get();
    final ManagedChannel channel =
        ManagedChannelBuilder
            .forAddress(
                piConfig.getServiceHostname(PiKubeServiceImpl.DATASTORE_SERVICE_ONPREM_HOST),
                piConfig.getInteger(PiKubeServicePort.DATASTORE_SERVICE_ONPREM_PORT)
            )
            .build();
    return QueueOnPremGrpc.newBlockingStub(channel);
  }
}
