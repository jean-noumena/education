FROM registry.noumenadigital.com/noumena/platform/engine:2021.1.175

ENV ENGINE_NPL_MIGRATION_DIRECTORYPATH="/npl" \
    FEATURE_APPLY_NPL_MIGRATIONS_AT_STARTUP="true"

COPY src/main/yaml /npl/yaml
COPY src/main/npl  /npl/npl-1.0.0
