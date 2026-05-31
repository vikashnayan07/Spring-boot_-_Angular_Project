#!/usr/bin/env bash
set -euo pipefail

if [ "$#" -ne 1 ]; then
  echo "Usage: machcare-deploy /path/to/machcare.war" >&2
  exit 2
fi

WAR_SOURCE="$1"
WEBAPPS_DIR="/var/lib/tomcat10/webapps"
BACKUP_DIR="/var/lib/tomcat10/deploy-backups"
APP_NAME="machcare"
TS="$(date +%Y%m%d-%H%M%S)"

if [ ! -f "$WAR_SOURCE" ]; then
  echo "WAR not found: $WAR_SOURCE" >&2
  exit 2
fi

systemctl stop tomcat10
mkdir -p "$BACKUP_DIR"

if [ -f "$WEBAPPS_DIR/$APP_NAME.war" ]; then
  mv "$WEBAPPS_DIR/$APP_NAME.war" "$BACKUP_DIR/$APP_NAME.war.$TS"
fi

if [ -d "$WEBAPPS_DIR/$APP_NAME" ]; then
  mv "$WEBAPPS_DIR/$APP_NAME" "$BACKUP_DIR/$APP_NAME.$TS"
fi

cp "$WAR_SOURCE" "$WEBAPPS_DIR/$APP_NAME.war"
chown tomcat:tomcat "$WEBAPPS_DIR/$APP_NAME.war"

systemctl start tomcat10

for _ in $(seq 1 36); do
  if curl -fsSI http://127.0.0.1:8080/machcare/ >/dev/null; then
    exit 0
  fi
  sleep 5
done

journalctl -u tomcat10 -n 120 --no-pager >&2 || true
echo "Tomcat did not serve /machcare/ after deployment." >&2
exit 1
