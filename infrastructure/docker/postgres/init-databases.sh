#!/usr/bin/env sh

set -eu

create_database() {
    database="$1"

    exists="$(
        psql \
            --username "$POSTGRES_USER" \
            --dbname "$POSTGRES_DB" \
            --tuples-only \
            --no-align \
            --command "SELECT 1 FROM pg_database WHERE datname = '$database';"
    )"

    if [ "$exists" != "1" ]; then
        psql \
            --username "$POSTGRES_USER" \
            --dbname "$POSTGRES_DB" \
            --command "CREATE DATABASE \"$database\";"
    fi
}

create_database "$USER_DB_NAME"
create_database "$INVENTORY_DB_NAME"
create_database "$ORDER_DB_NAME"
create_database "$NOTIFICATION_DB_NAME"