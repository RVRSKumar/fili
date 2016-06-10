// Copyright 2016 Yahoo Inc.
// Licensed under the terms of the Apache license. Please see LICENSE file distributed with this work for terms.
package com.yahoo.bard.webservice.druid.model.filter;

import java.util.List;

/**
 * Filter for logical AND applied to a set of druid filter expressions
 */
public class AndFilter extends MultiClauseFilter {

    public AndFilter(List<Filter> fields) {
        super(DefaultFilterType.AND, fields);
    }

    public AndFilter(Filter field) {
        super(DefaultFilterType.AND, field);
    }

    @Override
    public AndFilter withFields(List<Filter> fields) {
        return new AndFilter(fields);
    }

    @Override
    public AndFilter plusField(Filter field) {
        List<Filter> fields = getFields();
        fields.add(field);
        return new AndFilter(fields);
    }

    @Override
    public AndFilter plusFields(List<Filter> fields) {
        List<Filter> oldFields = getFields();
        fields.addAll(oldFields);
        return new AndFilter(fields);
    }

    public OrFilter asOrFilter() {
        return new OrFilter(getFields());
    }
}
