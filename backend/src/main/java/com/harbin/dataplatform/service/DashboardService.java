package com.harbin.dataplatform.service;

import com.harbin.dataplatform.dto.*;

import java.util.List;

public interface DashboardService {
    List<CatalogTableDTO> getCatalogTables();
    CatalogFieldsResponse getCatalogFields(String tableName);
    QueryResponse queryCatalog(QueryRequest request);
    List<HotFieldDTO> getHotFields();
    LineageResponse getLineage();
    QualityResponse getQuality();
}
