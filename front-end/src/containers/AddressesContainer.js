// Copyright (c) 2017 Practice Insight Pty Ltd. All rights reserved.
//jshint esversion:6
//@flow
import Addresses from "../components/Addresses";
import {changeTab} from "../reducers/addresses";
import {sortServiceAddress, unsortServiceAddress} from "../reducers/root";
import type {Dispatch, State} from "redux";
import {connect} from "react-redux";

const mapStateToProps = (state: State) => ({
  tab: state.addresses.tab,
  agents: state.root.value.suggestedAgents,
  serviceAddress: state.root.value.serviceAddressToSort,
})

const mapDispatchToProps = (dispatch: Dispatch) => ({
  onTabChange: (tab: number) => {
    dispatch(changeTab(tab));
  },
  onSortServiceAddress: (serviceAddressId: string, address: Object) => {
    dispatch(sortServiceAddress(serviceAddressId, address));
  },
  onUnsortServiceAddress: (serviceAddressId: string) => {
    dispatch(unsortServiceAddress(serviceAddressId));
  }
})

const AddressesContainer = connect(mapStateToProps, mapDispatchToProps)(Addresses)
export default AddressesContainer
