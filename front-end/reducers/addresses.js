// Copyright (c) 2017 Practice Insight Pty Ltd. All rights reserved.
//jshint esversion:6
//@flow
import createReducer, {Action} from "redux-updeep";
import * as Actions from "../actions/AddressesActions";

const initialState = {
  tab: 0
}

const addresses = createReducer(Actions.NAMESPACE, initialState)
export default addresses;

export const changeTab = (tab: number): Action => ({
  type: Actions.CHANGE_TAB,
  payload: {
    tab: tab
  }
});