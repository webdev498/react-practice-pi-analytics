// Copyright (c) 2017 Practice Insight Pty Ltd. All rights reserved.

const path = require("path")
const webpack = require("webpack")
const WebpackShellPlugin = require("webpack-shell-plugin")

module.exports = {
  devtool: "eval",
  entry: [
    "webpack-hot-middleware/client",
    "./index",
  ],
  output: {
    path: path.join(__dirname, "dist"),
    filename: "bundle.js",
    publicPath: "/la/dist/",
  },
  plugins: [
    new webpack.DefinePlugin({
      'process.env.NODE_ENV': JSON.stringify('development')
    }),
    new webpack.optimize.OccurrenceOrderPlugin(),
    new webpack.HotModuleReplacementPlugin(),
    new WebpackShellPlugin({ onBuildStart: [
      process.platform == "win32" ? "cd generated && build-js-from-proto" : "cd generated && ./build-js-from-proto.sh"
    ] }),
  ],
  module: {
    rules: [
      {
        test: /\.js$/,
        loader: "babel-loader",
        exclude: /node_modules/,
        include: __dirname,
      },
      {
        test: /\.s?css$/,
        use: [
          {
            loader: "style-loader",
          },
          {
            loader: "css-loader",
            options: {
              importLoaders: 1,
            }
          },
          {
            loader: "resolve-url-loader",
          },
          {
            loader: "sass-loader",
          },
        ],
      },
      {
        test: /\.(jpg|png)$/,
        loader: "file-loader",
      },
    ],
  },
}
