FROM amazoncorretto:17

ARG BUILD_DATE
ARG GIT_REV
ARG VERSION

WORKDIR /api

USER nobody

LABEL org.opencontainers.image.source = "https://github.com/noumenadigital/seed" \ 
      org.label-schema.name="api" \
      org.label-schema.vendor="SEED" \
      org.label-schema.build-date="${BUILD_DATE}" \
      org.label-schema.vcs-ref="${GIT_REV}" \
      org.label-schema.version="${VERSION}"

COPY target/classes/logback-json.xml /wp-plugin-service/
COPY target/dependency /api/lib
COPY target/api.jar /api/api.jar

CMD ["java", "-cp", "/api/api.jar:/api/lib/*", "main.MainKt"]
