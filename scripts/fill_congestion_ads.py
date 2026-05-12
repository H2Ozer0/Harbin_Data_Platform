"""
填充 ads.congestion_by_segment_hour 表

数据来源：
  - dw.fact_congestion_seg_hour: 拥堵小时事实表（1,340,958 行）
  - tdm.congestion_baseline_5day: 拥堵基线5天滑动窗口（834,170 行）
  - public.ways: OSM 道路信息（用于提取 road_name / road_type）

逻辑：
  1. 从 dw.fact_congestion_seg_hour 提取拥堵事实
  2. LEFT JOIN tdm.congestion_baseline_5day 获取基线速度
  3. LEFT JOIN public.ways 获取道路名称和类型
  4. 计算 deviation_pct = (avg_speed - baseline) / baseline * 100
  5. 处理 NULL 基线速度情况
  6. 清空并填充到 ads.congestion_by_segment_hour

用法：
  python scripts/fill_congestion_ads.py
"""

import psycopg2
import psycopg2.extras
import time

# 数据库连接配置
DB_CONFIG = {
    "host": "101.35.234.65",
    "port": 5432,
    "dbname": "postgres",
    "user": "osmuser",
    "password": "pass",
}

# 批量处理大小
BATCH_SIZE = 50000


def get_connection():
    """获取数据库连接"""
    conn = psycopg2.connect(**DB_CONFIG)
    conn.autocommit = False
    return conn


def truncate_target(conn):
    """清空目标表"""
    with conn.cursor() as cur:
        cur.execute("TRUNCATE TABLE ads.congestion_by_segment_hour")
        conn.commit()
    print("[OK] 已清空 ads.congestion_by_segment_hour")


def fill_congestion_data(conn):
    """从源表填充拥堵数据到 ADS 层"""
    start_time = time.time()

    insert_sql = """
        INSERT INTO ads.congestion_by_segment_hour (
            dt, hour_of_day, road_segment_id,
            road_name, road_type,
            avg_speed_kmh, p50_speed_kmh, p85_speed_kmh,
            congestion_index,
            baseline_speed_kmh,
            deviation_pct,
            day_type,
            trip_count, length_m, geometry
        )
        SELECT
            f.dt,
            f.hour_of_day,
            f.road_segment_id,
            NULLIF(w.tags -> 'name', '') AS road_name,
            NULLIF(w.tags -> 'highway', '') AS road_type,
            f.avg_speed_kmh,
            f.p50_speed_kmh,
            f.p85_speed_kmh,
            f.congestion_index,
            b.baseline_speed_kmh,
            CASE
                WHEN b.baseline_speed_kmh IS NOT NULL AND b.baseline_speed_kmh > 0
                THEN (f.avg_speed_kmh - b.baseline_speed_kmh) / b.baseline_speed_kmh * 100.0
                ELSE NULL
            END AS deviation_pct,
            COALESCE(f.day_type, 'unknown') AS day_type,
            COALESCE(f.trip_count, 0) AS trip_count,
            dr.length_m,
            dr.geom AS geometry
        FROM dw.fact_congestion_seg_hour f
        LEFT JOIN tdm.congestion_baseline_5day b
            ON b.road_segment_id = f.road_segment_id
            AND b.hour_of_day = f.hour_of_day
            AND b.day_type = COALESCE(f.day_type, 'unknown')
        LEFT JOIN dw.dim_road_segment dr
            ON dr.road_segment_id = f.road_segment_id
        LEFT JOIN public.ways w
            ON w.id = dr.osm_way_id
    """

    with conn.cursor() as cur:
        cur.execute(insert_sql)
        inserted = cur.rowcount
        conn.commit()

    elapsed = time.time() - start_time
    print(f"[OK] 插入 {inserted} 行，耗时 {elapsed:.1f}s")
    return inserted


def verify_data(conn):
    """验证数据完整性"""
    checks = [
        ("总行数", "SELECT COUNT(*) FROM ads.congestion_by_segment_hour"),
        ("deviation_pct 非空行数",
         "SELECT COUNT(*) FROM ads.congestion_by_segment_hour WHERE deviation_pct IS NOT NULL"),
        ("有道路名称的行数",
         "SELECT COUNT(*) FROM ads.congestion_by_segment_hour WHERE road_name IS NOT NULL"),
        ("有道路类型的行数",
         "SELECT COUNT(*) FROM ads.congestion_by_segment_hour WHERE road_type IS NOT NULL"),
        ("distinct 日期数",
         "SELECT COUNT(DISTINCT dt) FROM ads.congestion_by_segment_hour"),
        ("distinct 路段数",
         "SELECT COUNT(DISTINCT road_segment_id) FROM ads.congestion_by_segment_hour"),
    ]

    print("\n=== 数据验证 ===")
    with conn.cursor() as cur:
        for label, sql in checks:
            cur.execute(sql)
            result = cur.fetchone()[0]
            print(f"  {label}: {result}")

    # 随机采样验证
    cur = conn.cursor()
    cur.execute("""
        SELECT dt, hour_of_day, road_segment_id, road_name, road_type,
               avg_speed_kmh, congestion_index, baseline_speed_kmh,
               deviation_pct, day_type
        FROM ads.congestion_by_segment_hour
        WHERE road_name IS NOT NULL AND deviation_pct IS NOT NULL
        ORDER BY RANDOM()
        LIMIT 5
    """)
    print("\n=== 采样验证（随机5行，有名称 + 有 deviation） ===")
    cols = [desc[0] for desc in cur.description]
    for row in cur.fetchall():
        print("  " + ", ".join(f"{c}={row[i]}" for i, c in enumerate(cols)))
    cur.close()


def main():
    print("=" * 60)
    print("  P1-T2: 填充 ads.congestion_by_segment_hour")
    print("=" * 60)

    conn = get_connection()
    try:
        # Step 1: 清空目标表
        truncate_target(conn)

        # Step 2: 填充数据
        total = fill_congestion_data(conn)

        # Step 3: 验证
        verify_data(conn)

        print(f"\n[DONE] 成功填充 {total} 行拥堵数据到 ads.congestion_by_segment_hour")
    finally:
        conn.close()


if __name__ == "__main__":
    main()