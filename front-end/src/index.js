// Copyright (c) 2017 Practice Insight Pty Ltd. All rights reserved.
// @flow
//jshint esversion:6
import "babel-polyfill";
import React from "react";
import WebFont from "webfontloader";
import {render} from "react-dom";
import RootContainer from "./containers/RootContainer";
import {applyMiddleware, compose, createStore} from "redux";
import {createEpicMiddleware} from "redux-observable";
import rootReducer from "./reducers";
import {Provider} from "react-redux";
import rootEpic from "./epics";
import thunk from "redux-thunk";

WebFont.load({
               google: {
                 families: [
                   "Material Icons",
                   "Open+Sans:300,400,700",
                 ],
               },
             })

export const store = createStore(rootReducer, applyMiddleware(thunk, createEpicMiddleware(rootEpic)));

render(
  <Provider store={store}>
    <RootContainer />
  </Provider>,
  document.getElementById("root")
)
