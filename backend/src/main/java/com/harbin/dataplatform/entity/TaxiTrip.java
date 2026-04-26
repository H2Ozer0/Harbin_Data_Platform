package com.harbin.dataplatform.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "ods_taxi_trips_raw", schema = "ods")
public class TaxiTrip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "lon_seq", columnDefinition = "double precision[]")
    private String lonSeq;

    @Column(name = "lat_seq", columnDefinition = "double precision[]")
    private String latSeq;

    @Column(name = "tms_seq", columnDefinition = "double precision[]")
    private String tmsSeq;

    @Column(name = "devid")
    private String devid;

    @Column(name = "roads_seq", columnDefinition = "bigint[]")
    private String roadsSeq;

    @Column(name = "time_seq", columnDefinition = "bigint[]")
    private String timeSeq;

    @Column(name = "frac_seq", columnDefinition = "double precision[]")
    private String fracSeq;

    @Column(name = "route_ids", columnDefinition = "bigint[]")
    private String routeIds;

    @Column(name = "route_headings", columnDefinition = "text[]")
    private String routeHeadings;

    @Column(name = "route_geoms", columnDefinition = "geometry[]")
    private String routeGeoms;

    @Transient
    private LocalDateTime startTime;

    @Transient
    private LocalDateTime endTime;
}
