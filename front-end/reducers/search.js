// Copyright (c) 2017 Practice Insight Pty Ltd. All rights reserved.
//jshint esversion:6
//@flow
import createReducer, {Action} from "redux-updeep";
import * as Actions from "../actions/SearchActions";
import * as FetchActions from "../actions/FetchActions";

const initialState = {}

const search = createReducer(Actions.NAMESPACE, initialState)
export default search;

export const stopSearch = (): Action => ({
  type: Actions.STOP_SEARCH,
  payload: {
    query: undefined,
    loading: false
  }
})

export const searchQueryFulfilled = (items: Array<Object>): Action => ({
  type: Actions.SEARCH_QUERY_FULFILLED,
  payload: {
    loading: false,
    agents: items
  }
});

export const searchQueryError = (): Action => ({
  type: Actions.SEARCH_QUERY_ERROR,
  payload: {
    loading: false,
    agents: undefined
  }
});

export const doSearch = (query: string): Action => (dispatch: Dispatch) => {
  dispatch({type: Actions.START_SEARCH, payload: {query: query, loading: true}});
  dispatch({type: FetchActions.SEARCH_LAW_FIRMS, payload: {request: {searchTerm: query}}});
};