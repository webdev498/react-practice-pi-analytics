/*
 * Copyright (c) 2017 Practice Insight Pty Ltd.
 */

package pi.analytics.admin.serviceaddress.service;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import pi.admin.service_address_sorting.generated.AssignServiceAddressRequest;
import pi.admin.service_address_sorting.generated.CreateLawFirmRequest;
import pi.admin.service_address_sorting.generated.LawFirmCreated;
import pi.admin.service_address_sorting.generated.NextUnsortedServiceAddressRequest;
import pi.admin.service_address_sorting.generated.SearchLawFirmsRequest;
import pi.admin.service_address_sorting.generated.SearchResults;
import pi.admin.service_address_sorting.generated.ServiceAddressAssigned;
import pi.admin.service_address_sorting.generated.ServiceAddressBundle;
import pi.admin.service_address_sorting.generated.ServiceAddressSetAsNonLawFirm;
import pi.admin.service_address_sorting.generated.ServiceAddressSortingServiceGrpc;
import pi.admin.service_address_sorting.generated.ServiceAddressUnsorted;
import pi.admin.service_address_sorting.generated.SetServiceAddressAsNonLawFirmRequest;
import pi.admin.service_address_sorting.generated.UnsortServiceAddressRequest;
import pi.analytics.admin.serviceaddress.service.law_firm.LawFirmRepository;
import pi.analytics.admin.serviceaddress.service.service_address.ServiceAddressBundleFetcher;
import pi.analytics.admin.serviceaddress.service.service_address.UnsortedServiceAddressFetcher;

/**
 * @author shane.xie@practiceinsight.io
 */
@Singleton
public class ServiceAddressSortingServiceImpl extends ServiceAddressSortingServiceGrpc.ServiceAddressSortingServiceImplBase {

  private static final Logger log = LoggerFactory.getLogger(ServiceAddressSortingServiceImpl.class);

  @Inject
  private UnsortedServiceAddressFetcher unsortedServiceAddressFetcher;

  @Inject
  ServiceAddressBundleFetcher serviceAddressBundleFetcher;

  @Inject
  LawFirmRepository lawFirmRepository;

  @Override
  public void nextUnsortedServiceAddress(final NextUnsortedServiceAddressRequest request,
                                         final StreamObserver<ServiceAddressBundle> responseObserver) {
    try {
      final Optional<ServiceAddressBundle> serviceAddressBundle =
          unsortedServiceAddressFetcher
              .fetchNext(request.getRequestedBy())
              .filter(queuedServiceAddress -> queuedServiceAddress.serviceAddress().isPresent())
              .map(serviceAddressBundleFetcher::fetch);

      if (!serviceAddressBundle.isPresent()) {
        // Nothing in queues
        responseObserver.onError(Status.NOT_FOUND.asRuntimeException());
      }
      responseObserver.onNext(serviceAddressBundle.get());
      responseObserver.onCompleted();
    } catch (Throwable th) {
      log.error("Error generating next unsorted service address", th);
      responseObserver.onError(th);
    }
  }

  @Override
  public void searchLawFirms(final SearchLawFirmsRequest request, final StreamObserver<SearchResults> responseObserver) {
    try {
      final SearchResults searchResults =
          SearchResults
              .newBuilder()
              .addAllLawFirmAgents(lawFirmRepository.searchLawFirms(request.getSearchTerm()))
              .build();
      responseObserver.onNext(searchResults);
      responseObserver.onCompleted();
    } catch (Throwable th) {
      log.error("Error searching law firms", th);
      responseObserver.onError(th);
    }
  }

  @Override
  public void assignServiceAddress(final AssignServiceAddressRequest request,
                                   final StreamObserver<ServiceAddressAssigned> responseObserver) {
    // TODO(SX)
    // Assign a service address to a law firm (Cloud SQL)
    // Update lawfirm_service_address_v1 ES index at http://es-1.hatchedpi.com:9200/_cat/indices
    // Also see rpc UpsertThinLawFirmServiceAddress RPC endpoint from DatastoreSg3Service
    // Remember to delete from queue
    // Also record staff user action (IPFLOW-786)?
  }

  @Override
  public void unsortServiceAddress(final UnsortServiceAddressRequest request,
                                   final StreamObserver<ServiceAddressUnsorted> responseObserver) {
    // TODO(SX)
    // Unassign a service address from its law firm if any, set law_firm_entity_checked to false (Cloud SQL)
    // Update lawfirm_service_address_v1 ES index at http://es-1.hatchedpi.com:9200/_cat/indices
    // Also see rpc DeleteThinLawFirmServiceAddress RPC endpoint from DatastoreSg3Service
    // Also record staff user action (IPFLOW-786)?
  }

  @Override
  public void createLawFirm(final CreateLawFirmRequest request, final StreamObserver<LawFirmCreated> responseObserver) {
    // TODO(SX)
    // Create a new law firm and assign the service address to it (Cloud SQL)
    // Update ES indexes at http://es-1.hatchedpi.com:9200/_cat/indices
    // * lawfirm
    // * lawfirm_service_address_v1
    // Also see rpc UpsertThinLawFirmServiceAddress RPC endpoint from DatastoreSg3Service
    // Remember to delete from queue
    // Also record staff user action (IPFLOW-786)?
  }

  @Override
  public void setServiceAddressAsNonLawFirm(final SetServiceAddressAsNonLawFirmRequest request,
                                            final StreamObserver<ServiceAddressSetAsNonLawFirm> responseObserver) {
    // TODO(SX)
    // Remember to delete from queue
    // Also record staff user action (IPFLOW-786)?
  }

}
