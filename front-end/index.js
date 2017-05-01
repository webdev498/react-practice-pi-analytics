// Copyright (c) 2017 Practice Insight Pty Ltd. All rights reserved.
// @flow
import "babel-polyfill";
import React from "react";
import WebFont from "webfontloader";
import {render} from "react-dom";
import RootContainer from "./containers/RootContainer";
import {createStore, Store} from "redux";
import rootReducer from "./reducers";
import {Provider} from "react-redux";

WebFont.load({
               google: {
                 families: [
                   "Material Icons",
                   "Open+Sans:300,400,700",
                 ],
               },
             })

const configureStore = (): Store => {
  return createStore(rootReducer)
}

render(
  <Provider store={configureStore()}>
    <RootContainer />
  </Provider>,
  document.getElementById("root")
)
