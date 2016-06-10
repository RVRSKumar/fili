// Copyright 2016 Yahoo Inc.
// Licensed under the terms of the Apache license. Please see LICENSE file distributed with this work for terms.
package com.yahoo.bard.webservice.druid.client;

import com.fasterxml.jackson.databind.JsonNode;


/**
 * Callback from the async HTTP client on success.
 */
public interface SuccessCallback {
    void invoke(JsonNode rootNode);
}
