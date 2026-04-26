package com.harbin.dataplatform.service.impl;

import com.harbin.dataplatform.dto.PageResponse;
import com.harbin.dataplatform.dto.TaxiTripDTO;
import com.harbin.dataplatform.dto.TrajectorySliceDTO;
import com.harbin.dataplatform.entity.TaxiTrip;
import com.harbin.dataplatform.service.TaxiService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@Profile({"dev", "test"})
@ConditionalOnProperty(name = "app.taxi.mock-enabled", havingValue = "true", matchIfMissing = false)
public class MockTaxiService implements TaxiService {

    @Value("${app.taxi.data-start-date:2015-01-03T00:00:00}")
    private String dataStartDate;

    @Value("${app.taxi.data-end-date:2015-01-07T23:59:59}")
    private String dataEndDate;

    @Value("${app.taxi.min-lon:126.0}")
    private double minLon;

    @Value("${app.taxi.max-lon:127.2}")
    private double maxLon;

    @Value("${app.taxi.min-lat:45.4}")
    private double minLat;

    @Value("${app.taxi.max-lat:46.2}")
    private double maxLat;

    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private final List<TaxiTrip> mockTrips = Collections.synchronizedList(new ArrayList<>());
    private final Random random = new Random(42);

    @PostConstruct
    public void init() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        startDate = LocalDateTime.parse(dataStartDate, formatter);
        endDate = LocalDateTime.parse(dataEndDate, formatter);
        log.info("MockTaxiService initialized: {} to {}", startDate, endDate);
        generateMockTrips();
    }

    private void generateMockTrips() {
        int numTrips = 5000;
        log.info("Generating {} mock taxi trips for Harbin area", numTrips);

        long startEpoch = startDate.atZone(ZoneId.systemDefault()).toEpochSecond() * 1000;
        long endEpoch = endDate.atZone(ZoneId.systemDefault()).toEpochSecond() * 1000;
        long range = endEpoch - startEpoch;

        for (int i = 0; i < numTrips; i++) {
            TaxiTrip trip = new TaxiTrip();
            trip.setId((long) (i + 1));
            trip.setDevid(String.format("TAXI_%04d", random.nextInt(13000) + 1));

            int numPoints = random.nextInt(80) + 20;
            double[] lons = new double[numPoints];
            double[] lats = new double[numPoints];
            long[] tms = new long[numPoints];

            double tripStart = startEpoch + random.nextLong(range * 3 / 5);
            double startLon = minLon + random.nextDouble() * (maxLon - minLon);
            double startLat = minLat + random.nextDouble() * (maxLat - minLat);

            for (int j = 0; j < numPoints; j++) {
                lons[j] = startLon + (random.nextDouble() - 0.5) * 0.05;
                lats[j] = startLat + (random.nextDouble() - 0.5) * 0.05;
                tms[j] = (long) (tripStart + j * (random.nextDouble() * 60 + 10) * 1000);

                startLon = lons[j];
                startLat = lats[j];
            }

            trip.setLonSeq(arrayToString(lons));
            trip.setLatSeq(arrayToString(lats));
            trip.setTmsSeq(longArrayToString(tms));
            mockTrips.add(trip);
        }

        log.info("Generated {} mock trips successfully", mockTrips.size());
    }

    @Override
    public PageResponse<TaxiTripDTO> getTripsByTimeRange(
            LocalDateTime startTime, LocalDateTime endTime,
            int page, int size
    ) {
        log.info("Mock query trips: {} to {}, page={}, size={}", startTime, endTime, page, size);

        long startMs = startTime.atZone(ZoneId.systemDefault()).toEpochSecond() * 1000;
        long endMs = endTime.atZone(ZoneId.systemDefault()).toEpochSecond() * 1000;

        List<TaxiTrip> filtered = mockTrips.stream()
                .filter(t -> {
                    long[] ts = parseLongArray(t.getTmsSeq());
                    return ts.length > 0 && ts[0] >= startMs && ts[0] <= endMs;
                })
                .toList();

        long total = filtered.size();
        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, filtered.size());

        List<TaxiTripDTO> content = filtered.subList(
                Math.max(0, fromIndex),
                Math.max(0, toIndex)
        ).stream().map(this::toDTO).toList();

        return PageResponse.of(content, page, size, total);
    }

    @Override
    public List<TrajectorySliceDTO> getTrajectorySlice(
            LocalDateTime startTime, LocalDateTime endTime,
            Double minLon, Double maxLon, Double minLat, Double maxLat
    ) {
        log.info("Mock trajectory slice: {} to {}, BBOX=[{},{},{},{}]",
                startTime, endTime, minLon, maxLon, minLat, maxLat);

        long startMs = startTime.atZone(ZoneId.systemDefault()).toEpochSecond() * 1000;
        long endMs = endTime.atZone(ZoneId.systemDefault()).toEpochSecond() * 1000;

        double bMinLon = minLon != null ? minLon : this.minLon;
        double bMaxLon = maxLon != null ? maxLon : this.maxLon;
        double bMinLat = minLat != null ? minLat : this.minLat;
        double bMaxLat = maxLat != null ? maxLat : this.maxLat;

        List<TrajectorySliceDTO> slices = new ArrayList<>();

        for (TaxiTrip trip : mockTrips) {
            double[] lons = parseDoubleArray(trip.getLonSeq());
            double[] lats = parseDoubleArray(trip.getLatSeq());
            long[] tms = parseLongArray(trip.getTmsSeq());

            for (int i = 0; i < tms.length; i++) {
                if (tms[i] >= startMs && tms[i] <= endMs
                        && lons[i] >= bMinLon && lons[i] <= bMaxLon
                        && lats[i] >= bMinLat && lats[i] <= bMaxLat) {
                    TrajectorySliceDTO slice = new TrajectorySliceDTO();
                    slice.setDevid(trip.getDevid());
                    slice.setLon(lons[i]);
                    slice.setLat(lats[i]);
                    slice.setTimestamp(tms[i]);
                    slice.setSeqIndex(i);
                    slices.add(slice);
                }
            }
        }

        log.info("Returning {} trajectory slice points", slices.size());
        return slices;
    }

    @Override
    public PageResponse<TaxiTripDTO> getAllTrips(int page, int size) {
        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, mockTrips.size());

        List<TaxiTripDTO> content = mockTrips.subList(
                Math.max(0, fromIndex),
                Math.max(0, toIndex)
        ).stream().map(this::toDTO).toList();

        return PageResponse.of(content, page, size, mockTrips.size());
    }

    @Override
    public long countTripsByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        long startMs = startTime.atZone(ZoneId.systemDefault()).toEpochSecond() * 1000;
        long endMs = endTime.atZone(ZoneId.systemDefault()).toEpochSecond() * 1000;

        return mockTrips.stream()
                .filter(t -> {
                    long[] ts = parseLongArray(t.getTmsSeq());
                    return ts.length > 0 && ts[0] >= startMs && ts[0] <= endMs;
                })
                .count();
    }

    private TaxiTripDTO toDTO(TaxiTrip trip) {
        TaxiTripDTO dto = new TaxiTripDTO();
        dto.setId(trip.getId());
        dto.setDevid(trip.getDevid());
        dto.setLonSeq(Arrays.stream(parseDoubleArray(trip.getLonSeq()))
                .boxed().toList());
        dto.setLatSeq(Arrays.stream(parseDoubleArray(trip.getLatSeq()))
                .boxed().toList());
        dto.setTmsSeq(Arrays.stream(trip.getTmsSeq() != null ? parseLongArray(trip.getTmsSeq()) : new long[0])
                .boxed().toList());
        long[] tms = parseLongArray(trip.getTmsSeq());
        dto.setStartTime(tms.length > 0 ? (double) tms[0] : null);
        dto.setEndTime(tms.length > 0 ? (double) tms[tms.length - 1] : null);
        return dto;
    }

    private String arrayToString(double[] arr) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arr.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(arr[i]);
        }
        return sb.toString();
    }

    private String longArrayToString(long[] arr) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arr.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(arr[i]);
        }
        return sb.toString();
    }

    private double[] parseDoubleArray(String str) {
        if (str == null || str.isBlank()) return new double[0];
        return Arrays.stream(str.split(","))
                .mapToDouble(Double::parseDouble)
                .toArray();
    }

    private long[] parseLongArray(String str) {
        if (str == null || str.isBlank()) return new long[0];
        return Arrays.stream(str.split(","))
                .mapToLong(Long::parseLong)
                .toArray();
    }
}
