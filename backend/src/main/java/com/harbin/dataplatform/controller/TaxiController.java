package com.harbin.dataplatform.controller;

import com.harbin.dataplatform.dto.PageResponse;
import com.harbin.dataplatform.dto.TaxiTripDTO;
import com.harbin.dataplatform.dto.TrajectorySliceDTO;
import com.harbin.dataplatform.service.TaxiService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/taxi")
@RequiredArgsConstructor
@CrossOrigin
public class TaxiController {

    private final TaxiService taxiService;

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @GetMapping("/trips")
    public ResponseEntity<PageResponse<TaxiTripDTO>> getTrips(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime startTime,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime endTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        validateTimeRange(startTime, endTime);
        validatePageSize(page, size);
        return ResponseEntity.ok(taxiService.getTripsByTimeRange(startTime, endTime, page, size));
    }

    @GetMapping("/trajectory")
    public ResponseEntity<List<TrajectorySliceDTO>> getTrajectorySlice(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime startTime,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime endTime,
            @RequestParam(required = false) Double minLon,
            @RequestParam(required = false) Double maxLon,
            @RequestParam(required = false) Double minLat,
            @RequestParam(required = false) Double maxLat
    ) {
        validateTimeRange(startTime, endTime);
        validateBbox(minLon, maxLon, minLat, maxLat);
        List<TrajectorySliceDTO> slices = taxiService.getTrajectorySlice(
                startTime, endTime, minLon, maxLon, minLat, maxLat
        );
        return ResponseEntity.ok(slices);
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getTripStats(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime startTime,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime endTime
    ) {
        validateTimeRange(startTime, endTime);
        long count = taxiService.countTripsByTimeRange(startTime, endTime);
        return ResponseEntity.ok(Map.of(
                "startTime", startTime.format(FORMATTER),
                "endTime", endTime.format(FORMATTER),
                "tripCount", count
        ));
    }

    @GetMapping("/all")
    public ResponseEntity<PageResponse<TaxiTripDTO>> getAllTrips(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        validatePageSize(page, size);
        return ResponseEntity.ok(taxiService.getAllTrips(page, size));
    }

    private void validateTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("startTime and endTime are required");
        }
        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("endTime must be after startTime");
        }
    }

    private void validatePageSize(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("page must be >= 0");
        }
        if (size <= 0 || size > 1000) {
            throw new IllegalArgumentException("size must be between 1 and 1000");
        }
    }

    private void validateBbox(Double minLon, Double maxLon, Double minLat, Double maxLat) {
        if (minLon != null && maxLon != null && minLon > maxLon) {
            throw new IllegalArgumentException("minLon must be <= maxLon");
        }
        if (minLat != null && maxLat != null && minLat > maxLat) {
            throw new IllegalArgumentException("minLat must be <= maxLat");
        }
    }
}
