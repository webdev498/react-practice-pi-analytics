// Copyright (c) 2017 Practice Insight Pty Ltd. All rights reserved.
// @flow
import "babel-polyfill"
import React from "react"
import WebFont from "webfontloader"
import { render } from "react-dom"

WebFont.load({
  google: {
    families: [
      "Material Icons",
      "Open+Sans:300,400,700",
    ],
  },
})

render(
  <h1>Hello, World!</h1>,
  document.getElementById("root")
)
