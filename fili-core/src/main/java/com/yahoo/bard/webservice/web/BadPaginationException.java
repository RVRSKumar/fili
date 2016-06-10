// Copyright 2016 Yahoo Inc.
// Licensed under the terms of the Apache license. Please see LICENSE file distributed with this work for terms.
package com.yahoo.bard.webservice.web;

/**
 * Marks that we have experienced a problem with binding and parsing pagination parameters
 */
public class BadPaginationException extends RuntimeException {

    public BadPaginationException(String message) {
        super(message);
    }
}
