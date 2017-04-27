# Generated JavaScript Sources

The JavaScript sources in the proto/ directory are generated from our [protobuf definitions](../../../protobuf/README.md). To generate the JavaScript source files, run:

```
./build-js-from-proto.sh
```

The [JavaScript protoc compiler](https://github.com/google/protobuf/tree/master/js) cannot currently generate JavaScript sources that use ES2015 module imports. For now we use CommonJS to import the generated sources. E.g.  

```
const CitationSearchRequest = require("../generated/from-proto/citation_search_pb").CitationSearchRequest
```