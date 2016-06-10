// Copyright 2016 Yahoo Inc.
// Licensed under the terms of the Apache license. Please see LICENSE file distributed with this work for terms.
package com.yahoo.bard.webservice.data.volatility;

import com.yahoo.bard.webservice.druid.model.query.DruidAggregationQuery;
import com.yahoo.bard.webservice.table.PhysicalTable;
import com.yahoo.bard.webservice.util.SimplifiedIntervalList;

/**
 * A Volatile Intervals Service determines which requested interval buckets are volatile on a query. Volatile intervals
 * are intervals which may still not be finalized, and thus should not be cached. The normal use case for this is data
 * which is being ingested via real time ingestion, and thus which will be replaced later by new historical segments.
 */
public interface VolatileIntervalsService {

    /**
     * Retrieve a simplified list of intervals aligned to the time buckets of the query which are partially or fully
     * volatile.
     * Volatile intervals should not be cached (in the same way that partial intervals are not cached) but are
     * returned to the user (which partial intervals are typically not). Volatile intervals will typically abut or
     * overlap incomplete data because they are on the forward edge of ingestion.
     *
     * @param query The query whose time bucketed intervals will be checked for volatility
     * @param factSource The fact source table whose intervals are being retrieved
     *
     * @return A simplified interval list of volatile intervals
     */
    SimplifiedIntervalList getVolatileIntervals(DruidAggregationQuery<?> query, PhysicalTable factSource);
}
