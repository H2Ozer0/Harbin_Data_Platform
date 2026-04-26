package com.harbin.dataplatform.service.impl;

import com.harbin.dataplatform.common.util.TypeConverter;
import com.harbin.dataplatform.dto.PageResponse;
import com.harbin.dataplatform.dto.TaxiTripDTO;
import com.harbin.dataplatform.dto.TrajectorySliceDTO;
import com.harbin.dataplatform.repository.TaxiRepository;
import com.harbin.dataplatform.service.TaxiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RealTaxiService implements TaxiService {

    private final TaxiRepository taxiRepository;

    @Value("${app.taxi.min-lon:126.0}")
    private double defaultMinLon;

    @Value("${app.taxi.max-lon:127.2}")
    private double defaultMaxLon;

    @Value("${app.taxi.min-lat:45.4}")
    private double defaultMinLat;

    @Value("${app.taxi.max-lat:46.2}")
    private double defaultMaxLat;

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @Override
    public PageResponse<TaxiTripDTO> getTripsByTimeRange(
            LocalDateTime startTime, LocalDateTime endTime,
            int page, int size
    ) {
        log.info("Querying trips: {} to {}, page={}, size={}",
                startTime.format(FORMATTER), endTime.format(FORMATTER), page, size);

        double startTms = toTimestamp(startTime);
        double endTms = toTimestamp(endTime);

        long total = taxiRepository.countByTimeRange(startTms, endTms);
        List<Map<String, Object>> rows = taxiRepository.findByTimeRange(
                startTms, endTms, size, page * size
        );

        List<TaxiTripDTO> dtoList = rows.stream().map(this::rowToTripDTO).toList();
        return PageResponse.of(dtoList, page, size, total);
    }

    @Override
    public List<TrajectorySliceDTO> getTrajectorySlice(
            LocalDateTime startTime, LocalDateTime endTime,
            Double minLon, Double maxLon, Double minLat, Double maxLat
    ) {
        log.info("Querying trajectory slice: {} to {}, BBOX=[{},{},{},{}]",
                startTime.format(FORMATTER), endTime.format(FORMATTER),
                minLon, maxLon, minLat, maxLat);

        double startTms = toTimestamp(startTime);
        double endTms = toTimestamp(endTime);

        double bMinLon = minLon != null ? minLon : defaultMinLon;
        double bMaxLon = maxLon != null ? maxLon : defaultMaxLon;
        double bMinLat = minLat != null ? minLat : defaultMinLat;
        double bMaxLat = maxLat != null ? maxLat : defaultMaxLat;

        List<Map<String, Object>> rows = taxiRepository.findTrajectorySlice(
                startTms, endTms,
                bMinLon, bMaxLon, bMinLat, bMaxLat,
                10000
        );

        List<TrajectorySliceDTO> slices = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            TrajectorySliceDTO slice = new TrajectorySliceDTO();
            slice.setDevid(TypeConverter.toStringOrDefault(row.get("devid"), ""));
            slice.setLon(TypeConverter.toDouble(row.get("lon")));
            slice.setLat(TypeConverter.toDouble(row.get("lat")));
            slice.setTimestamp(TypeConverter.toLong(row.get("tms")));
            slice.setSeqIndex(TypeConverter.toInteger(row.get("idx")));
            slices.add(slice);
        }

        log.info("Returning {} trajectory slice points", slices.size());
        return slices;
    }

    @Override
    public PageResponse<TaxiTripDTO> getAllTrips(int page, int size) {
        long total = taxiRepository.countByTimeRange(0, Double.MAX_VALUE);
        List<Map<String, Object>> rows = taxiRepository.findAllPaginated(size, page * size);

        List<TaxiTripDTO> dtoList = rows.stream().map(this::rowToTripDTO).toList();
        return PageResponse.of(dtoList, page, size, total);
    }

    @Override
    public long countTripsByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        double startTms = toTimestamp(startTime);
        double endTms = toTimestamp(endTime);
        return taxiRepository.countByTimeRange(startTms, endTms);
    }

    private TaxiTripDTO rowToTripDTO(Map<String, Object> row) {
        TaxiTripDTO dto = new TaxiTripDTO();
        dto.setId(TypeConverter.toLong(row.get("id")));
        dto.setDevid(TypeConverter.toStringOrDefault(row.get("devid"), ""));
        dto.setLonSeq(TypeConverter.parseDoubleList(row.get("lon_seq")));
        dto.setLatSeq(TypeConverter.parseDoubleList(row.get("lat_seq")));
        dto.setTmsSeq(TypeConverter.parseLongList(row.get("tms_seq")));
        dto.setStartTime(TypeConverter.toDouble(row.get("start_tms")));
        dto.setEndTime(TypeConverter.toDouble(row.get("end_tms")));
        return dto;
    }

    private double toTimestamp(LocalDateTime dt) {
        return dt.atZone(ZoneId.of("UTC")).toEpochSecond();
    }
}
