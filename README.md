# Service Address Administration Web Application

## Initialise Project and Run Locally

```
git submodule update --init --recursive
cd front-end
npm install
npm start
```

You can now browse to http://localhost:3000/int/addressing/?username=youruser

## Type Checking

```
brew install flow
cd front-end
flow check
```

## Updating Protobuf Definitions

```
git submodule update --remote
```

