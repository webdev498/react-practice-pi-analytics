// Copyright (c) 2017 Practice Insight Pty Ltd. All rights reserved.
//jshint esversion:6
//@flow
import CreateLawFirmDialog from "../components/CreateLawFirmDialog";
import {closeFirmDialog} from "../reducers/root";
import {submitNewFirm} from "../reducers/createfirmdialog";
import type {Dispatch, State} from "redux";
import {connect} from "react-redux";

const mapStateToProps = (state: State) => ({
  value: state.root.value,
  open: state.root.isCreateFirmDialogOpen,
  loading: state.createfirmdialog.loading
})

const mapDispatchToProps = (dispatch: Dispatch) => ({
  onSubmit: (firm: Object) => {
    dispatch(submitNewFirm(firm));
  },
  onClose: () => {
    dispatch(closeFirmDialog(false));
  }
})

const CreateLawFirmDialogContainer = connect(mapStateToProps, mapDispatchToProps)(CreateLawFirmDialog)
export default CreateLawFirmDialogContainer;