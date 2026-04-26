package com.harbin.dataplatform.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class MapRepository {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public MapRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> findBoundariesInBBOX(
            double minLon, double minLat, double maxLon, double maxLat,
            int limit, int offset
    ) {
        String sql = "SELECT gid, osm_id, class_id, length, reverse, " +
                "maxspeed_forward, maxspeed_backward, priority, " +
                "ST_AsGeoJSON(geom)::text AS geojson " +
                "FROM public.bfmap_ways " +
                "WHERE ST_Intersects(geom, " +
                "ST_MakeEnvelope(?, ?, ?, ?, 4326)) " +
                "ORDER BY gid " +
                "LIMIT ? OFFSET ?";
        return jdbcTemplate.queryForList(sql, minLon, minLat, maxLon, maxLat, limit, offset);
    }

    public long countBoundariesInBBOX(double minLon, double minLat, double maxLon, double maxLat) {
        String sql = "SELECT COUNT(*) FROM public.bfmap_ways " +
                "WHERE ST_Intersects(geom, " +
                "ST_MakeEnvelope(?, ?, ?, ?, 4326))";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, minLon, minLat, maxLon, maxLat);
        return count != null ? count : 0;
    }

    public List<Map<String, Object>> findAllPaginated(int limit, int offset) {
        String sql = "SELECT gid, osm_id, class_id, length, reverse, " +
                "maxspeed_forward, maxspeed_backward, priority, " +
                "ST_AsGeoJSON(geom)::text AS geojson " +
                "FROM public.bfmap_ways " +
                "ORDER BY gid " +
                "LIMIT ? OFFSET ?";
        return jdbcTemplate.queryForList(sql, limit, offset);
    }

    public long countAll() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM public.bfmap_ways", Integer.class);
        return count != null ? count : 0;
    }

    public List<Map<String, Object>> findByClassId(int classId, int limit, int offset) {
        String sql = "SELECT gid, osm_id, class_id, length, reverse, " +
                "maxspeed_forward, maxspeed_backward, priority, " +
                "ST_AsGeoJSON(geom)::text AS geojson " +
                "FROM public.bfmap_ways " +
                "WHERE class_id = ? " +
                "ORDER BY gid " +
                "LIMIT ? OFFSET ?";
        return jdbcTemplate.queryForList(sql, classId, limit, offset);
    }

    public long countByClassId(int classId) {
        String sql = "SELECT COUNT(*) FROM public.bfmap_ways WHERE class_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, classId);
        return count != null ? count : 0;
    }
}
