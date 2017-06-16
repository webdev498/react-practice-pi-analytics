/*
 * Copyright (c) 2017 Practice Insight Pty Ltd.
 */

package pi.analytics.admin.serviceaddress.service.service_address;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import pi.ip.data.relational.generated.GetNextUnsortedServiceAddressRequest;
import pi.ip.data.relational.generated.ServiceAddressServiceGrpc;
import pi.ip.proto.generated.LangType;
import pi.ip.proto.generated.ServiceAddress;

/**
 * @author shane.xie@practiceinsight.io
 */
@Singleton
public class UnsortedServiceAddressFetcher {

  private static final Logger log = LoggerFactory.getLogger(UnsortedServiceAddressFetcher.class);

  private static final Set<String> ACTIVE_COUNTRY_CODES =
    ImmutableSet.of("AU", "AT", "BE", "BR", "CA", "CN", "CZ", "DK", "FI", "FR",
        "DE", "HK", "IS", "IN", "IE", "IL", "IT", "JP", "KR", "LI", "LU", "NL", "NZ", "NO", "RU", "SG", "ZA", "ES", "SE",
        "CH", "TW", "GB", "US"
    );

  private static final Set<String> IGNORED_COUNTRY_CODES = ImmutableSet.of("TW");

  @Inject
  private ServiceAddressServiceGrpc.ServiceAddressServiceBlockingStub serviceAddressServiceBlockingStub;

  public Optional<ServiceAddress> fetchNext(final String username) {
    final Set<String> officeCodes =
        ACTIVE_COUNTRY_CODES
            .stream()
            .filter(country -> !IGNORED_COUNTRY_CODES.contains(country))
            .collect(Collectors.toSet());

    final GetNextUnsortedServiceAddressRequest request =
        GetNextUnsortedServiceAddressRequest
            .newBuilder()
            .addAllRestrictToLangTypes(langTypesForUser(username))
            .addAllRestrictToOfficeCodes(officeCodes)
            .build();

    try {
      return Optional.of(serviceAddressServiceBlockingStub.getNextUnsortedServiceAddress(request));
    } catch (StatusRuntimeException sre) {
      if (sre.getStatus().equals(Status.NOT_FOUND)) {
        return Optional.empty();
      }
      // Any other status is an error
      throw sre;
    }
  }

  private List<LangType> langTypesForUser(final String userName) {
    if (StringUtils.equalsIgnoreCase(userName, "hellen")) {
      return ImmutableList.of(
          LangType.CHINESE,
          LangType.WESTERN_SCRIPT,
          LangType.JAPANESE
      );
    } else {
      return ImmutableList.of(
          LangType.WESTERN_SCRIPT,
          LangType.KOREAN,
          LangType.JAPANESE,
          LangType.CYRILLIC
      );
    }
  }
}
