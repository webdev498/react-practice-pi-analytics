// Copyright (c) 2017 Practice Insight Pty Ltd. All rights reserved.
//jshint esversion:6
//@flow
import createReducer, {Action} from "redux-updeep";
import * as Actions from "../actions/SearchActions";

const initialState = {
  query: "",
  loading: false
}

const search = createReducer(Actions.NAMESPACE, initialState)
export default search;

export const doSearch = (query: string): Action => ({
  type: Actions.DO_SEARCH,
  payload: {
    query: query,
    loading: query.length > 0
  }
});