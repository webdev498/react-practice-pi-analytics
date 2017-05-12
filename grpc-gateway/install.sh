#!/bin/bash
set -e

GOPATH=$(pwd)
echo "GOPATH=$GOPATH"

go get -u -v github.com/grpc-ecosystem/grpc-gateway/protoc-gen-grpc-gateway
go get -u -v github.com/golang/protobuf/protoc-gen-go

function generate_stubs {
    local proto_file=$1
    # gRPC stub
    protoc -I. \
        -I$GOPATH/src \
        -I$GOPATH/src/github.com/grpc-ecosystem/grpc-gateway/third_party/googleapis \
        --go_out=Mscalapb/scalapb.proto=github.com/vyshane/protobuf-common/generated-go/scalapb,plugins=grpc:. \
        $proto_file
    # Reverse proxy
    protoc -I. \
        -I$GOPATH/src \
        -I$GOPATH/src/github.com/grpc-ecosystem/grpc-gateway/third_party/googleapis \
        --grpc-gateway_out=logtostderr=true:. \
        $proto_file
}

pushd src/grpc-gateway/generated/serviceaddresssortingservice/
generate_stubs "*.proto"
popd

pushd src/grpc-gateway/
go get -d -v
popd

pushd src/grpc-gateway/
go install -v
