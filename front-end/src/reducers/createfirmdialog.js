// Copyright (c) 2017 Practice Insight Pty Ltd. All rights reserved.
//jshint esversion:6
//@flow
import createReducer from "redux-updeep"
import type {Action, Dispatch} from "redux"
import * as FetchActions from "../actions/FetchActions"
import * as Actions from "../actions/CreateFirmDialogActions"
import {closeFirmDialog, showError, getNextUnsortedServiceAddress} from "../reducers/root"

const initialState = {}

const createfirmdialog = createReducer(Actions.NAMESPACE, initialState)
export default createfirmdialog

export const submitNewFirm = (firm: Object): Action => (dispatch: Dispatch) => {
  dispatch({type: Actions.START_FETCH, payload: {loading: true}})
  dispatch({type: FetchActions.CREATE_LAW_FIRM, payload: {request: firm}})
};

export const lawFirmCreated = (firm: Object): Action => (dispatch: Dispatch) => {
  dispatch({type: Actions.FIRM_CREATED, payload: {loading: false}})
  dispatch(closeFirmDialog(true))
  dispatch(getNextUnsortedServiceAddress())
}

export const firmCreationError = (): Action => (dispatch: Dispatch) => {
  dispatch({type: Actions.FIRM_CREATION_ERROR, payload: {loading: false}})
  dispatch(
    showError("Unable to create Law Firm. Ensure required info is provided and that the law firm doesn't already exist.")
  )
}
