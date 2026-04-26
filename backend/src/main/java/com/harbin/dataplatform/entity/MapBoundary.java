package com.harbin.dataplatform.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "bfmap_ways", schema = "public")
public class MapBoundary {

    @Id
    @Column(name = "gid")
    private Long gid;

    @Column(name = "osm_id")
    private Long osmId;

    @Column(name = "class_id")
    private Integer classId;

    @Column(name = "source")
    private Long source;

    @Column(name = "target")
    private Long target;

    @Column(name = "length")
    private Double length;

    @Column(name = "reverse")
    private Double reverse;

    @Column(name = "maxspeed_forward")
    private Integer maxspeedForward;

    @Column(name = "maxspeed_backward")
    private Integer maxspeedBackward;

    @Column(name = "priority")
    private Double priority;
}
