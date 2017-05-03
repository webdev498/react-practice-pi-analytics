// Copyright (c) 2017 Practice Insight Pty Ltd. All rights reserved.
//jshint esversion:6
//@flow
import createReducer, {Action} from "redux-updeep";
import * as Actions from "../actions/RootActions";
import * as FetchActions from "../actions/FetchActions";

const initialState = {
  loading: true
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
});

export const unsortedServiceAddressFulfilled = (address: Object): Action => ({
  type: Actions.UNSORTED_SERVICE_ADDRESS_FULFILLED,
  payload: {
    firm: address,
    loading: false
  }
});

export const unsortedServiceAddressFetchError = (): Action => ({
  type: Actions.UNSORTED_SERVICE_ADDRESS_FETCH_ERROR,
  payload: {
    loading: false
  }
});

export const getNextUnsortedServiceAddress = (): Action => ({
  type: FetchActions.FETCH_NEXT_UNSORTED_SERVICE_ADDRESS,
  payload: {
    request: {},
    loading: true
  }
});

export const serviceAddressAssigned = (): Action => ({
  type: Actions.SERVICE_ADDRESS_ASSIGNED,
  payload: {
    mask: false
  }
});

export const serviceAddressUnsorted = (): Action => ({
  type: Actions.SERVICE_ADDRESS_UNSORTED,
  payload: {
    mask: false
  }
});