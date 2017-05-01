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
    firm: {}
  }
});

export const bingServiceAddress = (address: Object): Action => ({
  type: Actions.BIND_SERVICE_ADDRESS,
  payload: {
    firm: {
      address: address.serviceAddress,
      serviceAddressId: address.serviceAddressId
    }
  }
})