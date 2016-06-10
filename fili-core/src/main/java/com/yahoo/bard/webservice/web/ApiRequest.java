// Copyright 2016 Yahoo Inc.
// Licensed under the terms of the Apache license. Please see LICENSE file distributed with this work for terms.
package com.yahoo.bard.webservice.web;

import static com.yahoo.bard.webservice.web.ErrorMessageFormat.ACCEPT_FORMAT_INVALID;
import static com.yahoo.bard.webservice.web.ErrorMessageFormat.UNKNOWN_GRANULARITY;

import com.yahoo.bard.webservice.config.SystemConfig;
import com.yahoo.bard.webservice.config.SystemConfigProvider;
import com.yahoo.bard.webservice.data.time.TimeContext;
import com.yahoo.bard.webservice.druid.model.query.Granularity;
import com.yahoo.bard.webservice.util.GranularityParseException;
import com.yahoo.bard.webservice.util.Pagination;
import com.yahoo.bard.webservice.web.util.PaginationLink;
import com.yahoo.bard.webservice.web.util.PaginationParameters;

import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

import javax.validation.constraints.NotNull;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

/**
 * API Request. Abstract class offering default implementations for the common components of API request objects.
 */
public abstract class ApiRequest {
    private static final Logger LOG = LoggerFactory.getLogger(ApiRequest.class);
    private static final SystemConfig SYSTEM_CONFIG = SystemConfigProvider.getInstance();

    private static final int DEFAULT_PER_PAGE = SYSTEM_CONFIG.getIntProperty(
            SYSTEM_CONFIG.getPackageVariableName("default_per_page")
    );
    private static final int DEFAULT_PAGE = 1;
    private static final PaginationParameters DEFAULT_PAGINATION = new PaginationParameters(
            DEFAULT_PER_PAGE,
            DEFAULT_PAGE
    );

    public static final String COMMA_AFTER_BRACKET_PATTERN = "(?<=]),";

    protected final ResponseFormatType format;
    protected final Optional<PaginationParameters> paginationParameters;
    protected final UriInfo uriInfo;
    protected final Response.ResponseBuilder builder;
    protected Pagination<?> pagination;

    /**
     * Parses the API request URL and generates the API request object.
     *
     * @param format  response data format JSON or CSV. Default is JSON.
     * @param perPage  number of rows to display per page of results. If present in the original request, must be a
     * positive integer. If not present, must be the empty string.
     * @param page  desired page of results. If present in the original request, must be a positive integer. If not
     * present, must be the empty string.
     * @param uriInfo  The URI of the request object.
     *
     * @throws BadApiRequestException if pagination parameters in the API request are not positive integers.
     */
    public ApiRequest(
            String format,
            @NotNull String perPage,
            @NotNull String page,
            UriInfo uriInfo
    ) {
        this.uriInfo = uriInfo;
        this.format = generateAcceptFormat(format);
        this.paginationParameters = generatePaginationParameters(perPage, page);
        this.builder = Response.status(Response.Status.OK);
    }

    /**
     * No argument constructor, meant to be used only for testing.
     */
    @ForTesting
    protected ApiRequest() {
        this.uriInfo = null;
        this.format = null;
        this.paginationParameters = null;
        this.builder = Response.status(Response.Status.OK);
    }

    /**
     * All argument constructor, meant to be used for rewriting apiRequest.
     *
     * @param format  The format of the response
     * @param paginationParameters  The parameters used to describe pagination
     * @param uriInfo  The uri details
     * @param builder  The response builder for this request
     */
    protected ApiRequest(
            ResponseFormatType format,
            Optional<PaginationParameters> paginationParameters,
            UriInfo uriInfo,
            Response.ResponseBuilder builder
    ) {
        this.format = format;
        this.paginationParameters = paginationParameters;
        this.uriInfo = uriInfo;
        this.builder = builder;
    }

    /**
     * Generate a Granularity instance based on a path element.
     *
     * @param granularity  A string representation of the granularity
     * @param dateTimeZone  The time zone to use for this granularity
     * @param timeContext  The time context to parse this granularity
     *
     * @return A granularity instance with time zone information
     * @throws BadApiRequestException if the string matches no meaningful granularity
     */
    protected Granularity generateGranularity(
            @NotNull String granularity,
            @NotNull DateTimeZone dateTimeZone,
            @NotNull TimeContext timeContext
    ) {
        try {
            return timeContext.parseGranularity(granularity, dateTimeZone);
        } catch (GranularityParseException e) {
            LOG.error(UNKNOWN_GRANULARITY.logFormat(granularity), granularity);
            throw new BadApiRequestException(e.getMessage());
        }
    }

    /**
     * Generate a Granularity instance based on a path element.
     *
     * @param granularity  A string representation of the granularity
     * @param timeContext  The time context to parse this granularity
     *
     * @return A granularity instance without time zone information
     * @throws BadApiRequestException if the string matches no meaningful granularity
     */
    protected Granularity generateGranularity(String granularity, TimeContext timeContext) {
        try {
            return timeContext.parseGranularity(granularity);
        } catch (GranularityParseException e) {
            LOG.error(UNKNOWN_GRANULARITY.logFormat(granularity), granularity);
            throw new BadApiRequestException(e.getMessage());
        }
    }

    /**
     * Given a single dimension filter string, generate a metric name extension.
     *
     * @param filterString  Single dimension filter string.
     *
     * @return Metric name extension created for the filter.
     */
    protected String generateMetricName(String filterString) {
        return filterString.replace("|", "_").replace("-", "_").replace(",", "_").replace("]", "").replace("[", "_");
    }

    /**
     * Generates the format in which the response data is expected.
     *
     * @param format  Expects a URL format query String.
     *
     * @return Response format type (CSV or JSON).
     * @throws BadApiRequestException if the requested format is not found.
     */
    protected ResponseFormatType generateAcceptFormat(String format) {
        try {
            return format == null ?
                    ResponseFormatType.JSON :
                    ResponseFormatType.valueOf(format.toUpperCase(Locale.ENGLISH));
        } catch (IllegalArgumentException e) {
            LOG.error(ACCEPT_FORMAT_INVALID.logFormat(format), e);
            throw new BadApiRequestException(ACCEPT_FORMAT_INVALID.format(format));
        }
    }

    /**
     * Builds the paginationParameters object, if the request provides both a perPage and page field.
     *
     * @param perPage  The number of rows per page.
     * @param page  The page to display.
     *
     * @return An Optional wrapping a PaginationParameters if both 'perPage' and 'page' exist.
     * @throws BadApiRequestException if 'perPage' or 'page' is not a positive integer, or if either one is empty
     * string but not both.
     */
    protected Optional<PaginationParameters> generatePaginationParameters(String perPage, String page) {
        try {
            return "".equals(perPage) && "".equals(page) ?
                    Optional.empty() :
                    Optional.of(new PaginationParameters(perPage, page));
        } catch (BadPaginationException invalidParameters) {
            throw new BadApiRequestException(invalidParameters.getMessage());
        }
    }

    /**
     * Get the type of the requested response format.
     *
     * @return The format of the response for this API request.
     */
    public ResponseFormatType getFormat() {
        return format;
    }

    /**
     * Get the requested pagination parameters.
     *
     * @return The pagination parameters for this API request
     */
    public Optional<PaginationParameters> getPaginationParameters() {
        return paginationParameters;
    }

    /**
     * Get the uri info.
     *
     * @return The uri info of this API request
     */
    public UriInfo getUriInfo() {
        return uriInfo;
    }

    /**
     * Get the pagination object associated with this request.
     * This object has valid contents after a call to {@link #getPage}
     *
     * @return The pagination object.
     */
    public Pagination<?> getPagination() {
        return pagination;
    }

    /**
     * Get the response builder associated with this request.
     *
     * @return The response builder.
     */
    public Response.ResponseBuilder getBuilder() {
        return builder;
    }

    /**
     * Get the default pagination parameters for this type of API request.
     *
     * @return The uri info of this type of API request
     */
    protected PaginationParameters getDefaultPagination() {
        return DEFAULT_PAGINATION;
    }

    /**
     * Add page links to the header of the response builder.
     *
     * @param link  The type of the link to add.
     * @param pages  The paginated set of results containing the pages being linked to.
     */
    protected void addPageLink(PaginationLink link, Pagination<?> pages) {
        link.getPage(pages).ifPresent(page -> addPageLink(link, page));
    }

    /**
     * Add page links to the header of the response builder.
     *
     * @param link  The type of the link to add.
     * @param pageNumber  Number of the page to add the link for.
     */
    protected void addPageLink(PaginationLink link, int pageNumber) {
        UriBuilder uriBuilder = uriInfo.getRequestUriBuilder().replaceQueryParam("page", pageNumber);
        builder.header(HttpHeaders.LINK, Link.fromUriBuilder(uriBuilder).rel(link.getHeaderName()).build());
    }

    /**
     * Add links to the response builder and return a stream with the requested page from the raw data.
     *
     * @param <T>  The type of the collection elements
     * @param data  The data to be paginated.
     *
     * @return A stream corresponding to the requested page.
     */
    public <T> Stream<T> getPage(Collection<T> data) {
        // To avoid several explicit casts after type erasure, use a local pagination object in this method
        Pagination<T> local = new Pagination<>(data, getPaginationParameters().orElse(getDefaultPagination()));
        this.pagination = local;

        Arrays.stream(PaginationLink.values()).forEachOrdered(link -> addPageLink(link, local));

        return local.getPageOfData().stream();
    }
}
