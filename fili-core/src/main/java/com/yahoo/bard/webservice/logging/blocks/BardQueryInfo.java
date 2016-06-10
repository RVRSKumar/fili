// Copyright 2016 Yahoo Inc.
// Licensed under the terms of the Apache license. Please see LICENSE file distributed with this work for terms.
package com.yahoo.bard.webservice.logging.blocks;

import com.yahoo.bard.webservice.logging.LogInfo;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

/**
 * Main log of a request served by the TablesServlet.
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class BardQueryInfo implements LogInfo {
    protected final String type;
    protected final boolean cached;

    public BardQueryInfo(String type, boolean cached) {
        this.type = type;
        this.cached = cached;
    }
}
