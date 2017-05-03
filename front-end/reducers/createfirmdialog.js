// Copyright (c) 2017 Practice Insight Pty Ltd. All rights reserved.
//jshint esversion:6
//@flow
import createReducer, {Action} from "redux-updeep";
import * as FetchActions from "../actions/FetchActions";
import * as Actions from "../actions/CreateFirmDialogActions";

const initialState = {}

const createfirmdialog = createReducer(Actions.NAMESPACE, initialState)
export default createfirmdialog;

export const submitNewFirm = (firm: Object): Action => ({
  type: FetchActions.CREATE_LAW_FIRM,
  payload: {
    request: firm,
    loading: true
  }
});

export const lawFirmCreated = (firm: Object): Action => ({
  type: Actions.FIRM_CREATED,
  payload: {
    open: false,
    loading: false
  }
})

export const firmCreationError = (): Action => ({
  type: Actions.FIRM_CREATION_ERROR,
  payload: {
    loading: false
  }
})
