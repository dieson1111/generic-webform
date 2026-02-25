#!/bin/bash
# Wait for SQL Server to be ready, then run the init script.
# This script is called as the container's entrypoint after SQL Server starts.

set -e

echo "Waiting for SQL Server to start..."
for i in {1..30}; do
    /opt/mssql-tools18/bin/sqlcmd \
        -S localhost \
        -U sa \
        -P "${SA_PASSWORD}" \
        -C \
        -Q "SELECT 1" \
        > /dev/null 2>&1 && break
    echo "  attempt $i/30 — not ready yet, sleeping 2s..."
    sleep 2
done

echo "SQL Server is ready. Running init script..."
/opt/mssql-tools18/bin/sqlcmd \
    -S localhost \
    -U sa \
    -P "${SA_PASSWORD}" \
    -C \
    -i /init.sql

echo "Initialisation complete."
