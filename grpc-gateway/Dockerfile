FROM golang:1.8
MAINTAINER shane.xie@practiceinsight.io
ENV PROTOBUF_VERSION 3.2.0

RUN set -x && \
    apt-get -qq update && \
    DEBIAN_FRONTEND=noninteractive apt-get -yq install \
        unzip && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Install protoc
RUN mkdir -p /tmp/protobuf/ && \
    wget -O /tmp/protobuf.zip https://github.com/google/protobuf/releases/download/v${PROTOBUF_VERSION}/protoc-${PROTOBUF_VERSION}-linux-x86_64.zip && \
    unzip /tmp/protobuf.zip -d /tmp/protobuf/ && \
    mv /tmp/protobuf/bin/protoc /usr/bin/

COPY install.sh /go/install.sh
COPY src/grpc-gateway/ /go/src/grpc-gateway/

# Install the proto files that shipped with protoc
RUN mkdir -p /go/src/grpc-gateway/generated/serviceaddresssortingservice/ && \
    cp -r /tmp/protobuf/include/google/. /go/src/grpc-gateway/generated/serviceaddresssortingservice/google/

WORKDIR /go/
RUN ./install.sh
CMD ["grpc-gateway"]
