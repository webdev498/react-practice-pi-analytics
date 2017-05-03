// Copyright (c) 2017 Practice Insight Pty Ltd. All rights reserved.
//jshint esversion:6
//@flow
import createReducer, {Action} from "redux-updeep";
import * as Actions from "../actions/SearchActions";
import * as FetchActions from "../actions/FetchActions";

const initialState = {
  query: ""
}

const search = createReducer(Actions.NAMESPACE, initialState)
export default search;

export const doSearch = (query: string): Action => ({
  type: FetchActions.SEARCH_LAW_FIRMS,
  payload: {
    request: {search_term: query},
    loading: true
  }
});

export const searchQueryFulfilled = (items: Array<Object>): Action => ({
  type: Actions.SEARCH_QUERY_FULFILLED,
  payload: {
    loading: false,
    items: items
  }
});

export const searchQueryError = (): Action => ({
  type: Actions.SEARCH_QUERY_ERROR,
  payload: {
    loading: false
  }
});