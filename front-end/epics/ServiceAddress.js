// Copyright (c) 2017 Practice Insight Pty Ltd. All rights reserved.
//jshint esversion:6
//@flow
import {fetch, Methods} from "../web-service/Request";
import {unsortedServiceAddressFetchError, unsortedServiceAddressFulfilled} from "../reducers/root";
import {
  ASSIGN_SERVICE_ADDRESS,
  CREATE_LAW_FIRM,
  FETCH_NEXT_UNSORTED_SERVICE_ADDRESS,
  SEARCH_LAW_FIRMS,
  SET_SERVICE_ADDRESS_AS_NON_LAW_FIRM,
  UNSORT_SERVICE_ADDRESS
} from "../actions/FetchActions";

const createRequest = (url: string, action: Action): Request => ({
  url: url,
  method: Methods.POST,
  body: action.payload
})

export const fetchNextUnsortedServiceAddress = action$ =>
  action$.ofType(FETCH_NEXT_UNSORTED_SERVICE_ADDRESS)
    .mergeMap(action => fetch(createRequest("api/v1/addressing/nextunsortedserviceaddress", action)
                                .map(response => unsortedServiceAddressFulfilled(response))
                                .catch(error => unsortedServiceAddressFetchError());

export const searchLawFirm = action$ =>
  action$.ofType(SEARCH_LAW_FIRMS).mergeMap(action => fetch(createRequest("api/v1/addressing/searchlawfirms", action)
                                                              .map(response => unsortedServiceAddressFulfilled(response))
                                                              .catch(error => unsortedServiceAddressFetchError());

export const assignServiceAddress = action$ =>
  action$.ofType(ASSIGN_SERVICE_ADDRESS)
    .mergeMap(action => fetch(createRequest("api/v1/addressing/assignserviceaddress", action)
                                .map(response => unsortedServiceAddressFulfilled(response))
                                .catch(error => unsortedServiceAddressFetchError());

export const unsortServiceAddress = action$ =>
  action$.ofType(UNSORT_SERVICE_ADDRESS)
    .mergeMap(action => fetch(createRequest("api/v1/addressing/unsortserviceaddress", action)
                                .map(response => unsortedServiceAddressFulfilled(response))
                                .catch(error => unsortedServiceAddressFetchError());

export const createLadFirm = action$ =>
  action$.ofType(CREATE_LAW_FIRM)
    .mergeMap(action => fetch(createRequest("api/v1/addressing/createlawfirm", action)
                                .map(response => unsortedServiceAddressFulfilled(response))
                                .catch(error => unsortedServiceAddressFetchError());

export const setServiceAddressAsNonLawFirm = action$ =>
  action$.ofType(SET_SERVICE_ADDRESS_AS_NON_LAW_FIRM)
    .mergeMap(action => fetch(createRequest("api/v1/addressing/setserviceaddressasnonlawfirm", action)
                                .map(response => unsortedServiceAddressFulfilled(response))
                                .catch(error => unsortedServiceAddressFetchError());

