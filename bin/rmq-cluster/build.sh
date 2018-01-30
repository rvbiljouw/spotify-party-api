#!/bin/bash

set -e;
TAG=$1

GCP_PROJECT_ID=awsumio-191915
GCP_REGISTRY=gcr.io

cd docker/

docker build -t "${GCP_REGISTRY}/${GCP_PROJECT_ID}/rmq:${TAG}" .

gcloud docker -- push "${GCP_REGISTRY}/${GCP_PROJECT_ID}/rmq:${TAG}"