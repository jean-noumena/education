FROM openjdk:11-jre-slim

ARG BUILD_DATE
ARG GIT_REV
ARG VERSION

RUN apt-get update \
    && apt-get install -y curl \
    && apt-get clean

WORKDIR /api

USER nobody

LABEL org.label-schema.name="api" \
      org.label-schema.vendor="SEED" \
      org.label-schema.build-date="${BUILD_DATE}" \
      org.label-schema.vcs-ref="${GIT_REV}" \
      org.label-schema.version="${VERSION}"

COPY target/dependency /api/lib
COPY target/api.jar /api/api.jar

CMD ["java", "-cp", "/api/api.jar:/api/lib/*", "seed.server.MainKt"]
