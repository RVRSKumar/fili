// Copyright 2016 Yahoo Inc.
// Licensed under the terms of the Apache license. Please see LICENSE file distributed with this work for terms.
package com.yahoo.bard.webservice.util;

import com.yahoo.bard.webservice.data.dimension.Dimension;
import com.yahoo.bard.webservice.druid.model.query.DruidAggregationQuery;
import com.yahoo.bard.webservice.web.DataApiRequest;

import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *  Methods to support table operations and table resolution
 */
public class TableUtils {
    /**
     * Get the fact store column names from the dimensions and metrics
     *
     * @param request A request which supplies grouping dimensions and filtering dimensions
     * @param query A query model which has metric column and possibly dimension column names
     *
     * @return a set of strings representing fact store column names
     */
    public static Set<String> getColumnNames(DataApiRequest request, DruidAggregationQuery<?> query) {
        return Stream.<Stream<String>>of(
                getDimensions(request, query).map(Dimension::getDruidName),
                query.getDependentFieldNames().stream()
        ).flatMap(Function.identity()).collect(Collectors.toSet());
    }

    /**
     * Get the fact store dimension column names from the dimensions and metrics
     *
     * @param request A request which supplies grouping dimensions and filtering dimensions
     * @param query A query model which may have dimension column names
     *
     * @return a set of strings representing fact store column names
     */
    public static Set<String> getDimensionColumnNames(DataApiRequest request, DruidAggregationQuery<?> query) {
        return getDimensions(request, query)
                .map(Dimension::getDruidName)
                .collect(Collectors.toSet());
    }

    /**
     * Get a stream returning all the fact store dimensions
     *
     * @param request A request which supplies grouping dimensions and filtering dimensions
     * @param query A query model which may have dimension column names
     *
     * @return a set of strings representing fact store column names
     */
    public static Stream<Dimension> getDimensions(DataApiRequest request, DruidAggregationQuery<?> query) {
        return Stream.<Stream<Dimension>>of(
                request.getDimensions().stream(),
                request.getFilterDimensions().stream(),
                query.getMetricDimensions().stream()
        ).flatMap(Function.identity());
    }
}
