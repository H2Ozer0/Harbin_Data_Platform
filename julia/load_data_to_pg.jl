using JLD2, LibPQ, Tables

# 1. 包含原始结构体定义
if isfile("Trip.jl")
    include("Trip.jl")
elseif isfile("julia/Trip.jl")
    include("julia/Trip.jl")
else
    error("找不到 Trip.jl 文件。")
end

# 原生数组转换函数
function to_float_array(arr)
    isnothing(arr) && return Float64[]
    return Float64[Float64(x) for x in vec(arr) if !isnothing(x)]
end

function to_int_array(arr)
    isnothing(arr) && return Int64[]
    return Int64[round(Int64, x) for x in vec(arr) if !isnothing(x)]
end

function to_string_array(arr)
    isnothing(arr) && return String[]
    return String[string(x) for x in vec(arr) if !isnothing(x) && strip(string(x)) != ""]
end

# ==========================================
# 🛑 防重机制：在这里填入你已经成功导入的文件名
# ==========================================
completed_files = []

# 2. 数据库连接
conn = LibPQ.Connection("host=localhost port=5432 user=osmuser password=pass dbname=harbin")

# 3. 自动定位包含文件的文件夹
global_data_dir = ""
for d in ["../data/jldpath", "./data/jldpath", "."]
    if isdir(d)
        global global_data_dir = d
        break
    end
end

if global_data_dir == ""
    error("找不到存放数据的文件夹 data/jldpath")
end

# 获取所有 .jld2 文件
all_files = filter(f -> endswith(f, ".jld2"), readdir(global_data_dir))

println("🎯 扫描到 $(length(all_files)) 个 .jld2 文件，准备开始处理...\n")

# 4. 遍历处理文件
for fname in all_files
    println("=========================================")

    # 【关键防重判断】：如果文件已经在列表中，直接跳过
    if fname in completed_files
        println("⏭️ 文件 [ $(fname) ] 已经导入过，自动跳过。")
        continue
    end

    file_path = joinpath(global_data_dir, fname)
    println("▶️ 正在导入新文件: $(fname)")

    res = load(file_path)
    trips = res["trips"]
    total = length(trips)
    println("   成功读取 $(total) 条轨迹，开始写入数据库...")

    execute(conn, "BEGIN;")

    try
        for (i, t) in enumerate(trips)
            execute(conn, """
                INSERT INTO ods.ods_taxi_trips_raw
                (lon_seq, lat_seq, tms_seq, devid, roads_seq, time_seq, frac_seq, route_ids, route_headings, route_geoms)
                VALUES (\$1, \$2, \$3, \$4, \$5, \$6, \$7, \$8, \$9, \$10::text[]::geometry[])
            """, [
                to_float_array(t.lon),
                to_float_array(t.lat),
                to_float_array(t.tms),
                t.devid,
                to_int_array(t.roads),
                to_int_array(t.time),
                to_float_array(t.frac),
                to_int_array(t.route),
                to_string_array(t.route_heading),
                to_string_array(t.route_geom)
            ])

            if i % 5000 == 0
                println("   >> 进度: $(i) / $(total) ($(round(i/total*100, digits=1))%)")
            end
        end

        execute(conn, "COMMIT;")
        println("✅ 文件 $(fname) 导入完成！")

    catch e
        execute(conn, "ROLLBACK;")
        println("❌ 文件 $(fname) 导入失败。详细错误: $(e)")
    end
end

println("\n🎉🎉 所有需要导入的文件已全部处理完毕！")
close(conn)