FROM store/oracle/serverjre:8

ARG project
ENV project $project

RUN yum update -y \
  && yum install -y ca-certificates \
  && curl http://curl.haxx.se/ca/cacert.pem -o /etc/pki/tls/certs/ca-bundle.crt

ARG version
ENV version $version

RUN echo $project

COPY tmp/lib/ /lib/

COPY tmp/$project-$version.jar /

ENTRYPOINT  ["sh", "-c", "java -XX:+UnlockExperimentalVMOptions -jar /${project}-${version}.jar"]