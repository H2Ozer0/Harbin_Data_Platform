package com.harbin.dataplatform.service;

import com.harbin.dataplatform.dto.PageResponse;
import com.harbin.dataplatform.dto.TaxiTripDTO;
import com.harbin.dataplatform.dto.TrajectorySliceDTO;

import java.time.LocalDateTime;
import java.util.List;

public interface TaxiService {

    PageResponse<TaxiTripDTO> getTripsByTimeRange(
            LocalDateTime startTime, LocalDateTime endTime,
            int page, int size
    );

    List<TrajectorySliceDTO> getTrajectorySlice(
            LocalDateTime startTime, LocalDateTime endTime,
            Double minLon, Double maxLon, Double minLat, Double maxLat
    );

    PageResponse<TaxiTripDTO> getAllTrips(int page, int size);

    long countTripsByTimeRange(LocalDateTime startTime, LocalDateTime endTime);
}
