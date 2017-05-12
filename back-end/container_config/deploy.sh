#!/bin/bash
set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# E.g. `onprem-prod` `sg3-prod`, `pi-prod` `pi-staging`, `dev`
deployment_type=$1

# optionally set version of image tag to use (use latest to always deploy)
build_number=${2:-latest}

if ! type "pi_k8s_deploy.sh" > /dev/null; then
  echo "ensure pi-containers/bin is on your PATH (command not found)"
  exit 1
fi

echo "Deploying to Kubernetes namespace ${deployment_type} "

pi_k8s_deploy.sh ${deployment_type} ${build_number} ${SCRIPT_DIR}