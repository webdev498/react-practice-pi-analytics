/*
 * Copyright (c) 2017 Practice Insight Pty Ltd.
 */

package pi.analytics.admin.serviceaddress.server;

import com.google.inject.Guice;
import com.google.inject.Injector;

import pi.analytics.admin.serviceaddress.guice.ServiceAddressSortingModule;

/**
 * @author shane.xie@practiceinsight.io
 */
public class ServiceAddressSortingServerLauncher {

  public static void main(final String[] args) throws Exception {
    final Injector injector = Guice.createInjector(new ServiceAddressSortingModule());

    injector.getInstance(MetricsWebServer.class).start();

    final ServiceAddressSortingServer server = injector.getInstance(ServiceAddressSortingServer.class);
    server.start().awaitTermination();
  }
}
