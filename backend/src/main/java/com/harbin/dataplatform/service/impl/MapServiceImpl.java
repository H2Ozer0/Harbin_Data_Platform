package com.harbin.dataplatform.service.impl;

import com.harbin.dataplatform.common.util.TypeConverter;
import com.harbin.dataplatform.dto.MapBoundaryDTO;
import com.harbin.dataplatform.dto.PageResponse;
import com.harbin.dataplatform.repository.MapRepository;
import com.harbin.dataplatform.service.MapService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MapServiceImpl implements MapService {

    private final MapRepository mapRepository;

    @Override
    public PageResponse<MapBoundaryDTO> getBoundariesInBBOX(
            double minLon, double minLat, double maxLon, double maxLat,
            int page, int size
    ) {
        log.info("Querying bfmap_ways in BBOX: [{},{}, {},{}], page={}, size={}",
                minLon, minLat, maxLon, maxLat, page, size);

        long total = mapRepository.countBoundariesInBBOX(minLon, minLat, maxLon, maxLat);
        List<Map<String, Object>> rows = mapRepository.findBoundariesInBBOX(
                minLon, minLat, maxLon, maxLat, size, page * size
        );

        List<MapBoundaryDTO> dtoList = rows.stream().map(this::rowToDTO).toList();
        return PageResponse.of(dtoList, page, size, total);
    }

    @Override
    public PageResponse<MapBoundaryDTO> getAllBoundaries(int page, int size) {
        long total = mapRepository.countAll();
        List<Map<String, Object>> rows = mapRepository.findAllPaginated(size, page * size);

        List<MapBoundaryDTO> dtoList = rows.stream().map(this::rowToDTO).toList();
        return PageResponse.of(dtoList, page, size, total);
    }

    @Override
    public PageResponse<MapBoundaryDTO> getBoundariesByType(
            String highwayType, int page, int size
    ) {
        int classId;
        try {
            classId = Integer.parseInt(highwayType);
        } catch (NumberFormatException e) {
            return PageResponse.of(List.of(), page, size, 0);
        }

        List<Map<String, Object>> rows = mapRepository.findByClassId(
                classId, size, page * size
        );
        long total = mapRepository.countByClassId(classId);

        List<MapBoundaryDTO> dtoList = rows.stream().map(this::rowToDTO).toList();
        return PageResponse.of(dtoList, page, size, total);
    }

    @Override
    public List<MapBoundaryDTO> getFullMapBoundary() {
        log.info("Loading full Harbin map data from bfmap_ways");
        List<Map<String, Object>> rows = mapRepository.findAllPaginated(30000, 0);
        return rows.stream().map(this::rowToDTO).toList();
    }

    private MapBoundaryDTO rowToDTO(Map<String, Object> row) {
        MapBoundaryDTO dto = new MapBoundaryDTO();
        dto.setGid(TypeConverter.toLong(row.get("gid")));
        dto.setOsmId(TypeConverter.toLong(row.get("osm_id")));
        dto.setClassId(TypeConverter.toInteger(row.get("class_id")));
        dto.setLength(TypeConverter.toDouble(row.get("length")));
        dto.setReverse(TypeConverter.toDouble(row.get("reverse")));
        dto.setMaxspeedForward(TypeConverter.toInteger(row.get("maxspeed_forward")));
        dto.setMaxspeedBackward(TypeConverter.toInteger(row.get("maxspeed_backward")));
        dto.setPriority(TypeConverter.toDouble(row.get("priority")));
        dto.setGeojson(row.get("geojson") != null ? row.get("geojson").toString() : null);
        return dto;
    }
}
