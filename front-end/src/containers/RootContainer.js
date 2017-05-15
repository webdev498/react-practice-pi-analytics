// Copyright (c) 2017 Practice Insight Pty Ltd. All rights reserved.
//jshint esversion:6
//@flow
import Root from "../components/Root";
import {
  createFirm,
  dismissServiceAddress,
  getNextUnsortedServiceAddress,
  skipServiceAddress,
  undoServiceAddress
} from "../reducers/root";
import * as FetchActions from "../actions/FetchActions";
import {Dispatch, State} from "redux";
import {connect} from "react-redux";
import Authentication from "../services/Authentication";

const mapStateToProps = (state: State) => ({
  value: state.root.value,
  isCreateFirmDialogOpen: state.root.isCreateFirmDialogOpen,
  loading: state.root.loading,
  undo: state.root.undo
})

const mapDispatchToProps = (dispatch: Dispatch) => ({
  onCreateFirm: () => {
    dispatch(createFirm());
  },
  onSkip: (queueId: string) => {
    dispatch(skipServiceAddress(queueId));
  },
  onGetNextServiceAddress: () => {
    dispatch(!Authentication.user ? {type: FetchActions.GET_CURRENT_USER} : getNextUnsortedServiceAddress());
  },
  onDismiss: (queueId: string, serviceAddressId: string) => {
    dispatch(dismissServiceAddress(queueId, serviceAddressId));
  },
  onUndo: (serviceAddressId: string) => {
    dispatch(undoServiceAddress(serviceAddressId));
  }
})

const RootContainer = connect(mapStateToProps, mapDispatchToProps)(Root)
export default RootContainer