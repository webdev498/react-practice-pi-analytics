// Copyright (c) 2017 Practice Insight Pty Ltd. All rights reserved.
//jshint esversion:6
//@flow
import Root from "../components/Root";
import {closeFirmDialog, createFirm} from "../reducers/root";
import {Dispatch, State} from "redux";
import {connect} from "react-redux";

const mapStateToProps = (state: State) => ({
  firm: state.root.firm,
  isCreateFirmDialogOpen: state.root.isCreateFirmDialogOpen,
  loading: state.root.loading
})

const mapDispatchToProps = (dispatch: Dispatch) => ({
  onCreateFirm: () => {
    dispatch(createFirm());
  },
  onCloseFirmDialog: () => {
    dispatch(closeFirmDialog());
  }
})

const RootContainer = connect(mapStateToProps, mapDispatchToProps)(Root)
export default RootContainer
