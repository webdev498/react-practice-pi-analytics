// Copyright (c) 2017 Practice Insight Pty Ltd. All rights reserved.
//jshint esversion:6
//@flow
export type NextUnsortedServiceAddressRequest = {
  requested_by: string
}

export type ServiceAddressBundle = {
  service_address_to_sort: ServiceAddress,
  en_translation: string,
  suggested_agents: Array<Agent>
}

export type ServiceAddress = {
  service_address_id: number,
  law_firm_id: number,
  law_firm_entity_checked: boolean,
  name: string,
  address: string,
  country: string,
  telephone: string,
  language_type: string
}

export type Agent {
  law_firm: Object,
  non_law_firm: Object,
  service_addresses: Array<ServiceAddress>
}

export type NonLawFirm {
  name: string
}

export type SearchLawFirmsRequest {
  requested_by: string,
  search_term: string
}

export type SearchResults {
  law_firm_agents: Array<Agent>
}

export type AssignServiceAddressRequest {
  requested_by: string,
  service_address_id: number,
  law_firm_id: number
}

export type ServiceAddressAssigned {
}

export type UnsortServiceAddressRequest {
  requested_by: string,
  service_address_id: number
}

export type ServiceAddressUnsorted {
}

export type CreateLawFirmRequest {
  requested_by: string,
  name: string,
  state: string,
  country_code: string,
  website_url: string,
  service_address: ServiceAddress
}

export type LawFirmCreated {
  law_firm_id: number
}

export type SetServiceAddressAsNonLawFirmRequest {
  requested_by: string,
  service_address_id: int
}

export type ServiceAddressSetAsNonLawFirm {
}