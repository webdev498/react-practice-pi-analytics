// Copyright (c) 2017 Practice Insight Pty Ltd. All rights reserved.
//jshint esversion:6
//@flow

const API_BASE_URL = "http://172.16.44.212/int/"

export const ApiUrls = {
  nextUnsortedServiceAddress: API_BASE_URL + "api/v1/addressing/nextunsortedserviceaddress",
  searchLawFirms: API_BASE_URL + "api/v1/addressing/searchlawfirms",
  assignServiceAddress: API_BASE_URL + "api/v1/addressing/assignserviceaddress",
  unsortServiceAddress: API_BASE_URL + "api/v1/addressing/unsortserviceaddress",
  createLawFirm: API_BASE_URL + "api/v1/addressing/createlawfirm",
  setServiceAddressAsNonLawFirm: API_BASE_URL + "api/v1/addressing/setserviceaddressasnonlawfirm",
  skipServiceAddress: API_BASE_URL + "api/v1/addressing/skipserviceaddress"
}

export const OuterUrls = {
  sessionInfo: "addressing/sessionInfo.jsp",
  login: "login.jsp#agents",
  firmInfo: "dataServices/firmInfo.jsp?id=",
  googleSearch: "https://www.google.com/search?q=",
  country: "addressing/localCountryC.jsp?findLocalEntity=Find&localID="
}