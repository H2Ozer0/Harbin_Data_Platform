import os
import sys
import psycopg2

DEFAULT_CONN = {
    "host": "101.35.234.65",
    "port": 5432,
    "dbname": "postgres",
    "user": "osmuser",
    "password": "pass",
}

HOTSPOT_SQL = """
INSERT INTO ads.hotspot_grid_enriched
(dt, hour_of_day, event_type, grid_id, lon, lat, event_count, hotspot_score, rank_in_hour, supply_demand_ratio, day_type)
WITH base AS (
    SELECT dt, hour_of_day, event_type, grid_id, hotspot_score, rank_in_hour
    FROM tdm.grid_hotspot_score
),
event_counts AS (
    SELECT dt, hour_of_day, event_type, grid_id, event_count, day_type
    FROM dw.fact_trip_event_grid_hour
),
supply AS (
    SELECT dt, hour_of_day, grid_id,
           SUM(CASE WHEN event_type = 'pickup' THEN event_count ELSE 0 END) AS pickups,
           SUM(CASE WHEN event_type = 'dropoff' THEN event_count ELSE 0 END) AS dropoffs
    FROM dw.fact_trip_event_grid_hour
    GROUP BY dt, hour_of_day, grid_id
),
grid_meta AS (
    SELECT grid_id,
           split_part(grid_id, '_', 1)::double precision AS lon,
           split_part(grid_id, '_', 2)::double precision AS lat
    FROM tdm.grid_hotspot_score
    GROUP BY grid_id
)
SELECT b.dt,
       b.hour_of_day,
       b.event_type,
       b.grid_id,
       gm.lon,
       gm.lat,
       ec.event_count::int,
       b.hotspot_score,
       b.rank_in_hour,
       CASE
           WHEN (s.pickups + s.dropoffs) > 0
               THEN (s.pickups - s.dropoffs)::double precision / (s.pickups + s.dropoffs)
           ELSE NULL
       END AS supply_demand_ratio,
       ec.day_type
FROM base b
LEFT JOIN event_counts ec
  ON b.dt = ec.dt
 AND b.hour_of_day = ec.hour_of_day
 AND b.event_type = ec.event_type
 AND b.grid_id = ec.grid_id
LEFT JOIN supply s
  ON b.dt = s.dt
 AND b.hour_of_day = s.hour_of_day
 AND b.grid_id = s.grid_id
LEFT JOIN grid_meta gm
  ON b.grid_id = gm.grid_id;
"""

DRIVER_SQL = """
INSERT INTO ads.driver_behavior_summary
(dt, shift_pattern, pattern_score, avg_active_minutes, avg_rest_minutes, rest_event_count, driver_count)
SELECT s.dt,
       s.shift_pattern,
       AVG(s.pattern_score) AS pattern_score,
       AVG(s.total_active_minutes) AS avg_active_minutes,
       AVG(r.rest_minutes) AS avg_rest_minutes,
       COUNT(r.id) AS rest_event_count,
       COUNT(DISTINCT s.devid) AS driver_count
FROM tdm.driver_shift_pattern s
LEFT JOIN tdm.driver_rest_location r
  ON s.devid = r.devid
 AND s.dt = r.dt
GROUP BY s.dt, s.shift_pattern;
"""

DRIVER_REST_SQL = """
INSERT INTO ads.driver_rest_enriched
(dt, devid, lon, lat, rest_minutes, rest_start, shift_pattern)
SELECT r.dt,
       r.devid,
       r.lon,
       r.lat,
       r.rest_minutes,
       r.rest_start,
       s.shift_pattern
FROM tdm.driver_rest_location r
LEFT JOIN tdm.driver_shift_pattern s
  ON r.devid = s.devid AND r.dt = s.dt;
"""


def get_conn_params():
    return {
        "host": os.getenv("DB_HOST", DEFAULT_CONN["host"]),
        "port": int(os.getenv("DB_PORT", DEFAULT_CONN["port"])),
        "dbname": os.getenv("DB_NAME", DEFAULT_CONN["dbname"]),
        "user": os.getenv("DB_USER", DEFAULT_CONN["user"]),
        "password": os.getenv("DB_PASSWORD", DEFAULT_CONN["password"]),
    }


def main():
    params = get_conn_params()
    print("Connecting to database...")
    conn = psycopg2.connect(**params)
    conn.autocommit = False

    try:
        with conn.cursor() as cur:
            print("Truncating ADS tables...")
            cur.execute("TRUNCATE ads.hotspot_grid_enriched;")
            cur.execute("TRUNCATE ads.driver_behavior_summary;")
            cur.execute("TRUNCATE ads.driver_rest_enriched;")

            print("Filling ads.hotspot_grid_enriched...")
            cur.execute(HOTSPOT_SQL)
            print(f"Inserted hotspot rows: {cur.rowcount}")

            print("Filling ads.driver_behavior_summary...")
            cur.execute(DRIVER_SQL)
            print(f"Inserted driver rows: {cur.rowcount}")

            print("Filling ads.driver_rest_enriched...")
            cur.execute(DRIVER_REST_SQL)
            print(f"Inserted driver rest rows: {cur.rowcount}")

        conn.commit()
        print("Done.")
    except Exception as exc:
        conn.rollback()
        print(f"Failed: {exc}")
        sys.exit(1)
    finally:
        conn.close()


if __name__ == "__main__":
    main()
