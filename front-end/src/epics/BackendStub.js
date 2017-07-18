// Copyright (c) 2017 Practice Insight Pty Ltd. All rights reserved.
//jshint esversion:6
//@flow
import "rxjs/add/operator/map";
import "rxjs/add/operator/mapTo";
import "rxjs/add/operator/delay";
import {
  getNextUnsortedServiceAddress,
  serviceAddressUnsorted,
  serviceAddressSkipped,
  undoServiceAddressSuccess,
  unsortedServiceAddressFulfilled,
  unsortedServiceAddressFetchError,
  unsortedServiceAddressPreFetched,
} from "../reducers/root";
import {
  firmCreationError
} from "../reducers/createfirmdialog";
import {searchQueryFulfilled} from "../reducers/search";
import {lawFirmCreated} from "../reducers/createfirmdialog";
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
import type {Action} from "redux";
import type {ServiceAddressBundle} from "../services/Types";

const tableRows = {
  lawFirmAgents: [
    {
      lawFirm: {
        lawFirmId: 58586,
        name: "Law Offices Of Mark A. Wilson, CA",
        websiteUrl: "http://google.com"
      },
      serviceAddresses: [{
        name: "Law Offices Of Mark A. Wilson, CA",
        address: "WILSON, Mark, LawOffices of MarkWilson PMB: 348 2530 Berryessa Rd San Jose,CA 95132",
        serviceAddressId: "1036990"
      }, {
        name: "Law Offices Of Mark A. Wilson, CA",
        address: "Mark Wilson PMB: 348 2530 BerryessaRoad SanJose, California 95132",
        serviceAddressId: "1036991"
      }]
    },
    {
      nonLawFirm: {
        name: "NEL R. FONTANILLA"
      },
      serviceAddresses: [{
        name: "NEL R. FONTANILLA",
        address: "1536 HEMMINGWAY ROAD, SAN JOSE CA 95132",
        serviceAddressId: "1051425"
      }]
    },
    {
      nonLawFirm: {
        name: "INTERACTIV CORPORATION"
      },
      serviceAddresses: [{
        name: "INTERACTIV CORPORATION",
        address: "1659 N CAPITOL AVE # 225, 1659 N CAPITOL AVE # 225, SAN JOSE,CA 95132",
        serviceAddressId: "563472"
      }]
    },
    {
      nonLawFirm: {
        name: "BEAUTYQQ INC"
      },
      serviceAddresses: [{
        name: "BEAUTYQQ INC",
        address: "2928 LAMBETH COURT, SAN JOSE, CA 95132",
        serviceAddressId: "584214"
      }]
    },
    {
      lawFirm: {
        lawFirmId: 44161,
        name: "PEREZ, YVONNE, CA",
      },
      serviceAddresses: [{
        name: "PEREZ, YVONNE, CA",
        address: "PEREZ, YVONNE, 2121 LIMEWOOD DR., SANJOSE, CA95132",
        serviceAddressId: "610070"
      }]
    },
  ]
}

const firm = {
  serviceAddressToSort: {
    name: "WILSON, Mark",
    lawFirmId: 44161,
    address: "Law Offices of Mark Wilson PMB: 348 2530 Berryessa Road San Jose, CA 95132",
    phone: "",
    country: "US",
    serviceAddressId: "1326958",
    languageType: "en"
  },
  enTranslation: "Law Offices of Mark Wilson PMB: 348 2530 Berryessa Road San Jose, CA 95132",
  suggestedAgents: tableRows.lawFirmAgents
}

export const fetchNextUnsortedServiceAddress = (action$: ActionsObservable<Action>): ActionsObservable<Action> =>
  action$.ofType(FETCH_NEXT_UNSORTED_SERVICE_ADDRESS)
    .delay(1000)
    .mapTo(unsortedServiceAddressFulfilled(firm));

export const preFetchNextUnsortedServiceAddress = (action$: ActionsObservable<Action>): ActionsObservable<Action> =>
  action$.ofType(PRE_FETCH_NEXT_UNSORTED_SERVICE_ADDRESS)
    .delay(1000)
    .mapTo(unsortedServiceAddressPreFetched(firm));

export const skipServiceAddress = (action$: ActionsObservable<Action>): ActionsObservable<Action> =>
  action$.ofType(SKIP_SERVICE_ADDRESS)
    .delay(1000)
    .mapTo(serviceAddressSkipped());

export const searchLawFirm = (action$: ActionsObservable<Action>): ActionsObservable<Action> =>
  action$.ofType(SEARCH_LAW_FIRMS)
    .delay(1000)
    .mapTo(searchQueryFulfilled(tableRows));

export const assignServiceAddress = (action$: ActionsObservable<Action>): ActionsObservable<Action> =>
  action$.ofType(ASSIGN_SERVICE_ADDRESS)
    .delay(1000)
    .mapTo(getNextUnsortedServiceAddress());

export const unsortServiceAddress = (action$: ActionsObservable<Action>): ActionsObservable<Action> =>
  action$.ofType(UNSORT_SERVICE_ADDRESS)
    .delay(1000)
    .mapTo(serviceAddressUnsorted());

export const undoServiceAddress = (action$: ActionsObservable<Action>): ActionsObservable<Action> =>
  action$.ofType(UNDO_SERVICE_ADDRESS)
    .delay(1000)
    .mapTo(undoServiceAddressSuccess());

export const createLawFirm = (action$: ActionsObservable<Action>): ActionsObservable<Action> =>
  action$.ofType(CREATE_LAW_FIRM)
    .delay(1000)
    .mapTo(firmCreationError());

export const setServiceAddressAsNonLawFirm = (action$: ActionsObservable<Action>): ActionsObservable<Action> =>
  action$.ofType(SET_SERVICE_ADDRESS_AS_NON_LAW_FIRM)
    .delay(1000)
    .mapTo(getNextUnsortedServiceAddress());

export const setInsufficientInfoStatus = (action$: ActionsObservable<Action>): ActionsObservable<Action> =>
  action$.ofType(SET_INSUFFICIENT_INFO_STATUS)
    .delay(1000)
    .map(getNextUnsortedServiceAddress());

export const getCurrentUser = (acitons$: ActionsObservable<Action>): ActionsObservable<Action> =>
  acitons$.ofType(GET_CURRENT_USER)
    .delay(1000)
    .map(() => {
      Authentication.user = "test_user";
      return getNextUnsortedServiceAddress();
    });