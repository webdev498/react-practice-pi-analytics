/**
 * Copyright (c) 2017 Practice Insight Pty Ltd. All rights reserved.
 */
package main

import (
	"flag"
	"net/http"
	"os"
	"strings"

	"github.com/golang/glog"
	"github.com/gorilla/handlers"
	"github.com/grpc-ecosystem/grpc-gateway/runtime"
	"golang.org/x/net/context"
	"google.golang.org/grpc"

	serviceAddressSortingServiceGrpc "grpc-gateway/generated/serviceaddresssortingservice"
)

var (
	// service-address-sorting-service
	publicUserServiceEndpoint = flag.String("user_endpoint",
		os.Getenv("SERVICE_ADDRESS_SORTING_SERVICE_HOST")+":"+os.Getenv("SERVICE_ADDRESS_SORTING_SERVICE_PORT"),
		"Public user Service endpoint")
)

func run() error {
	ctx := context.Background()
	ctx, cancel := context.WithCancel(ctx)
	defer cancel()
	mux := runtime.NewServeMux()
	dialOptions := []grpc.DialOption{grpc.WithInsecure()}

	// service-address-sorting-service
	err := serviceAddressSortingServiceGrpc.RegisterwServiceAddressSortingServiceHandlerFromEndpoint(ctx, mux, *publicUserServiceEndpoint, dialOptions)
	if err != nil {
		return err
	}

	allowedMethods := handlers.AllowedMethods([]string{"OPTIONS", "DELETE", "GET", "HEAD", "POST", "PUT"})
	allowedOrigins := getAllowedOriginsFromConfig("CORS_ALLOWED_ORIGINS")
	allowedHeaders := handlers.AllowedHeaders([]string{"Authorization", "Origin", "Content-Type"})
	http.ListenAndServe(":"+os.Getenv("GATEWAY_PORT"), handlers.CORS(allowedMethods, allowedOrigins, allowedHeaders)(mux))
	return nil
}

func main() {
	flag.Parse()
	defer glog.Flush()

	if err := run(); err != nil {
		glog.Error(err)
	}
}

func getAllowedOriginsFromConfig(env string) handlers.CORSOption {
	configuredOrigins := strings.Split(os.Getenv(env), ",")
	for key, url := range configuredOrigins {
		configuredOrigins[key] = strings.TrimSpace(url)
	}
	return handlers.AllowedOrigins(configuredOrigins)
}
