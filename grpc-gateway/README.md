# grpc-gateway

JSON gateway for service address sorting admin backend gRPC service implemented using [grpc-gateway](https://github.com/gengo/grpc-gateway).

To add more backends to the gateway:

  1. Update the [install.sh](install.sh) script to generate stubs for the backend's .proto file
  2. Add the backend to [src/grpc-gateway/main.go](src/grpc-gateway/main.go)
  3. Rebuild the project

## Tooling

You will need Go and protoc to build this project. To install on macOS:

```
brew install golang
brew install protobuf --devel
```

## Building for Docker

To build a Docker image:

```
./build.sh
```

## Installing Locally

To install locally:

```
./install.sh
```

The binary will be available in the bin/ directory.

