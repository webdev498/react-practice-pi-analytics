echo off

rmdir /s /q from-proto 
mkdir from-proto\tmp
xcopy ..\..\pi-shared-protobuf\src from-proto\tmp\ /y /e

cd from-proto\tmp\

protoc --js_out=import_style=commonjs:..\ .\google\protobuf\descriptor.proto .\google\api\annotations.proto .\google\api\http.proto .\patent_common.proto .\service_address_sorting.proto

cd ..\..\
rmdir /s /q from-proto\tmp\
