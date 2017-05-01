// Copyright (c) 2017 Practice Insight Pty Ltd. All rights reserved.
//jshint esversion:6
//@flow
import Search from "../components/Search";
import {doSearch} from "../reducers/search";
import {Dispatch, State} from "redux";
import {connect} from "react-redux";
import {bingServiceAddress} from "../reducers/root";

const mapStateToProps = (state: State) => ({
  items: state.search.items,
  query: state.search.query,
  loading: state.search.loading
})

const mapDispatchToProps = (dispatch: Dispatch) => ({
  onSearch: (event: Event) => {
    event.preventDefault();
    dispatch(doSearch(event.target.value));
  },
  onBindServiceAddress: (address: Object) => {
    dispatch(bingServiceAddress(address));
  }
})

const SearchContainer = connect(mapStateToProps, mapDispatchToProps)(Search)
export default SearchContainer
