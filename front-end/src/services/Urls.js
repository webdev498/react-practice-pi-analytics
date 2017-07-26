// Copyright (c) 2017 Practice Insight Pty Ltd. All rights reserved.
//jshint esversion:6
//@flow

const getBaseUrl = (): string => {
  const url = window.location.href.toString()
  if (url.startsWith("http://localhost")  // Local development
      || url.startsWith("http://172.16.37.6/")) {  // IP address of service address sorting front-end k8s service
    return "http://172.16.44.212/int/"  // service-address-admin-grpc-gateway IP address
  }
  return "/int/"
}

const API_BASE_URL = getBaseUrl()

export const ApiUrls = {
  nextUnsortedServiceAddress: API_BASE_URL + "api/v1/addressing/nextunsortedserviceaddress",
  searchLawFirms: API_BASE_URL + "api/v1/addressing/searchlawfirms",
  assignServiceAddress: API_BASE_URL + "api/v1/addressing/assignserviceaddress",
  unsortServiceAddress: API_BASE_URL + "api/v1/addressing/unsortserviceaddress",
  createLawFirm: API_BASE_URL + "api/v1/addressing/createlawfirm",
  setServiceAddressAsNonLawFirm: API_BASE_URL + "api/v1/addressing/setserviceaddressasnonlawfirm",
  skipServiceAddress: API_BASE_URL + "api/v1/addressing/skipserviceaddress",
  setInsufficientInfoStatus: API_BASE_URL + "api/v1/addressing/insufficientinfotosort",
}

export const OuterUrls = {
  sessionInfo: "http://practiceinsight.io/int/addressing/sessionInfo.jsp",
  // login: "http://practiceinsight.io/int/login.jsp#agents",
  login: "/int/addressing",
  firmInfo: "http://practiceinsight.io/int/dataServices/firmInfo.jsp?id=",
  googleSearch: "https://www.google.com/search?q=",
  country: "https://practiceinsight.io/int/addressing/localCountryC.jsp?findLocalEntity=Find&localID=",
}
