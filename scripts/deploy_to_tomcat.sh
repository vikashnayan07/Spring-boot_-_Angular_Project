#!/usr/bin/env bash
set -euo pipefail

if [ "$#" -lt 1 ] || [ "$#" -gt 2 ]; then
  echo "Usage: machcare-deploy deploy /path/to/machcare.war | machcare-deploy rollback" >&2
  exit 2
fi

ACTION="$1"
WAR_SOURCE="${2:-}"
WEBAPPS_DIR="/var/lib/tomcat10/webapps"
BACKUP_DIR="/var/lib/tomcat10/deploy-backups"
APP_NAME="machcare"
TS="$(date +%Y%m%d-%H%M%S)"
LATEST_BACKUP="$BACKUP_DIR/latest-stable-backup"

wait_for_app() {
  for _ in $(seq 1 36); do
    if curl -fsSI http://127.0.0.1:8080/machcare/ >/dev/null; then
      return 0
    fi
    sleep 5
  done
  return 1
}

rollback() {
  if [ ! -L "$LATEST_BACKUP" ] && [ ! -d "$LATEST_BACKUP" ]; then
    echo "No stable backup is available for rollback." >&2
    exit 1
  fi

  BACKUP_PATH="$(readlink -f "$LATEST_BACKUP")"
  if [ -z "$BACKUP_PATH" ] || [ ! -d "$BACKUP_PATH" ]; then
    echo "Stable backup path is invalid: $LATEST_BACKUP" >&2
    exit 1
  fi

  systemctl stop tomcat10
  mkdir -p "$BACKUP_DIR/failed-$TS"

  if [ -f "$WEBAPPS_DIR/$APP_NAME.war" ]; then
    mv "$WEBAPPS_DIR/$APP_NAME.war" "$BACKUP_DIR/failed-$TS/$APP_NAME.war"
  fi

  if [ -d "$WEBAPPS_DIR/$APP_NAME" ]; then
    mv "$WEBAPPS_DIR/$APP_NAME" "$BACKUP_DIR/failed-$TS/$APP_NAME"
  fi

  if [ -f "$BACKUP_PATH/$APP_NAME.war" ]; then
    cp "$BACKUP_PATH/$APP_NAME.war" "$WEBAPPS_DIR/$APP_NAME.war"
  fi

  if [ -d "$BACKUP_PATH/$APP_NAME" ]; then
    cp -a "$BACKUP_PATH/$APP_NAME" "$WEBAPPS_DIR/$APP_NAME"
  fi

  chown -R tomcat:tomcat "$WEBAPPS_DIR/$APP_NAME"* 2>/dev/null || true
  systemctl start tomcat10

  if wait_for_app; then
    echo "Rollback restored $APP_NAME from $BACKUP_PATH"
    exit 0
  fi

  journalctl -u tomcat10 -n 120 --no-pager >&2 || true
  echo "Rollback failed to serve /machcare/." >&2
  exit 1
}

if [ "$ACTION" = "rollback" ]; then
  rollback
fi

if [ "$ACTION" != "deploy" ]; then
  echo "Unknown action: $ACTION" >&2
  exit 2
fi

if [ ! -f "$WAR_SOURCE" ]; then
  echo "WAR not found: $WAR_SOURCE" >&2
  exit 2
fi

mkdir -p "$BACKUP_DIR"
RELEASE_BACKUP="$BACKUP_DIR/$TS"
mkdir -p "$RELEASE_BACKUP"

systemctl stop tomcat10

if [ -f "$WEBAPPS_DIR/$APP_NAME.war" ]; then
  cp "$WEBAPPS_DIR/$APP_NAME.war" "$RELEASE_BACKUP/$APP_NAME.war"
  rm -f "$WEBAPPS_DIR/$APP_NAME.war"
fi

if [ -d "$WEBAPPS_DIR/$APP_NAME" ]; then
  cp -a "$WEBAPPS_DIR/$APP_NAME" "$RELEASE_BACKUP/$APP_NAME"
  rm -rf "$WEBAPPS_DIR/$APP_NAME"
fi

ln -sfn "$RELEASE_BACKUP" "$LATEST_BACKUP"

cp "$WAR_SOURCE" "$WEBAPPS_DIR/$APP_NAME.war"
chown tomcat:tomcat "$WEBAPPS_DIR/$APP_NAME.war"

systemctl start tomcat10

if wait_for_app; then
  exit 0
fi

journalctl -u tomcat10 -n 120 --no-pager >&2 || true
echo "Tomcat did not serve /machcare/ after deployment. Rolling back." >&2
rollback
