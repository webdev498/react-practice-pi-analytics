// Copyright (c) 2017 Practice Insight Pty Ltd. All rights reserved.

const express = require("express");
const app = new (require("express"))()
const port = 3000

if (process.env.NODE_ENV !== 'production') {
  const webpack = require("webpack")
  const webpackDevMiddleware = require("webpack-dev-middleware")
  const webpackHotMiddleware = require("webpack-hot-middleware")
  const config = require("./webpack.config")
  const compiler = webpack(config)
  app.use(webpackDevMiddleware(compiler, {noInfo: true, publicPath: config.output.publicPath}))
  app.use(webpackHotMiddleware(compiler))
} else {
  app.use("/int/addressing", express.static(__dirname + "/dist"))
}

app.get("/int/addressing", (req, res) => {
  res.sendFile(__dirname + "/index.html")
})

app.listen(port, (error) => {
  if (error) {
    console.error(error)
  } else {
    console.info("==> Listening on port %s.", port)
  }
})

