#!/usr/bin/env bash
set -euo pipefail

echo "Starting GKE deployment..."

: "${WORKSPACE:?WORKSPACE is missing}"
: "${GCP_KEY:?GCP_KEY is missing}"
: "${BUILD_NUMBER:?BUILD_NUMBER is missing}"
: "${GCP_PROJECT:?GCP_PROJECT is missing}"
: "${GKE_CLUSTER:?GKE_CLUSTER is missing}"
: "${GKE_ZONE:?GKE_ZONE is missing}"
: "${IMAGE:?IMAGE is missing}"
: "${TAG:?TAG is missing}"

WORKSPACE_WSL="$WORKSPACE"
KEY_WSL="$GCP_KEY"

if [[ ! -f "$KEY_WSL" ]]; then
    echo "ERROR: GCP service account key file not found."
    exit 1
fi

export CLOUDSDK_CONFIG="/tmp/gcloud-jenkins-${BUILD_NUMBER}"
export KUBECONFIG="${CLOUDSDK_CONFIG}/kubeconfig"

mkdir -p "$CLOUDSDK_CONFIG"

cleanup() {
    rm -rf "$CLOUDSDK_CONFIG"
}
trap cleanup EXIT

cd "$WORKSPACE_WSL"

echo "Authenticating with Google Cloud..."

gcloud auth activate-service-account \
    --key-file="$KEY_WSL" \
    --project="$GCP_PROJECT"

echo "Getting GKE cluster credentials..."

gcloud container clusters get-credentials "$GKE_CLUSTER" \
    --zone="$GKE_ZONE" \
    --project="$GCP_PROJECT" \
    --dns-endpoint
echo "Running Ansible deployment..."

ANSIBLE_CONFIG=ansible/ansible.cfg \
ansible-playbook ansible/deploy.yml \
    -e "image_tag=${IMAGE}:${TAG}"

echo "GKE deployment completed successfully."

