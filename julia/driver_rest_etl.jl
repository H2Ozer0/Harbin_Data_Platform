using LibPQ
using DataFrames
using Dates

include(joinpath(@__DIR__, "parameters.jl"))

const DEFAULT_CONN = "host=101.35.234.65 port=5432 dbname=postgres user=osmuser password=pass"

function connect_db()
    conn_str = get(ENV, "DB_CONN", DEFAULT_CONN)
    LibPQ.Connection(conn_str)
end

function reconnect_db(conn)
    try
        close(conn)
    catch
    end
    connect_db()
end

function ensure_table(conn)
    execute(conn, """
        CREATE TABLE IF NOT EXISTS tdm.driver_rest_location (
            id SERIAL PRIMARY KEY,
            devid TEXT NOT NULL,
            dt DATE NOT NULL,
            rest_start TIMESTAMP NOT NULL,
            rest_end TIMESTAMP NOT NULL,
            rest_minutes DOUBLE PRECISION NOT NULL,
            lon DOUBLE PRECISION NOT NULL,
            lat DOUBLE PRECISION NOT NULL,
            created_at TIMESTAMP DEFAULT NOW()
        );
    """)
end

function load_points(conn; limit::Union{Nothing,Int}=nothing)
    limit_clause = isnothing(limit) ? "" : "LIMIT $(limit)"
    sql = """
        SELECT devid, tms, lon, lat
        FROM ods.ods_taxi_trips_raw,
             unnest(tms_seq, lon_seq, lat_seq) AS u(tms, lon, lat)
        WHERE devid IS NOT NULL
          AND tms IS NOT NULL
          AND lon IS NOT NULL
          AND lat IS NOT NULL
        $(limit_clause)
    """
    DataFrame(execute(conn, sql))
end

function load_driver_ids(conn; limit::Union{Nothing,Int}=nothing)
    limit_clause = isnothing(limit) ? "" : "LIMIT $(limit)"
    sql = """
        SELECT DISTINCT devid
        FROM ods.ods_taxi_trips_raw
        WHERE devid IS NOT NULL
        $(limit_clause)
    """
    DataFrame(execute(conn, sql)).devid
end

function load_points_for_driver(conn, devid::AbstractString)
    sql = """
        SELECT tms, lon, lat
        FROM ods.ods_taxi_trips_raw,
             unnest(tms_seq, lon_seq, lat_seq) AS u(tms, lon, lat)
        WHERE devid = \$1
          AND tms IS NOT NULL
          AND lon IS NOT NULL
          AND lat IS NOT NULL
    """
    DataFrame(execute(conn, sql, [devid]))
end

function unix_to_datetime(ts::Int64)
    Dates.unix2datetime(ts)
end

function flush_events!(conn_ref, events, insert_sql)
    nrow(events) == 0 && return
    for attempt in 1:2
        conn = conn_ref[]
        try
            execute(conn, "BEGIN;")
            for row in eachrow(events)
                execute(conn, insert_sql, [
                    row.devid,
                    row.dt,
                    row.rest_start,
                    row.rest_end,
                    row.rest_minutes,
                    row.lon,
                    row.lat
                ])
            end
            execute(conn, "COMMIT;")
            empty!(events)
            return
        catch e
            try
                execute(conn, "ROLLBACK;")
            catch
            end
            if attempt == 2
                rethrow(e)
            end
            println("[warn] DB connection lost, reconnecting...")
            conn_ref[] = reconnect_db(conn)
        end
    end
end

function extract_and_insert(conn_ref, points::DataFrame; batch_size::Int=1000)
    if nrow(points) == 0
        println("No points loaded.")
        return
    end

    points.tms = Int64.(round.(points.tms))
    sort!(points, [:devid, :tms])

    insert_sql = """
        INSERT INTO tdm.driver_rest_location
        (devid, dt, rest_start, rest_end, rest_minutes, lon, lat)
        VALUES (\$1, \$2, \$3, \$4, \$5, \$6, \$7)
    """

    events = DataFrame(
        devid=String[],
        dt=Date[],
        rest_start=DateTime[],
        rest_end=DateTime[],
        rest_minutes=Float64[],
        lon=Float64[],
        lat=Float64[]
    )

    for g in groupby(points, :devid)
        tms = g.tms
        if length(tms) < 2
            continue
        end
        diffs = diff(tms)
        gap_idx = findall(>(MAX_GAP_SECONDS), diffs)
        for i in gap_idx
            gap_sec = diffs[i]
            rest_minutes = gap_sec / 60.0
            if rest_minutes < MIN_DURATION_MINUTES || rest_minutes > MAX_DURATION_MINUTES
                continue
            end
            start_ts = tms[i]
            end_ts = tms[i + 1]
            rest_start = unix_to_datetime(start_ts)
            rest_end = unix_to_datetime(end_ts)
            push!(events, (
                string(g.devid[i]),
                Date(rest_start),
                rest_start,
                rest_end,
                rest_minutes,
                Float64(g.lon[i]),
                Float64(g.lat[i])
            ))

            if nrow(events) >= batch_size
                flush_events!(conn_ref, events, insert_sql)
            end
        end
    end

    flush_events!(conn_ref, events, insert_sql)
end

function extract_and_insert_by_driver(conn_ref; driver_limit::Union{Nothing,Int}=nothing, batch_size::Int=1000)
    insert_sql = """
        INSERT INTO tdm.driver_rest_location
        (devid, dt, rest_start, rest_end, rest_minutes, lon, lat)
        VALUES (\$1, \$2, \$3, \$4, \$5, \$6, \$7)
    """

    events = DataFrame(
        devid=String[],
        dt=Date[],
        rest_start=DateTime[],
        rest_end=DateTime[],
        rest_minutes=Float64[],
        lon=Float64[],
        lat=Float64[]
    )

    driver_ids = load_driver_ids(conn_ref[]; limit=driver_limit)
    total = length(driver_ids)
    println("Drivers to process: $(total)")

    for (idx, devid) in enumerate(driver_ids)
        points = try
            load_points_for_driver(conn_ref[], devid)
        catch e
            println("[warn] query failed, reconnecting...")
            conn_ref[] = reconnect_db(conn_ref[])
            load_points_for_driver(conn_ref[], devid)
        end
        if nrow(points) < 2
            continue
        end
        points.tms = Int64.(round.(points.tms))
        sort!(points, :tms)
        tms = points.tms
        diffs = diff(tms)
        gap_idx = findall(>(MAX_GAP_SECONDS), diffs)
        for i in gap_idx
            gap_sec = diffs[i]
            rest_minutes = gap_sec / 60.0
            if rest_minutes < MIN_DURATION_MINUTES || rest_minutes > MAX_DURATION_MINUTES
                continue
            end
            start_ts = tms[i]
            end_ts = tms[i + 1]
            rest_start = unix_to_datetime(start_ts)
            rest_end = unix_to_datetime(end_ts)
            push!(events, (
                string(devid),
                Date(rest_start),
                rest_start,
                rest_end,
                rest_minutes,
                Float64(points.lon[i]),
                Float64(points.lat[i])
            ))

            if nrow(events) >= batch_size
                flush_events!(conn_ref, events, insert_sql)
            end
        end

        if idx % 100 == 0 || idx == total
            println("Processed $(idx)/$(total) drivers...")
        end
    end

    flush_events!(conn_ref, events, insert_sql)
end

function main()
    println("Connecting to database...")
    conn = connect_db()
    conn_ref = Ref(conn)
    try
        ensure_table(conn_ref[])
        execute(conn_ref[], "TRUNCATE tdm.driver_rest_location;")

        limit = get(ENV, "SAMPLE_LIMIT", "")
        limit_val = isempty(limit) ? nothing : parse(Int, limit)

        if isnothing(limit_val)
            println("Processing by driver (full run)...")
            extract_and_insert_by_driver(conn_ref)
        else
            println("Loading points (sample)...")
            points = load_points(conn_ref[]; limit=limit_val)
            println("Loaded $(nrow(points)) points.")
            println("Extracting rest events (sample)...")
            extract_and_insert(conn_ref, points)
        end

        println("Done.")
    finally
        try
            close(conn_ref[])
        catch
        end
    end
end

main()
