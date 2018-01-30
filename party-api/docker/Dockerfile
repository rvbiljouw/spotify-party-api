FROM openjdk:8-jre-alpine

ARG project
ENV project $project

ARG version
ENV version $version

RUN echo $project

COPY tmp/lib/ /lib/

COPY tmp/$project-$version.jar /

ENTRYPOINT  ["sh", "-c", "java  -Xmx384M -jar /${project}-${version}.jar"]