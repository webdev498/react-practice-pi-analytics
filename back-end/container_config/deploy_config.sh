#!/usr/bin/env bash

module_name=service-address-admin-backend

# artifact_name -> the name of the distribution artifact (with the .tar suffix)
artifact_name=service-address-admin-backend-latest.tar

# deployment_name -> the name of the `deployment` resource in k8s
deployment_name=service-address-admin-backend

# image_name -> a string to unique identify the image name within the container pod definitions
image_name=service-address-admin-backend
