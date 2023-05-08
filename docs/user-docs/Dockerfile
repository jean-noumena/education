FROM ghcr.io/noumenadigital/documentation/mkdocs-builder:latest

# Note: this is not the final or even suggested folder structure. That is still TBD.
RUN rm -rf /docs/content/docs/seed
COPY seed /docs/content/docs/seed

# Move this to the corresponding file in the `documentation`
# repo once the seed docs are ready for publishing:
RUN echo "    - Seed Project: seed" >> /docs/content/docs/.pages

ENV CHECK_SNIPPET_PATHS=false

RUN mkdocs build
