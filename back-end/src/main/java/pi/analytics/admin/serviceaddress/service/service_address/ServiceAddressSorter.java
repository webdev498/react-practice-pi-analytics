/*
 * Copyright (c) 2017 Practice Insight Pty Ltd.
 */

package pi.analytics.admin.serviceaddress.service.service_address;

import com.google.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import io.practiceinsight.licensingalert.citationsearch.generated.LawFirmSearchServiceGrpc.LawFirmSearchServiceBlockingStub;
import pi.admin.service_address_sorting.generated.AssignServiceAddressRequest;
import pi.admin.service_address_sorting.generated.SetServiceAddressAsNonLawFirmRequest;
import pi.admin.service_address_sorting.generated.UnsortServiceAddressRequest;
import pi.ip.data.relational.generated.ServiceAddressServiceGrpc.ServiceAddressServiceBlockingStub;

/**
 * @author shane.xie@practiceinsight.io
 */
public class ServiceAddressSorter {

  private static final Logger log = LoggerFactory.getLogger(ServiceAddressSorter.class);

  @Inject
  LawFirmSearchServiceBlockingStub lawFirmSearchServiceBlockingStub;

  @Inject
  ServiceAddressServiceBlockingStub serviceAddressServiceBlockingStub;


  public void assignServiceAddress(final AssignServiceAddressRequest request) {
    // TODO(SX)

    // Assign a service address to a law firm (Cloud SQL)
    // Update lawfirm_service_address_v1 ES index at http://es-1.hatchedpi.com:9200/_cat/indices
    // Also see rpc UpsertThinLawFirmServiceAddress RPC endpoint from DatastoreSg3Service
    // Remember to delete from queue
    // Also record staff user action (IPFLOW-786)?

    throw new NotImplementedException();
  }

  public void unsortServiceAddress(final UnsortServiceAddressRequest request) {
    // TODO(SX)

    // Unassign a service address from its law firm if any, set law_firm_entity_checked to false (Cloud SQL)
    // Update lawfirm_service_address_v1 ES index at http://es-1.hatchedpi.com:9200/_cat/indices
    // Also see rpc DeleteThinLawFirmServiceAddress RPC endpoint from DatastoreSg3Service
    // Also record staff user action (IPFLOW-786)?

    throw new NotImplementedException();
  }

  public void setServiceAddressAsNonLawFirm(final SetServiceAddressAsNonLawFirmRequest request) {
    // TODO(SX)

    // Remember to delete from queue
    // Also record staff user action (IPFLOW-786)?

    throw new NotImplementedException();
  }
}
