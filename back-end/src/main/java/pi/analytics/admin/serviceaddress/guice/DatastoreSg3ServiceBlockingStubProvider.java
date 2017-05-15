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
import pi.ip.generated.datastore_sg3.DatastoreSg3ServiceGrpc;
import pi.ip.generated.datastore_sg3.DatastoreSg3ServiceGrpc.DatastoreSg3ServiceBlockingStub;

/**
 * @author shane.xie@practiceinsight.io
 */
@Singleton
public class DatastoreSg3ServiceBlockingStubProvider implements Provider<DatastoreSg3ServiceBlockingStub> {

    @Override
    public DatastoreSg3ServiceBlockingStub get() {
      final PiConfig piConfig = PiConfig.get();
      final ManagedChannel channel =
          ManagedChannelBuilder
              .forAddress(
                  piConfig.getServiceHostname(PiKubeServiceImpl.DATASTORE_SERVICE_SG3_HOST),
                  piConfig.getInteger(PiKubeServicePort.DATASTORE_SERVICE_SG3_PORT)
              )
              .usePlaintext(true)
              .build();
      return DatastoreSg3ServiceGrpc.newBlockingStub(channel);
    }
}
