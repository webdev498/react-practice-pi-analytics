// Copyright (c) 2017 Practice Insight Pty Ltd. All rights reserved.
//jshint esversion:6
//@flow
import Root from "../components/Root";
import {createFirm} from "../reducers/root";
import {Dispatch, State} from "redux";
import {connect} from "react-redux";

const mapStateToProps = (state: State) => ({
  firm: state.root.firm
})

const mapDispatchToProps = (dispatch: Dispatch) => ({
  onCreateFirm: () => {
    dispatch(createFirm());
  }
})

const RootContainer = connect(mapStateToProps, mapDispatchToProps)(Root)
export default RootContainer
