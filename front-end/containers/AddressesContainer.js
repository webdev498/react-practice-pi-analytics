// Copyright (c) 2017 Practice Insight Pty Ltd. All rights reserved.
//jshint esversion:6
//@flow
import Addresses from "../components/Addresses";
import {changeTab} from "../reducers/addresses";
import {sortServiceAddress} from "../reducers/root";
import {Dispatch, State} from "redux";
import {connect} from "react-redux";

const mapStateToProps = (state: State) => ({
  tab: state.addresses.tab,
  items: state.root.firm.suggestedAgents,
  firmAddress: state.root.firm.serviceAddressToSort
})

const mapDispatchToProps = (dispatch: Dispatch) => ({
  onTabChange: (tab: number) => {
    dispatch(changeTab(tab));
  },
  onSortServiceAddress: (address: Object) => {
    dispatch(sortServiceAddress(address));
  }
})

const AddressesContainer = connect(mapStateToProps, mapDispatchToProps)(Addresses)
export default AddressesContainer
