FROM registry.noumenadigital.com/platform-tools/cli/clint:11.162.0

ARG BUILD_DATE
ARG GIT_REV
ARG VERSION
LABEL org.label-schema.name="npl" \
      org.label-schema.vendor="seed" \
      org.label-schema.build-date="${BUILD_DATE}" \
      org.label-schema.vcs-ref="${GIT_REV}" \
      org.label-schema.version="${VERSION}"

WORKDIR /upshift
USER nobody

COPY src/main/yaml /upshift/home/yaml
COPY src/main/npl  /upshift/home/npl-1.0.0

ENV ENGINE_DB_URL=jdbc:postgresql://engine-db:5432/engine \
    ENGINE_DB_USER=seed \
    ENGINE_DB_PASSWORD=secret

ENTRYPOINT ["java", "-jar", "/clint-exec.jar"]
CMD ["upshift", \
     "--log:home", "/upshift/home", \
     "--log:file", "/yaml/upshift.yml", \
     "--log", \
     "--engine:party", "system" \
     ]
