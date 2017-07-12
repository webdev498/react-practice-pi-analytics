/*
 * Copyright (c) 2017 Practice Insight Pty Ltd.
 */

package pi.analytics.admin.serviceaddress.service.service_address;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import pi.analytics.admin.serviceaddress.service.user.UserService;
import pi.ip.data.relational.generated.GetNextServiceAddressForSortingRequest;
import pi.ip.data.relational.generated.ServiceAddressServiceGrpc;
import pi.ip.proto.generated.ServiceAddress;

/**
 * @author shane.xie@practiceinsight.io
 */
@Singleton
public class SortableServiceAddressFetcher {

  private static final Logger log = LoggerFactory.getLogger(SortableServiceAddressFetcher.class);

  private static final Set<String> COUNTRIES_TO_SORT =
    ImmutableSet.of("AU", "AT", "BE", "BR", "CA", "CN", "CZ", "DK", "FI", "FR",
        "DE", "HK", "IS", "IN", "IE", "IL", "IT", "JP", "KR", "LI", "LU", "NL", "NZ", "NO", "RU", "SG", "ZA", "ES", "SE",
        "CH", "TW", "GB", "US"
    );

  private static final Set<String> DISABLED_COUNTRIES = ImmutableSet.of("TW");  // Taiwan addresses not ready for sorting

  @Inject
  private UserService userService;

  @Inject
  private ServiceAddressServiceGrpc.ServiceAddressServiceBlockingStub serviceAddressServiceBlockingStub;

  public Optional<ServiceAddress> fetchNext(final String username) {
    final Set<String> officeCodes =
        COUNTRIES_TO_SORT
            .stream()
            .filter(country -> !DISABLED_COUNTRIES.contains(country))
            .collect(Collectors.toSet());

    final GetNextServiceAddressForSortingRequest request =
        GetNextServiceAddressForSortingRequest
            .newBuilder()
            .addAllRestrictToLangTypes(userService.getLangTypes(username))
            .addAllRestrictToOfficeCodes(officeCodes)
            .setAlreadySortedWeightedChance(userService.getAlreadySortedWeightedChance(username))
            .build();

    try {
      return Optional.of(serviceAddressServiceBlockingStub.getNextServiceAddressForSorting(request));
    } catch (StatusRuntimeException sre) {
      if (sre.getStatus().equals(Status.NOT_FOUND)) {
        return Optional.empty();
      }
      // Any other status is an error
      throw sre;
    }
  }
}
