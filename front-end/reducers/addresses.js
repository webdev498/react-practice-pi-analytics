// Copyright (c) 2017 Practice Insight Pty Ltd. All rights reserved.
//jshint esversion:6
//@flow
import createReducer, {Action} from "redux-updeep";
import * as Actions from "../actions/AddressesActions";

const address = "Law Offices of Mark Wilson PMB: 348 2530 Berryessa Road San Jose, CA 95132";

const tableRows = [
  {
    isLawFirm: true,
    lawFirmId: 58586,
    entity: "Law Offices Of Mark A. Wilson, CA",
    serviceAddress: "WILSON, Mark, LawOffices of MarkWilson PMB: 348 2530 Berryessa Rd San Jose,CA 95132 Mark WilsonPMB: 348 2530 BerryessaRoad SanJose, California 95132",
    website: "http://google.com",
    serviceAddressId: "1036990"
  },
  {
    isLawFirm: false,
    lawFirmId: null,
    entity: "NEL R. FONTANILLA",
    serviceAddress: "1536 HEMMINGWAY ROAD, SAN JOSE CA 95132",
    website: "",
    serviceAddressId: "1051425"
  },
  {
    isLawFirm: false,
    lawFirmId: null,
    entity: "INTERACTIV CORPORATION",
    serviceAddress: "1659 N CAPITOL AVE # 225, 1659 N CAPITOL AVE # 225, SAN JOSE,CA 95132",
    website: "",
    serviceAddressId: "563472"
  },
  {
    isLawFirm: false,
    lawFirmId: null,
    entity: "BEAUTYQQ INC",
    serviceAddress: "2928 LAMBETH COURT, SAN JOSE, CA 95132",
    website: "",
    serviceAddressId: "584214"
  },
  {
    isLawFirm: true,
    lawFirmId: 44161,
    entity: "PEREZ, YVONNE, CA",
    serviceAddress: "PEREZ, YVONNE, 2121 LIMEWOOD DR., SANJOSE, CA95132",
    website: "",
    serviceAddressId: "610070"
  },
]

const initialState = {
  tab: 0,
  firmAddress: address,
  items: tableRows
}

const addresses = createReducer(Actions.NAMESPACE, initialState)
export default addresses;

export const changeTab = (tab: number): Action => ({
  type: Actions.CHANGE_TAB,
  payload: {
    tab: tab
  }
});