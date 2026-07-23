#!/usr/bin/env bash
set -euo pipefail

until mysqladmin ping -hmysql-primary -uroot -p"${MYSQL_ROOT_PASSWORD}" --silent; do sleep 2; done
until mysqladmin ping -hmysql-replica -uroot -p"${MYSQL_ROOT_PASSWORD}" --silent; do sleep 2; done

mysql -hmysql-replica -uroot -p"${MYSQL_ROOT_PASSWORD}" <<SQL
STOP REPLICA;
RESET REPLICA ALL;
CHANGE REPLICATION SOURCE TO
  SOURCE_HOST='mysql-primary',
  SOURCE_PORT=3306,
  SOURCE_USER='refine_repl',
  SOURCE_PASSWORD='${MYSQL_REPLICATION_PASSWORD}',
  SOURCE_AUTO_POSITION=1,
  GET_SOURCE_PUBLIC_KEY=1;
START REPLICA;
SQL

for attempt in $(seq 1 60); do
  state=$(mysql -N -B -hmysql-replica -uroot -p"${MYSQL_ROOT_PASSWORD}" -e \
    "SELECT CONCAT(COALESCE((SELECT SERVICE_STATE FROM performance_schema.replication_connection_status LIMIT 1),''),':',COALESCE((SELECT SERVICE_STATE FROM performance_schema.replication_applier_status LIMIT 1),''))" 2>/dev/null || true)
  if [[ "${state}" == "ON:ON" ]]; then
    echo "MySQL GTID replication is running"
    exit 0
  fi
  sleep 2
done

mysql -hmysql-replica -uroot -p"${MYSQL_ROOT_PASSWORD}" -e "SHOW REPLICA STATUS\\G"
echo "Replication did not become healthy" >&2
exit 1
