#!/bin/bash
set -e

# Clear previous run
rm -rf from-proto/
mkdir -p from-proto/tmp/
cp -r ../protobuf/. from-proto/tmp/

cd from-proto/tmp/

protoc --js_out=import_style=commonjs:../ \
    google/protobuf/descriptor.proto \
    google/api/annotations.proto \
    google/api/http.proto \
    patent_common.proto \
    service_address_sorting.proto \

cd ../../
rm -rf from-proto/tmp/
