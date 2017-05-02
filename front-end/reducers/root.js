// Copyright (c) 2017 Practice Insight Pty Ltd. All rights reserved.
//jshint esversion:6
//@flow
import createReducer, {Action} from "redux-updeep";
import * as Actions from "../actions/RootActions";

const firm = {
  entity: "WILSON, Mark",
  address: "Law Offices of Mark Wilson PMB: 348 2530 Berryessa Road San Jose, CA 95132",
  phone: "",
  country: "US",
  serviceAddressId: "1326958"
}

const initialState = {
  firm: firm
}

const root = createReducer(Actions.NAMESPACE, initialState)
export default root;

export const createFirm = (): Action => ({
  type: Actions.CREATE_FIRM,
  payload: {
    isCreateFirmDialogOpen: true
  }
});

export const closeFirmDialog = (): Action => ({
  type: Actions.CLOSE_FIRM_DIALOG,
  payload: {
    isCreateFirmDialogOpen: false
  }
})

export const sortServiceAddress = (address: Object): Action => ({
  type: Actions.BIND_SERVICE_ADDRESS,
  payload: {
    firm: {
      address: address.serviceAddress,
      serviceAddressId: address.serviceAddressId
    }
  }
})

export const unsortedServiceAddressFulfilled = (address: Object): Action => ({
  type: Actions.UNSORTED_SERVICE_ADDRESS_FULFILLED,
  payload: {
    firm: address,
    loading: false
  }
})

export const unsortedServiceAddressFetchError = (): Action => ({
  type: Actions.UNSORTED_SERVICE_ADDRESS_FETCH_ERROR,
  payload: {
    loading: false
  }
})