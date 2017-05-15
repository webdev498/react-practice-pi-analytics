FROM node:7.2
ENV LAST_UPDATED 2016-12-12

# Install protoc. Required by our Webpack build.
ENV PROTOBUF_VERSION 3.0.2
RUN set -x \
    && apt-get -qq update \
    && DEBIAN_FRONTEND=noninteractive apt-get -yq install \
        unzip \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*
RUN wget -O /tmp/protobuf.zip https://github.com/google/protobuf/releases/download/v${PROTOBUF_VERSION}/protoc-${PROTOBUF_VERSION}-linux-x86_64.zip && \
    unzip /tmp/protobuf.zip && \
    mv bin/protoc /usr/bin/

# Workaround for npm install failing (See https://github.com/npm/npm/issues/9863)
RUN cd $(npm root -g)/npm \
    && npm install fs-extra \
    && sed -i -e s/graceful-fs/fs-extra/ -e s/fs.rename/fs.move/ ./lib/utils/rename.js

# Install la-web-frontend
RUN mkdir -p /usr/src/
COPY src/ /usr/src/app/

# Build JavaScript models from protobuf
COPY build-artifacts/protobuf-src/ /usr/src/app/protobuf/
WORKDIR /usr/src/app/generated/
RUN ./build-js-from-proto.sh

WORKDIR /usr/src/app/
RUN npm install --production \
    && npm rebuild node-sass  # Needs to be built specifically for Linux
RUN npm install -g webpack \
    && npm run build

EXPOSE 3000
ENV NODE_ENV production
CMD ["npm", "start"]
