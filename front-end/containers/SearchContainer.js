// Copyright (c) 2017 Practice Insight Pty Ltd. All rights reserved.
//jshint esversion:6
//@flow
import Search from "../components/Search";
import {doSearch, stopSearch} from "../reducers/search";
import {Dispatch, State} from "redux";
import {connect} from "react-redux";
import {sortServiceAddress, unsortServiceAddress} from "../reducers/root";

const mapStateToProps = (state: State) => ({
  agents: state.search.agents,
  query: state.search.query,
  loading: state.search.loading,
  serviceAddress: state.root.value.serviceAddressToSort
})

const mapDispatchToProps = (dispatch: Dispatch) => ({
  onSearch: (query: string) => {
    if (query && query.length > 0) {
      dispatch(doSearch(query));
    } else {
      dispatch(stopSearch());
    }
  },
  onSortServiceAddress: (serviceAddressId: string, address: Object) => {
    dispatch(sortServiceAddress(serviceAddressId, address));
  },
  onUnsortServiceAddress: (serviceAddressId: string) => {
    dispatch(unsortServiceAddress(serviceAddressId));
  }
})

const SearchContainer = connect(mapStateToProps, mapDispatchToProps)(Search)
export default SearchContainer
