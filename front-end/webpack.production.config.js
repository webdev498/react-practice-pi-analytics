const path = require("path")
const webpack = require("webpack")

module.exports = {
  entry: [
    "./index",
  ],
  output: {
    path: path.join(__dirname, "dist"),
    filename: "bundle.js",
    publicPath: "/la/dist/",
  },
  plugins: [
    new webpack.DefinePlugin({
      'process.env.NODE_ENV': JSON.stringify('production')
    }),
    // new webpack.optimize.CommonsChunkPlugin('common.js'),
    new webpack.optimize.DedupePlugin(),
    new webpack.optimize.UglifyJsPlugin(),
    new webpack.optimize.AggressiveMergingPlugin(),
    new webpack.optimize.OccurrenceOrderPlugin(),
  ],
  module: {
    rules: [
      {
        test: /\.js$/,
        exclude: /node_modules/,
        include: __dirname,
        loader: "babel-loader",
      },
      {
        test: /\.s?css$/,
        use: [
          {
            loader: "style",
          },
          {
            loader: "css",
          },
          {
            loader: "postcss",
          },
          {
            loader: "resolve-url",
          },
          {
            loader: "sass",
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
