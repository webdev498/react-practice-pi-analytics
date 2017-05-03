// Copyright (c) 2017 Practice Insight Pty Ltd. All rights reserved.
//jshint esversion:6
//@flow
import Root from "../components/Root";
import {createFirm, getNextUnsortedServiceAddress} from "../reducers/root";
import * as FetchActions from "../actions/FetchActions";
import {Dispatch, State} from "redux";
import {connect} from "react-redux";
import Authentication from "../services/Authentication";

const mapStateToProps = (state: State) => ({
  firm: state.root.firm,
  isCreateFirmDialogOpen: state.root.isCreateFirmDialogOpen,
  loading: state.root.loading
})

const mapDispatchToProps = (dispatch: Dispatch) => ({
  onCreateFirm: () => {
    dispatch(createFirm());
  },
  onSkip: () => {
    dispatch(getNextUnsortedServiceAddress());
  },
  onGetNextServiceAddress: () => {
    dispatch(!Authentication.user ? {type: FetchActions.GET_CURRENT_USER} : getNextUnsortedServiceAddress());
  },
  onDismiss: () => {
    dispatch({type: FetchActions.SET_SERVICE_ADDRESS_AS_NON_LAW_FIRM, payload: {loading: true}});
  }
})

const RootContainer = connect(mapStateToProps, mapDispatchToProps)(Root)
export default RootContainer
