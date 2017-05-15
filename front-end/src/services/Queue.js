// Copyright (c) 2017 Practice Insight Pty Ltd. All rights reserved.
//jshint esversion:6
//@flow
import {store} from "../index";
import * as FetchActions from "../actions/FetchActions";
import type {ServiceAddressBundle} from "./Types";

const queue = [];

export const canPush = (): boolean => queue.length < 4;

export const push = (bundle: ServiceAddressBundle) => {
  queue.push(bundle);
}

export const pop = (): ServiceAddressBundle => {
  var result = queue.pop();
  if (canPush()) {
    store.dispatch({type: FetchActions.PRE_FETCH_NEXT_UNSORTED_SERVICE_ADDRESS});
  }
  return result;
}