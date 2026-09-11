#!/usr/bin/env bash
# Import Natural Earth 1:110m shapefiles into the geodata PostGIS database.
# Run this once after `docker compose up geoserver-db`.
set -euo pipefail

CONTAINER="users-geoserver-db"
DB="geodata"
DB_USER="geouser"
NE_BASE="https://naciscdn.org/naturalearth/110m"
WORKDIR="$(mktemp -d)"
trap 'rm -rf "$WORKDIR"' EXIT

echo "==> Checking container '$CONTAINER' is running..."
if ! docker inspect --format '{{.State.Running}}' "$CONTAINER" 2>/dev/null | grep -q true; then
  echo "ERROR: Container '$CONTAINER' is not running."
  echo "       Start it with: docker compose up -d geoserver-db"
  exit 1
fi

echo "==> Ensuring ogr2ogr (gdal-tools) is available in container..."
if ! docker exec "$CONTAINER" sh -c "which ogr2ogr" >/dev/null 2>&1; then
  docker exec "$CONTAINER" apk add --no-cache gdal-tools >/dev/null 2>&1
fi

import_layer() {
  local TABLE="$1"
  local URL="$2"
  local SHP="$3"
  local ZIPFILE="$WORKDIR/${TABLE}.zip"
  local EXTRACTDIR="$WORKDIR/$TABLE"

  echo "==> Downloading $TABLE..."
  curl -fsSL --retry 3 -o "$ZIPFILE" "$URL"

  echo "==> Extracting $TABLE..."
  mkdir -p "$EXTRACTDIR"
  unzip -q "$ZIPFILE" -d "$EXTRACTDIR"

  echo "==> Copying shapefile into container..."
  docker cp "$EXTRACTDIR/." "$CONTAINER:/tmp/ne_$TABLE/"

  echo "==> Importing into PostGIS (table: public.$TABLE)..."
  docker exec "$CONTAINER" sh -c \
    "ogr2ogr -f PGDUMP /vsistdout/ /tmp/ne_$TABLE/$SHP \
      -nln public.$TABLE \
      -nlt PROMOTE_TO_MULTI \
      -t_srs EPSG:4326 \
      -lco GEOMETRY_NAME=geom \
      -lco FID=gid \
      -lco DROP_TABLE=ON 2>/dev/null \
     | psql -U ${DB_USER} -d ${DB} -q"

  docker exec "$CONTAINER" rm -rf "/tmp/ne_$TABLE"
  echo "    OK: $TABLE imported."
}

import_layer countries \
  "${NE_BASE}/cultural/ne_110m_admin_0_countries.zip" \
  "ne_110m_admin_0_countries.shp"

import_layer rivers \
  "${NE_BASE}/physical/ne_110m_rivers_lake_centerlines.zip" \
  "ne_110m_rivers_lake_centerlines.shp"

import_layer cities \
  "${NE_BASE}/cultural/ne_110m_populated_places_simple.zip" \
  "ne_110m_populated_places_simple.shp"

echo ""
echo "==> Verifying row counts..."
docker exec "$CONTAINER" psql -U "$DB_USER" -d "$DB" -c "
  SELECT 'countries' AS layer, count(*) FROM countries
  UNION ALL SELECT 'rivers', count(*) FROM rivers
  UNION ALL SELECT 'cities', count(*) FROM cities;
"

echo ""
echo "Done. All three layers are ready for GeoServer."
