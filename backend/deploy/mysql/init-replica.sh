#!/usr/bin/env bash
set -euo pipefail

mysql --protocol=socket -uroot -p"${MYSQL_ROOT_PASSWORD}" <<SQL
SET PERSIST_ONLY read_only=ON;
SET PERSIST_ONLY super_read_only=ON;
SQL

