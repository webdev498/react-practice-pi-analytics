// Copyright (c) 2017 Practice Insight Pty Ltd. All rights reserved.
//jshint esversion:6
//@flow
import addresses from "./addresses";
import root from "./root";
import search from "./search";
import createfirmdialog from "./createfirmdialog";
import {combineReducers} from "redux";
import type {Reducer} from "redux";

const rootReducer: Reducer<any, any> = combineReducers({
                                                         createfirmdialog,
                                                         addresses,
                                                         search,
                                                         root
                                                       })

export default rootReducer