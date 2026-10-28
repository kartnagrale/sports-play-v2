const path = require('node:path');
const webpack = require('next/dist/compiled/webpack/webpack-lib');
webpack({
  mode: 'production', target: 'web', entry: path.join(__dirname, 'ui-harness.tsx'),
  optimization: { minimize: false },
  output: { path: path.resolve(__dirname, '../../tmp'), filename: 'phase2-ui-harness.js' },
  resolve: { extensions: ['.tsx','.ts','.js'], alias: { '@': path.resolve(__dirname,'../src') } },
  module: { rules: [{ test: /\.tsx?$/, exclude: /node_modules/, use: path.join(__dirname,'ts-loader.cjs') }] },
  plugins: [new webpack.ProvidePlugin({ process: 'next/dist/build/polyfills/process' }), new webpack.DefinePlugin({ 'process.env.__NEXT_IMAGE_OPTS': JSON.stringify(require('next/dist/shared/lib/image-config').imageConfigDefault) })],
}, (error, stats) => { if (error || stats.hasErrors()) { console.error(error || stats.toString({ all:false, errors:true })); process.exitCode=1; } });
