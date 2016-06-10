// Copyright 2016 Yahoo Inc.
// Licensed under the terms of the Apache license. Please see LICENSE file distributed with this work for terms.
package com.yahoo.bard.webservice.druid.model.datasource;

import com.yahoo.bard.webservice.druid.model.query.DruidFactQuery;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Set;

/**
 * QueryDataSource interface
 */
public class QueryDataSource extends DataSource {

    private final DruidFactQuery<?> query;

    public QueryDataSource(DruidFactQuery<?> query) {
        super(DefaultDataSourceType.QUERY);

        this.query = query;
    }

    @Override
    @JsonIgnore
    public Set<String> getNames() {
        return query.getInnermostQuery().getDataSource().getNames();
    }

    @Override
    public DruidFactQuery<?> getQuery() {
        return query;
    }
}
