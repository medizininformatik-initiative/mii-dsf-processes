#!/bin/bash
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE DATABASE dic_fhir;
    GRANT ALL PRIVILEGES ON DATABASE dic_fhir TO liquibase_user;
    CREATE DATABASE dic_bpe;
    GRANT ALL PRIVILEGES ON DATABASE dic_bpe TO liquibase_user;
    CREATE DATABASE cos_fhir;
    GRANT ALL PRIVILEGES ON DATABASE cos_fhir TO liquibase_user;
    CREATE DATABASE cos_bpe;
    GRANT ALL PRIVILEGES ON DATABASE cos_bpe TO liquibase_user;
    CREATE DATABASE hrp_fhir;
    GRANT ALL PRIVILEGES ON DATABASE hrp_fhir TO liquibase_user;
    CREATE DATABASE hrp_bpe;
    GRANT ALL PRIVILEGES ON DATABASE hrp_bpe TO liquibase_user;
EOSQL