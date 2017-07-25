// Copyright (c) 2017 Practice Insight Pty Ltd. All rights reserved.
//jshint esversion:6
//@flow
import "rxjs/add/operator/map";
import "rxjs/add/operator/catch";
import "rxjs/add/operator/mergeMap";
import "rxjs/add/operator/mapTo";
import "rxjs/add/observable/of";
import {Observable} from "rxjs/Observable";
import {get, post} from "../services/Request";
import {
  getNextUnsortedServiceAddress,
  globalFetchError,
  serviceAddressUnsorted,
  undoServiceAddressSuccess,
  unsortedServiceAddressFulfilled,
  unsortedServiceAddressPreFetched,
  unsortedServiceAddressFetchError,
  serviceAddressSkipped
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
  UNDO_SERVICE_ADDRESS,
  UNSORT_SERVICE_ADDRESS,
  SET_INSUFFICIENT_INFO_STATUS,
  SKIP_SERVICE_ADDRESS
} from "../actions/FetchActions";
import {ActionsObservable} from "redux-observable";
import Authentication from "../services/Authentication";
import camelcaseKeysDeep from "camelcase-keys-deep";
import {ApiUrls, OuterUrls} from "../services/Urls";
import u from "updeep";
import type {Action} from "redux";

const mapResponse = method => ajax => method(u.freeze(camelcaseKeysDeep(ajax.response)));
const mapError = method => error => Observable.of(error).mapTo(method());

export const fetchNextUnsortedServiceAddress = (action$: ActionsObservable<Action>): ActionsObservable<Action> =>
  action$.ofType(FETCH_NEXT_UNSORTED_SERVICE_ADDRESS)
    .mergeMap(action => post(ApiUrls.nextUnsortedServiceAddress, action.payload)
      .map(mapResponse(unsortedServiceAddressFulfilled))
      .catch(mapError(unsortedServiceAddressFetchError))
    );

export const preFetchNextUnsortedServiceAddress = (action$: ActionsObservable<Action>): ActionsObservable<Action> =>
  action$.ofType(PRE_FETCH_NEXT_UNSORTED_SERVICE_ADDRESS)
    .mergeMap(action => post(ApiUrls.nextUnsortedServiceAddress, action.payload)
      .map(mapResponse(unsortedServiceAddressPreFetched))
      .catch(error => {})
    );

export const skipServiceAddress = (action$: ActionsObservable<Action>): ActionsObservable<Action> =>
  action$.ofType(SKIP_SERVICE_ADDRESS)
    .mergeMap(action => post(ApiUrls.skipServiceAddress, action.payload)
      .map(mapResponse(serviceAddressSkipped))
      .catch(mapError(globalFetchError))
    );

export const searchLawFirm = (action$: ActionsObservable<Action>): ActionsObservable<Action> =>
  action$.ofType(SEARCH_LAW_FIRMS).mergeMap(action => post(ApiUrls.searchLawFirms, action.payload)
    .map(mapResponse(searchQueryFulfilled))
    .catch(mapError(searchQueryError))
  );

export const assignServiceAddress = (action$: ActionsObservable<Action>): ActionsObservable<Action> =>
  action$.ofType(ASSIGN_SERVICE_ADDRESS)
    .mergeMap(action => post(ApiUrls.assignServiceAddress, action.payload)
      .map(mapResponse(getNextUnsortedServiceAddress))
      .catch(mapError(globalFetchError))
    );

export const unsortServiceAddress = (action$: ActionsObservable<Action>): ActionsObservable<Action> =>
  action$.ofType(UNSORT_SERVICE_ADDRESS)
    .mergeMap(action => post(ApiUrls.unsortServiceAddress, action.payload)
      .map(mapResponse(serviceAddressUnsorted))
      .catch(mapError(globalFetchError))
    );

export const undoServiceAddress = (action$: ActionsObservable<Action>): ActionsObservable<Action> =>
  action$.ofType(UNDO_SERVICE_ADDRESS)
    .mergeMap(action => post(ApiUrls.unsortServiceAddress, action.payload)
      .map(mapResponse(undoServiceAddressSuccess))
      .catch(mapError(globalFetchError))
    );

export const createLawFirm = (action$: ActionsObservable<Action>): ActionsObservable<Action> =>
  action$.ofType(CREATE_LAW_FIRM)
    .mergeMap(action => post(ApiUrls.createLawFirm, action.payload)
      .map(mapResponse(lawFirmCreated))
      .catch(mapError(firmCreationError))
    );

export const setServiceAddressAsNonLawFirm = (action$: ActionsObservable<Action>): ActionsObservable<Action> =>
  action$.ofType(SET_SERVICE_ADDRESS_AS_NON_LAW_FIRM)
    .mergeMap(action => post(ApiUrls.setServiceAddressAsNonLawFirm, action.payload)
      .map(mapResponse(getNextUnsortedServiceAddress))
      .catch(mapError(globalFetchError))
    );

export const setInsufficientInfoStatus = (action$: ActionsObservable<Action>): ActionsObservable<Action> =>
  action$.ofType(SET_INSUFFICIENT_INFO_STATUS)
    .mergeMap(action => post(ApiUrls.setInsufficientInfoStatus, action.payload)
      .map(mapResponse(getNextUnsortedServiceAddress))
      .catch(mapError(globalFetchError))
    );

export const getCurrentUser = (acitons$: ActionsObservable<Action>): ActionsObservable<Action> =>
  acitons$.ofType(GET_CURRENT_USER)
    .mergeMap(action => get(OuterUrls.sessionInfo)
      .map(response => {
        Authentication.user = response.username;
        return Observable.of(response).mapTo(getNextUnsortedServiceAddress());
      })
      .catch(error => {
        var urlParams = new URLSearchParams(location.search);
        if (urlParams.has("username")) {
          Authentication.user = urlParams.get("username");
          return Observable.of(error).mapTo(getNextUnsortedServiceAddress());
        } else {
          window.location = OuterUrls.login;
          console.log(error);
        }
      }));