// Copyright (c) 2017 Practice Insight Pty Ltd. All rights reserved.
//jshint esversion:6
//@flow
import Root from "../components/Root";
import {
  createFirm,
  dismissServiceAddress,
  getNextUnsortedServiceAddress,
  undoServiceAddress,
  toggleApplicationsPanel,
  hideMessage,
  skipServiceAddress
} from "../reducers/root";
import * as FetchActions from "../actions/FetchActions";
import type {Dispatch, State} from "redux";
import {connect} from "react-redux";
import Authentication from "../services/Authentication";

const mapStateToProps = (state: State) => ({
  value: state.root.value,
  isCreateFirmDialogOpen: state.root.isCreateFirmDialogOpen,
  loading: state.root.loading,
  undo: state.root.undo,
  message: state.root.message,
  applicationsPanelOpen: state.root.applicationsPanelOpen,
  skipping: state.root.skipping
})

const mapDispatchToProps = (dispatch: Dispatch) => ({
  onCreateFirm: () => {
    dispatch(createFirm());
  },
  onSkip: (serviceAddressId: string) => {
    dispatch(skipServiceAddress(serviceAddressId));
  },
  onGetNextServiceAddress: () => {
    dispatch(!Authentication.user ? {type: FetchActions.GET_CURRENT_USER} : getNextUnsortedServiceAddress());
  },
  onSetAsNonLawFirm: (serviceAddressId: string) => {
    dispatch(dismissServiceAddress(serviceAddressId, FetchActions.SET_SERVICE_ADDRESS_AS_NON_LAW_FIRM));
  },
  onSetSortingImpossible: (serviceAddressId: string) => {
    dispatch(dismissServiceAddress(serviceAddressId, FetchActions.SET_SORTING_IMPOSSIBLE));
  },
  onUndo: (serviceAddressId: string) => {
    dispatch(undoServiceAddress(serviceAddressId));
  },
  onHideSnackbar: () => {
    dispatch(hideMessage());
  },
  onToggleApplicationsPanel: (previous: boolean) => {
    dispatch(toggleApplicationsPanel(previous));
  }
})

const RootContainer = connect(mapStateToProps, mapDispatchToProps)(Root)
export default RootContainer
