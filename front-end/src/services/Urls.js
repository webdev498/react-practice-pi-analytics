// Copyright (c) 2017 Practice Insight Pty Ltd. All rights reserved.
//jshint esversion:6
//@flow

const baseUrl = "http://172.16.44.212/int/api/v1/addressing";

export const ApiUrls = {
  nextUnsortedServiceAddress: baseUrl.concat("/nextunsortedserviceaddress"),
  searchLawFirms: baseUrl.concat("/searchlawfirms"),
  assignServiceAddress: baseUrl.concat("/assignserviceaddress"),
  unsortServiceAddress: baseUrl.concat("/unsortserviceaddress"),
  createLawFirm: baseUrl.concat("/createlawfirm"),
  setServiceAddressAsNonLawFirm: baseUrl.concat("/setserviceaddressasnonlawfirm"),
  skipServiceAddress: baseUrl.concat("/skipserviceaddress")
}

export const OuterUrls = {
  sessionInfo: "addressing/sessionInfo.jsp",
  login: "login.jsp#agents",
  firmInfo: "dataServices/firmInfo.jsp?id=",
  googleSearch: "https://www.google.com/search?q=",
  country: "addressing/localCountryC.jsp?findLocalEntity=Find&localID="
}