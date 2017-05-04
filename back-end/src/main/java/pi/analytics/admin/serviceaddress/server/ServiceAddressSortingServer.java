/*
 * Copyright (c) 2017 Practice Insight Pty Ltd.
 */

package pi.analytics.admin.serviceaddress.server;

import com.pi.common.config.PiConfig;
import com.pi.common.config.PiKubeServicePort;

import java.io.IOException;

import javax.inject.Inject;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import pi.analytics.admin.serviceaddress.service.ServiceAddressSortingServiceImpl;

/**
 * @author shane.xie@practiceinsight.io
 */
public class ServiceAddressSortingServer {

  @Inject
  private ServiceAddressSortingServiceImpl serviceAddressSortingService;

  public final Server start() throws IOException {
    final Server server =
        ServerBuilder
            .forPort(PiConfig.get().getInteger(PiKubeServicePort.SERVICE_ADDRESS_SORTING_SERVICE_PORT))
            .addService(serviceAddressSortingService)
            .build();
    server.start();
    return server;
  }
}
