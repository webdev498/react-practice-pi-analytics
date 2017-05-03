// Copyright (c) 2017 Practice Insight Pty Ltd. All rights reserved.
//jshint esversion:6
//@flow
import "rxjs/add/operator/map";
import "rxjs/add/operator/mapTo";
import "rxjs/add/operator/delay";
import {
  getNextUnsortedServiceAddress,
  serviceAddressAssigned,
  serviceAddressUnsorted,
  unsortedServiceAddressFulfilled
} from "../reducers/root";
import {searchQueryFulfilled} from "../reducers/search";
import {lawFirmCreated} from "../reducers/createfirmdialog";
import {
  ASSIGN_SERVICE_ADDRESS,
  CREATE_LAW_FIRM,
  FETCH_NEXT_UNSORTED_SERVICE_ADDRESS,
  GET_CURRENT_USER,
  SEARCH_LAW_FIRMS,
  SET_SERVICE_ADDRESS_AS_NON_LAW_FIRM,
  UNSORT_SERVICE_ADDRESS
} from "../actions/FetchActions";
import {ActionsObservable} from "redux-observable";
import Authentication from "../services/Authentication";

const tableRows = [
  {
    lawFirm: {
      lawFirmId: 58586
    },
    serviceAddress: {
      entity: "Law Offices Of Mark A. Wilson, CA",
      address: "WILSON, Mark, LawOffices of MarkWilson PMB: 348 2530 Berryessa Rd San Jose,CA 95132 Mark WilsonPMB: 348 2530 BerryessaRoad SanJose, California 95132",
      website: "http://google.com",
      serviceAddressId: "1036990"
    }
  },
  {
    nonLawFirm: {
      name: "NEL R. FONTANILLA"
    },
    serviceAddress: {
      entity: "NEL R. FONTANILLA",
      address: "1536 HEMMINGWAY ROAD, SAN JOSE CA 95132",
      website: "",
      serviceAddressId: "1051425"
    }
  },
  {
    nonLawFirm: {
      name: "INTERACTIV CORPORATION"
    },
    serviceAddress: {
      entity: "INTERACTIV CORPORATION",
      address: "1659 N CAPITOL AVE # 225, 1659 N CAPITOL AVE # 225, SAN JOSE,CA 95132",
      website: "",
      serviceAddressId: "563472"
    }
  },
  {
    nonLawFirm: {
      name: "BEAUTYQQ INC"
    },
    serviceAddress: {
      entity: "BEAUTYQQ INC",
      address: "2928 LAMBETH COURT, SAN JOSE, CA 95132",
      website: "",
      serviceAddressId: "584214"
    }
  },
  {
    lawFirm: {
      lawFirmId: 44161
    },
    serviceAddress: {
      entity: "PEREZ, YVONNE, CA",
      address: "PEREZ, YVONNE, 2121 LIMEWOOD DR., SANJOSE, CA95132",
      website: "",
      serviceAddressId: "610070"
    }
  },
]

const firm = {
  serviceAddressToSort: {
    entity: "WILSON, Mark",
    address: "Law Offices of Mark Wilson PMB: 348 2530 Berryessa Road San Jose, CA 95132",
    phone: "",
    country: "US",
    serviceAddressId: "1326958",
  },
  enTranslation: "en",
  suggestedAgents: tableRows
}

export const fetchNextUnsortedServiceAddress = (action$: ActionsObservable<Action>): ActionsObservable<Action> =>
  action$.ofType(FETCH_NEXT_UNSORTED_SERVICE_ADDRESS)
    .delay(1000)
    .mapTo(unsortedServiceAddressFulfilled(firm));

export const searchLawFirm = (action$: ActionsObservable<Action>): ActionsObservable<Action> =>
  action$.ofType(SEARCH_LAW_FIRMS)
    .delay(1000)
    .mapTo(searchQueryFulfilled(tableRows));

export const assignServiceAddress = (action$: ActionsObservable<Action>): ActionsObservable<Action> =>
  action$.ofType(ASSIGN_SERVICE_ADDRESS)
    .delay(1000)
    .mapTo(serviceAddressAssigned());

export const unsortServiceAddress = (action$: ActionsObservable<Action>): ActionsObservable<Action> =>
  action$.ofType(UNSORT_SERVICE_ADDRESS)
    .delay(1000)
    .mapTo(serviceAddressUnsorted());

export const createLawFirm = (action$: ActionsObservable<Action>): ActionsObservable<Action> =>
  action$.ofType(CREATE_LAW_FIRM)
    .delay(1000)
    .mapTo(lawFirmCreated({lawFirmId: 1}));

export const setServiceAddressAsNonLawFirm = (action$: ActionsObservable<Action>): ActionsObservable<Action> =>
  action$.ofType(SET_SERVICE_ADDRESS_AS_NON_LAW_FIRM)
    .delay(1000)
    .mapTo(getNextUnsortedServiceAddress())

export const getCurrentUser = (acitons$: ActionsObservable<Action>): ActionsObservable<Action> =>
  acitons$.ofType(GET_CURRENT_USER)
    .delay(1000)
    .map(() => {
      Authentication.user = "test_user";
      return getNextUnsortedServiceAddress();
    });