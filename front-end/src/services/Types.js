// Copyright (c) 2017 Practice Insight Pty Ltd. All rights reserved.
//jshint esversion:6
//@flow

export type SamplePatentApp = {
  appNum: string,
  applicants: Array<string>
}

export type ServiceAddress = {
  serviceAddressId: string,
  lawFirmId?: number,
  lawFirmEntityChecked?: boolean,
  name: string,
  address: string,
  country?: string,
  telephone?: string,
  languageType?: string
}

export type Agent = {
  lawFirm?: Object,
  nonLawFirm?: Object,
  serviceAddresses: Array<ServiceAddress>
}

export type ServiceAddressBundle = {
  serviceAddressToSort: ServiceAddress,
  enTranslation?: string,
  suggestedAgents?: Array<Agent>,
  samplePatentApps?: Array<SamplePatentApp>
}

export type NonLawFirm = {
  name: string
}

export type LawFirm = {
  lawFirmId: number,
  name: string,
  stateStr: string,
  country: string,
  websiteUrl: string
}

export type SearchResults = {
  lawFirmAgents: Array <Agent>
}