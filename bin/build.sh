#!/bin/bash
set -e;

ENV_FILE=$2
if [ -n "$ENV_FILE" ]; then
    source $ENV_FILE
fi

PROJECT_ID=$1

ls ../$PROJECT_ID

if [ $? -ne 0 ]; then
    echo "Couldn't find project directory for $PROJECT_ID"
    exit 253
fi

ls ../$PROJECT_ID/docker

if [ $? -ne 0 ]; then
    echo "Couldn't find Docker directory for $PROJECT_ID"
    exit 253
fi

ls ../$PROJECT_ID/docker/Dockerfile

if [ $? -ne 0 ]; then
    echo "Couldn't find Dockerfile for $PROJECT_ID"
    exit 253
fi

echo "Building project $PROJECT_ID (and dependencies)"

cd ../

mvn package -Dmaven.test.skip=true -pl $PROJECT_ID -am

if [ $? -ne 0 ]; then
    echo "Maven packaging failed"
    exit 253
fi

cd $PROJECT_ID

# Run it once so we're sure the output on the next request matches up
MVN_OUTPUT="$(mvn org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate -Dexpression=project.version)"

# Run it again and export it into the env
export POM_VERSION="$(mvn org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate -Dexpression=project.version | grep -v '\[')"

export POM_PROJECT="$(mvn org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate -Dexpression=project.name | grep -v '\[')"

if [ ${#POM_VERSION} -lt 5 ]; then
    echo "Couldn't retrieve POM version"
    exit 252;
fi

if [ ${#POM_VERSION} -gt 30 ]; then
    echo "Couldn't retrieve POM version"
    exit 252;
fi

cd docker

rm -rf ./tmp

mkdir ./tmp

cp -r ../target/ ./tmp

docker build -t "${GCP_REGISTRY}/${GCP_PROJECT_ID}/${POM_PROJECT}:${POM_VERSION}" \
    --build-arg project=${POM_PROJECT} \
    --build-arg version=${POM_VERSION} .

gcloud docker -- push "${GCP_REGISTRY}/${GCP_PROJECT_ID}/${POM_PROJECT}:${POM_VERSION}"

rm -rf ./tmp

set +x;
