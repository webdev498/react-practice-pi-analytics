// Copyright (c) 2017 Practice Insight Pty Ltd. All rights reserved.
//jshint esversion:6
//@flow
import "rxjs/add/operator/map";
import "rxjs/add/operator/catch";
import "rxjs/add/operator/mergeMap";
import {fetch, Methods} from "../services/Request";
import {
  getNextUnsortedServiceAddress,
  serviceAddressAssignError,
  serviceAddressUnsorted,
  undoServiceAddressSuccess,
  unsortedServiceAddressFetchError,
  unsortedServiceAddressFulfilled,
  unsortedServiceAddressPreFetched
} from "../reducers/root";
import {searchQueryError, searchQueryFulfilled} from "../reducers/search";
import {firmCreationError, lawFirmCreated} from "../reducers/createfirmdialog";
import {
  ASSIGN_SERVICE_ADDRESS,
  CREATE_LAW_FIRM,
  FETCH_NEXT_UNSORTED_SERVICE_ADDRESS,
  GET_CURRENT_USER,
  PRE_FETCH_NEXT_UNSORTED_SERVICE_ADDRESS,
  SEARCH_LAW_FIRMS,
  SET_SERVICE_ADDRESS_AS_NON_LAW_FIRM,
  SKIP_SERVICE_ADDRESS,
  UNDO_SERVICE_ADDRESS,
  UNSORT_SERVICE_ADDRESS
} from "../actions/FetchActions";
import {ActionsObservable} from "redux-observable";
import Authentication from "../services/Authentication";
import camelcaseKeysDeep from "camelcase-keys-deep";

const createServiceRequest = (url: string, action: Action): Request => ({
  url: url,
  method: Methods.POST,
  body: action.payload.request ? action.payload.request : {}
})

export const fromResponse = (response: Object): Object => u.freeze(camelcaseKeysDeep(response));

export const fetchNextUnsortedServiceAddress = (action$: ActionsObservable<Action>): ActionsObservable<Action> =>
  action$.ofType(FETCH_NEXT_UNSORTED_SERVICE_ADDRESS)
    .mergeMap(action => fetch(createServiceRequest("api/v1/addressing/nextunsortedserviceaddress", action))
      .map(response => unsortedServiceAddressFulfilled(fromResponse(response)))
      .catch(error => unsortedServiceAddressFetchError()));

export const preFetchNextUnsortedServiceAddress = (action$: ActionsObservable<Action>): ActionsObservable<Action> =>
  action$.ofType(PRE_FETCH_NEXT_UNSORTED_SERVICE_ADDRESS)
    .mergeMap(action => fetch(createServiceRequest("api/v1/addressing/nextunsortedserviceaddress", action))
      .map(response => unsortedServiceAddressPreFetched(fromResponse(response)));

export const searchLawFirm = (action$: ActionsObservable<Action>): ActionsObservable<Action> =>
  action$.ofType(SEARCH_LAW_FIRMS).mergeMap(action => fetch(createServiceRequest("api/v1/addressing/searchlawfirms", action))
    .map(response => searchQueryFulfilled(fromResponse(response)))
    .catch(error => searchQueryError()));

export const assignServiceAddress = (action$: ActionsObservable<Action>): ActionsObservable<Action> =>
  action$.ofType(ASSIGN_SERVICE_ADDRESS)
    .mergeMap(action => fetch(createServiceRequest("api/v1/addressing/assignserviceaddress", action))
      .map(response => getNextUnsortedServiceAddress())
      .catch(error => serviceAddressAssignError()));

export const unsortServiceAddress = (action$: ActionsObservable<Action>): ActionsObservable<Action> =>
  action$.ofType(UNSORT_SERVICE_ADDRESS)
    .mergeMap(action => fetch(createServiceRequest("api/v1/addressing/unsortserviceaddress", action))
      .map(response => serviceAddressUnsorted())
      .catch(error => serviceAddressUnsorted()));

export const undoServiceAddress = (action$: ActionsObservable<Action>): ActionsObservable<Action> =>
  action$.ofType(UNDO_SERVICE_ADDRESS)
    .mergeMap(action => fetch(createServiceRequest("api/v1/addressing/unsortserviceaddress", action))
      .map(response => undoServiceAddressSuccess())
      .catch(error => serviceAddressUnsorted()));

export const createLawFirm = (action$: ActionsObservable<Action>): ActionsObservable<Action> =>
  action$.ofType(CREATE_LAW_FIRM)
    .mergeMap(action => fetch(createServiceRequest("api/v1/addressing/createlawfirm", action))
      .map(response => lawFirmCreated(fromResponse(response)))
      .catch(error => firmCreationError()));

export const setServiceAddressAsNonLawFirm = (action$: ActionsObservable<Action>): ActionsObservable<Action> =>
  action$.ofType(SET_SERVICE_ADDRESS_AS_NON_LAW_FIRM)
    .mergeMap(action => fetch(createServiceRequest("api/v1/addressing/setserviceaddressasnonlawfirm", action))
      .map(response => getNextUnsortedServiceAddress())
      .catch(error => unsortedServiceAddressFetchError()));

export const skipServiceAddress = (action$: ActionsObservable<Action>): ActionsObservable<Action> =>
  action$.ofType(SKIP_SERVICE_ADDRESS)
    .mergeMap(action => fetch(createServiceRequest("/int/api/v1/addressing/skipserviceaddress", action))
      .map(response => getNextUnsortedServiceAddress())
      .catch(error => unsortedServiceAddressFetchError()));

export const getCurrentUser = (acitons$: ActionsObservable<Action>): ActionsObservable<Action> =>
  acitons$.ofType(GET_CURRENT_USER)
    .mergeMap(action => fetch({url: "addressing/sessionInfo.jsp", method: Methods.GET})
      .map(response => {
        Authentication.user = response.username;
        return getNextUnsortedServiceAddress();
      }));