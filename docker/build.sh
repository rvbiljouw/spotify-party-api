#!/bin/bash
set -e;

ENV_FILE=$1
if [ -n "$ENV_FILE" ]; then
    source $ENV_FILE
fi

rm -rf tmp/

cd ../

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

mvn package

cd docker

mkdir -p tmp/lib

cp -R ../target/lib/* tmp/lib/
cp "../target/${POM_PROJECT}-${POM_VERSION}.jar" tmp/

docker build -t "${GCP_REGISTRY}/${GCP_PROJECT_ID}/${POM_PROJECT}:${POM_VERSION}" \
    --build-arg project=${POM_PROJECT} \
    --build-arg version=${POM_VERSION} .

gcloud docker -- push "${GCP_REGISTRY}/${GCP_PROJECT_ID}/${POM_PROJECT}:${POM_VERSION}"