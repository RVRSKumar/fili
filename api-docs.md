Fili API Guide
==================

The Fili API provides access to the underlying data that powers Fili.

All interaction with the API is via HTTPS `GET` requests, which means that your entire query is just a URL, making it
easy to save and share queries.

Table of Contents
-----------------

- [Core Concepts](#core-concepts)
    - [Dimensions](#dimensions)
    - [Metrics](#metrics)
    - [Tables](#tables)
    - [Filters](#filters)
    - [Interval](#interval)
    - [Time Grain](#time-grain)
- [Data Queries](#data-queries)
    - [Basics](#basics)
    - [Dimension Breakout](#dimension-breakout)
    - [Filtering Example](#filtering-example)
    - [Response Format Example](#response-format-example)
- [Query Options](#query-options)
    - [Pagination / Limit](#pagination--limit)
    - [Response Format](#response-format)
    - [Filtering](#filtering)
    - [Having](#having)
    - [Sorting](#sorting)
- [Misc](#misc)
    - [Dates and Times](#dates-and-times)
    - [Case Sensitivity](#case-sensitivity)
    - [Rate Limiting](#rate-limiting)
    - [Errors](#errors)

Core Concepts
-------------

There are 5 main concepts in the Fili API:

- [Dimensions](#dimensions)
- [Metrics](#metrics)
- [Tables](#tables)
- [Filters](#filters)
- [Time Grain](#time-grain)
- [Interval](#interval)

### Dimensions ###

Dimensions are the dimensions along which you can slice and dice the data. Dimensions can be used for grouping and
aggregating, as well as filtering of data, and are a critical part of the system. Each dimension has a set of available
fields, as well as a collection of possible values for that dimension. These dimension fields and values serve two
primary purposes: Filtering and Annotating data query results.

All dimensions have an Id property (a natural key) and a Desc property (a human-readable description). Both of these
fields can be used to filter rows reported on, and both of these fields are included in the data query result set for
each dimension.

Get a [list of all dimensions](https://digits3.data.yahoo.com:4443/v1/dimensions):

    GET https://digits3.data.yahoo.com:4443/v1/dimensions

Get a [specific dimension](https://digits3.data.yahoo.com:4443/v1/dimensions/productRegion):

    GET https://digits3.data.yahoo.com:4443/v1/dimensions/productRegion

Get a [list of possible values for a dimension](https://digits3.data.yahoo.com:4443/v1/dimensions/productRegion/values):

    GET https://digits3.data.yahoo.com:4443/v1/dimensions/productRegion/values

Additionally, the values for a dimension have some options for querying them:

- [Pagination](#pagination--limit)
- [Format](#response-format)
- [Filtering](#filtering) (All filters are supported)

For example, to get the [2nd page of User Countries with U in the description, with 5 entries per page, in JSON format](https://digits3.data.yahoo.com:4443/v1/dimensions/userCountry/values?filters=userCountry|desc-contains[U]&page=2&perPage=5&format=json):

    GET https://digits3.data.yahoo.com:4443/v1/dimensions/userCountry/values?filters=userCountry|desc-contains[U]&page=2&perPage=5&format=json

### Metrics ###

Metrics are the data points, and include things like Page Views, Daily Average Time Spent, etc. Since the metrics
available depend on the particular table and time grain, you can discover the available metrics by querying the table
you are interested in, as well as a metrics collection resource that lists all metrics supported by the system.

Get a [list of all metrics](https://digits3.data.yahoo.com:4443/v1/metrics):

    GET https://digits3.data.yahoo.com:4443/v1/metrics
    
Get a [specific metric](https://digits3.data.yahoo.com:4443/v1/metrics/timeSpent):

    GET https://digits3.data.yahoo.com:4443/v1/metrics/timeSpent

### Tables ###

Tables are the connecting point that tell you what combination of [Metrics](#metrics) and [Dimensions](#dimensions) are
available, and at what [time grain](#time-grain). For each table, and a specific time grain, there is a set of Metrics
and Dimensions that are available on that table.

Get a [list of all tables](https://digits3.data.yahoo.com:4443/v1/tables):

    GET https://digits3.data.yahoo.com:4443/v1/tables

Get a [specific table](https://digits3.data.yahoo.com:4443/v1/tables/network/week):

    GET https://digits3.data.yahoo.com:4443/v1/tables/network/week

### Filters ###

Filters allow you to constrain the data processed by the system. Depending on what resource is being requested, filters
may constrain the rows in the response, or may constrain the data that the system is processing. 

For non-Data resource requests, since there isn't any data aggregation happening, filters primarily exclude or include 
rows in the response.

For [Data resource](#data-queries) requests, however, filters primarily exclude or include raw data rows aggregated to
produce a result. In order for filters on the Data resource requests to constrain the _rows in the  response_, the 
request must have a [Dimension Breakout](#dimension-breakout) along the dimension being filtered.

### Interval ###

The Interval (or `dateTime`) of a data query is the date/time range for data to include in the query. The interval is 
expressed as a pair of start and stop instants in time, using the ISO 8601 format, where the start instant is inclusive 
and the end instant is exclusive. It sounds complicated, but it's pretty straight-forward once you get the hang of it.

One important thing to note about intervals is that they must align to the [time grain](#time-grain) specified in the
query. So, if you ask for monthly time grain, the interval must start and stop on month boundaries, and if you ask for 
weekly time grain, the interval must start and stop on a Monday, which is when our week starts and stops.

### Time Grain ###

Time grain is the granularity or "bucket size" of each response row. Or, to look at it another way, time grain is the
period over which metrics are aggregated. In particular, this matters a lot for metrics that are counting "unique" 
things, like Unique Identifier.

Available time grains currently are:

  1. Day
  2. Week
  3. Month

_Note: The finest grain stored in Fili is at the day level, so we can't break the data up into smaller buckets than 1 
day._

Data Queries
------------

Data queries are the meat of the Fili API. The data resource allows for:

- Grouping by [dimensions](#dimensions)
- Grouping by a [time grain](#time-grain)
- [Filtering](#filtering) by dimension values
- Performing [Having](#having) filters on metric values
- Selecting [metrics](#metrics)
- Reporting on a specific [interval](#interval) of time
- [Sorting](#sorting) by a metric within a time grain
- Selecting the [response format](#response-format)

### Basics ###

Let's start by looking at the URL format, using an example:

    GET https://digits3.data.yahoo.com:4443/v1/data/network/week?metrics=pageViews,dayAvgTimeSpent&dateTime=2014-09-01/2014-09-08

This [basic query](https://digits3.data.yahoo.com:4443/v1/data/network/week?metrics=pageViews,dayAvgTimeSpent&dateTime=2014-09-01/2014-09-08)
gives us network-level page views and daily average time spent data for one week. Let's break down the different 
components of this URL.

- **https\://** - The Fili API is only available over a secure connection, so _HTTPS is required_. _HTTP queries
    will not work_.
- **digits3.data.yahoo.com** - This is where the Fili API lives.
- **4443** - Yahoo security standard specifies that internal secure web services use port 4443.
- **v1** - The version of the API.
- **data** - This is the resource we are querying, and is the base for all data reports.
- **network** - Network is the [table](#tables) we are getting data from.
- **week** - The top-level reporting [time grain](#time-grain) of our results. Each row in our response will aggregate a
    week of data.
- **metrics** - The different [metrics](#metrics) we want included in our response, as a comma-separated list of
    metrics. (Note: these are case-sensitive)
- **dateTime** - Indicates the [interval](#interval) that we are running the report over, in
    [ISO 8601 format](#dates-and-times).

### Dimension Breakout ###

Great, we've got the basics! But, what if we want to add a dimension breakout? Perhaps [along the Product Region 
dimension](https://digits3.data.yahoo.com:4443/v1/data/network/week/productRegion?metrics=pageViews,dayAvgTimeSpent&dateTime=2014-09-01/2014-09-08)?

    GET https://digits3.data.yahoo.com:4443/v1/data/network/week/productRegion?metrics=pageViews,dayAvgTimeSpent&dateTime=2014-09-01/2014-09-08

The only difference is that we've added an additional path segment to the URL (`productRegion`). All breakout dimensions
are added as path segments after the [time grain](#time-grain) path segment. To group by more [dimensions](#dimensions),
just add more path segments!

So, if we wanted to [also breakout by `gender`](https://digits3.data.yahoo.com:4443/v1/data/network/week/productRegion/gender?metrics=pageViews,dayAvgTimeSpent&dateTime=2014-09-01/2014-09-08) 
in addition to breaking out by `productRegion` (a 2-dimension breakout):

    GET https://digits3.data.yahoo.com:4443/v1/data/network/week/productRegion/gender?metrics=pageViews,dayAvgTimeSpent&dateTime=2014-09-01/2014-09-08

### Filtering Example ###

Now that we can group by dimensions, can we filter out data that we don't want? Perhaps we want to see [our global
numbers, but excluding the US data](https://digits3.data.yahoo.com:4443/v1/data/network/week?metrics=pageViews,dayAvgTimeSpent&dateTime=2014-09-01/2014-09-08&filters=productRegion|id-notin[Americas+Region])?

    GET https://digits3.data.yahoo.com:4443/v1/data/network/week?metrics=pageViews,dayAvgTimeSpent&dateTime=2014-09-01/2014-09-08&filters=productRegion|id-notin[Americas Region]

We've now added a [filter](#filtering) to our query that excludes (`notin`) rows that have the `Americas Region` id for
the `productRegion` dimension. We also removed the `productRegion` grouping dimension we added earlier, since we wanted 
to see the global numbers, not the per-region numbers. 

This example also shows that we can filter by dimensions that we are not grouping on! Filters are very rich and
powerful, so take a look at the [Filters](#filtering) section for more details. Oh, and one last thing about filters on
the Data resource: Only `in` and `notin` are supported at the moment, but additional filter operations will be added 
soon!

### Response Format Example ###

Now, the very last thing we need from our report: We need it [in CSV format](https://digits3.data.yahoo.com:4443/v1/data/network/week?metrics=pageViews,dayAvgTimeSpent&dateTime=2014-09-01/2014-09-08&filters=productRegion|id-notin[Americas Region]&format=csv),
so that we can pull it into Excel and play around with it! No worries, the Fili API supports CSV!

    GET https://digits3.data.yahoo.com:4443/v1/data/network/week?metrics=pageViews,dayAvgTimeSpent&dateTime=2014-09-01/2014-09-08&filters=productRegion|id-notin[Americas Region]&format=csv

For additional information about response format, take a look at the [Format](#response-format) section!

Query Options
-------------

Many of the resources in the Fili API support different query options. Here are the different options that are
supported, and how the use the options:

- [Pagination / Limit](#pagination--limit)
- [Response Format](#response-format)
- [Filtering](#filtering)
- [Having](#having)
- [Dimension Field Selection](#dimension-field-selection)
- [Sorting](#sorting)

### Pagination / Limit ###

Pagination allows us to split our rows into pages, and then retrieve only the desired page. So rather than getting one 
giant response with a million result rows and then write code to extract rows 5000 to 5999 ourselves, we can use 
pagination to break the response into a thousand pages, each with a thousand result rows, and then ask for page 5.

At this point, only the [Dimension](#dimensions) and [Data](#data-queries) endpoints support pagination.

In addition to containing only the desired page of results, the response also contains pagination metadata. Currently, 
the dimension and data endpoints show different metadata, but there are plans to have the dimension endpoint display
the same kind of metadata as the data endpoint.

To paginate a resource that supports it, there are two query parameters at your disposal:

- **perPage**: How many result rows/resources to have on a page. Takes a positive integer as a parameter.

- **page**: Which page to return, with `perPage` rows per page. Takes a positive integer as a parameter.

With these two parameters, we can, for example, get the [2nd page with 3 records per page](https://digits3.data.yahoo.com:4443/v1/data/network/day?metrics=pageViews&dateTime=2014-09-01/2014-09-08&perPage=3&page=2):

    GET https://digits3.data.yahoo.com:4443/v1/data/network/day?metrics=pageViews&dateTime=2014-09-01/2014-09-08&perPage=3&page=2
   
With all response formats, a `link` header is added to the HTTP response. These are links to the first, last, next, and
previous pages with `rel` attributes `first`, `last`, `next`, and `prev` respectively. If we use our [previous example](https://digits3.data.yahoo.com:4443/v1/data/network/day?metrics=pageViews&dateTime=2014-09-01/2014-09-08&perPage=3&page=2),    
the link header in the response would be:
    
     Link: 
        https://digits3.data.yahoo.com:4443/v1/data/network/day?metrics=pageViews&dateTime=2014-09-01/2014-09-08&perPage=3&page=1; rel="first",
        https://digits3.data.yahoo.com:4443/v1/data/network/day?metrics=pageViews&dateTime=2014-09-01/2014-09-08&perPage=3&page=3; rel="last"
        https://digits3.data.yahoo.com:4443/v1/data/network/day?metrics=pageViews&dateTime=2014-09-01/2014-09-08&perPage=3&page=3; rel="next",
        https://digits3.data.yahoo.com:4443/v1/data/network/day?metrics=pageViews&dateTime=2014-09-01/2014-09-08&perPage=3&page=1; rel="prev",

There are, however, a few differences between pagination for Dimension and Data endpoints:

#### Data #####
    
For JSON (and JSON-API) responses, a `meta` object is included in the body of the response:
 
```json
"meta": {
    "pagination": {
        "currentPage": 2,
        "rowsPerPage": 3,
        "numberOfResults": 7
        "first": "https://digits3.data.yahoo.com:4443/v1/data/network/day?metrics=pageViews&dateTime=2014-09-01/2014-09-08&perPage=3&page=1",
        "previous": "https://digits3.data.yahoo.com:4443/v1/data/network/day?metrics=pageViews&dateTime=2014-09-01/2014-09-08&perPage=3&page=1",
        "next": "https://digits3.data.yahoo.com:4443/v1/data/network/day?metrics=pageViews&dateTime=2014-09-01/2014-09-08&perPage=3&page=3",
        "last": "https://digits3.data.yahoo.com:4443/v1/data/network/day?metrics=pageViews&dateTime=2014-09-01/2014-09-08&perPage=3&page=3"
    }
}
```

The `meta` object contains a `pagination` object which contains links to the `first`, `last`, `next` and `previous`
pages. The `meta` object contains other information as well:

- `currentPage` : The page currently being displayed
- `rowsPerPage` : The number of rows on each page
- `numberOfResults` : The total number of rows in the entire result

_Note: For the data endpoint, **both** the `perPage` and `page` parameters must be provided. The data endpoint has no 
default pagination._

##### Pagination Links #####

When paginating, the `first` and `last` links will always be present, but the `next` and `previous` links will only be
included in the response if there is at least 1 page either after or before the requested page. Or, to put it another
way, the response for the [1st page](https://digits3.data.yahoo.com:4443/v1/data/network/day?metrics=pageViews&dateTime=2014-09-01/2014-09-08&perPage=3&page=1)
won't include a link to the `previous` page, and the response for the [last page](https://digits3.data.yahoo.com:4443/v1/data/network/day?metrics=pageViews&dateTime=2014-09-01/2014-09-08&perPage=3&page=3)
won't include a link to the `next` page.

#### Dimension ####

Currently, the dimension endpoint only prints the `previous` and `next` links inside the top-level JSON object. It does, 
however, include the same links in the headers as the data endpoint: `first`, `last`, `next` and `prev`.

Unlike the Data endpoint, the Dimension endpoint _always_ paginates. It defaults to page 1, and 10000 rows per page. The
default rows per page is configurable, and may be adjusted by modifying the configuration `default_per_page.` 

_Note that `default_per_page` applies **only** to the Dimension endpoint. It does not affect the Data endpoint._

- **perPage**:
    Setting only the `perPage` parameter also gives a "limit" behavior, returning only the top `perPage` rows.

    [Example](https://digits3.data.yahoo.com:4443/v1/dimensions/productRegion/values?perPage=2): `GET https://digits3.data.yahoo.com:4443/v1/dimensions/productRegion/values?perPage=2`
    
    _Note: This will likely change to not return "all" by default in a future version_
    
- **page**:    
    `page` defaults to 1, the first page.

    Note: In order to use `page`, the `perPage` query parameter must also be set.

    [Example](https://digits3.data.yahoo.com:4443/v1/dimensions/productRegion/values?perPage=2&page=2): `GET https://digits3.data.yahoo.com:4443/v1/dimensions/productRegion/values?perPage=2&page=2`

### Response Format ###

Some resources support different response formats. The default response format is JSON, and some resources also support
the CSV and JSON-API formats.

To change the format of a response, use the `format` query string parameter.

[JSON](https://digits3.data.yahoo.com:4443/v1/data/network/day/gender?metrics=pageViews&dateTime=2014-09-01/2014-09-02&format=json): `GET https://digits3.data.yahoo.com:4443/v1/data/network/day/gender?metrics=pageViews&dateTime=2014-09-01/2014-09-02&format=json`

```json
{
    "rows": [
        {
            "dateTime": "2014-09-01 00:00:00.000",
            "gender|id": "-1",
            "gender|desc": "Unknown",
            "pageViews": 1681441753
        },{
            "dateTime": "2014-09-01 00:00:00.000",
            "gender|id": "f",
            "gender|desc": "Female",
            "pageViews": 958894425
        },{
            "dateTime": "2014-09-01 00:00:00.000",
            "gender|id": "m",
            "gender|desc": "Male",
            "pageViews": 1304365910
        }
    ]
}
```

[CSV](https://digits3.data.yahoo.com:4443/v1/data/network/day/gender?metrics=pageViews&dateTime=2014-09-01/2014-09-02&format=csv): `GET https://digits3.data.yahoo.com:4443/v1/data/network/day/gender?metrics=pageViews&dateTime=2014-09-01/2014-09-02&format=csv`

```csv
dateTime,gender|id,gender|desc,pageViews
2014-09-01 00:00:00.000,-1,Unknown,1681441753
2014-09-01 00:00:00.000,f,Female,958894425
2014-09-01 00:00:00.000,m,Male,1304365910
```

[JSON-API](https://digits3.data.yahoo.com:4443/v1/data/network/day/gender?metrics=pageViews&dateTime=2014-09-01/2014-09-02&format=jsonapi): `GET https://digits3.data.yahoo.com:4443/v1/data/network/day/gender?metrics=pageViews&dateTime=2014-09-01/2014-09-02&format=jsonapi`

```json
{
    "rows": [
        {
            "dateTime": "2014-09-01 00:00:00.000",
            "gender": "-1",
            "pageViews": 1681441753
        },{
            "dateTime": "2014-09-01 00:00:00.000",
            "gender": "f",
            "pageViews": 958894425
        },{
            "dateTime": "2014-09-01 00:00:00.000",
            "gender": "m",
            "pageViews": 1304365910
        }
    ],
    "gender": [
        {
            "id": "-1",
            "desc": "Unknown"
        },{
            "id": "f",
            "desc": "Female"
        },{
            "id": "m",
            "desc": "Male"
        }
    ]
}
```

### Filtering ###

Filters allow you to filter by [dimension](#dimension) values. What is being filtered depends on the resource, but the
general format for filters and their logical meaning is the same regardless of resource.

The general format of a single filter is:

    dimensionName|dimensionField-filterOperation[some,list,of,url,encoded,filter,strings]

These filters can be combined by comma-separating each individual filter, and the filter strings are [URL-encoded](http://en.wikipedia.org/wiki/Percent-encoding),
comma-separated values:

    myDim|id-contains[foo,bar],myDim|id-notin[baz],yourDim|desc-startsWith[Once%20upon%20a%20time,in%20a%20galaxy]

These are the available filter operations (Though not all of them are supported by all endpoints):

- **in**: `In` filters are an exact match on a filter string, where only matching rows are included
- **notin**: `Not In` filters are an exact match on a filter string, where all rows _except_ matching rows are included
- **contains**: `Contains` filters search for the filter string to be contained in the searched field, and work like an 
    `in` filter
- **startsWith**: `Starts With` filters search for the filter string to be at the beginning of the searched field, and 
    work like an `in` filter

Let's take an example, and break down what it means.

[Example](https://digits3.data.yahoo.com:4443/v1/dimensions/productRegion/values?filters=productRegion|id-notin[Americas%20Region,EMEA%20Region],productRegion|desc-contains[Region]): 

    GET https://digits3.data.yahoo.com:4443/v1/dimensions/productRegion/values?filters=productRegion|id-notin[Americas%20Region,EMEA%20Region],productRegion|desc-contains[Region]

What this filter parameter means is the following: 

    Return dimension values that 
        don't have 
            productRegion dimension values 
            with an ID of "Americas Region" or "EMEA Region", 
        and have 
            productRegion dimension values 
            with a description that contains "Region".


### Having ###

Having clauses allow you to to filter out result rows based on conditions on aggregated metrics. 
This is similar to, but distinct from [Filtering](#filtering), which allows you to filter out results based on 
dimensions. As a result, the format for writing a having clause is very similar to that of a filter. 

The general format of a single having clause is:

    metricName-operator[x,y,z]
    
where the parameters `x, y, z` are numbers (integer or float) in decimal (`3, 3.14159`) or scientific (`4e8`) notation. 
Although three numbers are used in the template above, the list of parameters may be of any length, but must be 
non-empty.

These clauses can be combined by comma-separating individual clauses:

    metricName1-greaterThan[w,x],metricName2-equals[y],metricName3-notLessThan[y, z]
  
which is read as _return all rows such that metricName1 is greater than w or x, and metricName2 is
equal to y, and metricName3 is less than neither y nor z_.

Note that you may only perform a having filter on metrics that have been requested in the `metrics` clause.

Following are the available having operators. Each operator has an associated shorthand. The shorthand is indicated
in parenthesis after the name of the operator. Both the full name and the shorthand may be used in a query.

- **equal(eq)**: `Equal` returns rows whose having-metric is equal to at least one of the specified values.
- **greaterThan(gt)**: `Greater Than` returns rows whose having-metric is strictly greater than at least one of the
specified values.
- **lessThan(lt)**: `Less Than` returns rows whose having-metric is strictly less than at least one of the specified 
values.

Each operation may also be prefixed with `not`, which negates the operation. So `noteq` returns all the rows whose
having-metric is equal to _none_ of the specified values.

Let's take [an example](https://digits3.data.yahoo.com:4443/v1/data/network/day?metrics=pageViews,regBcookies&dateTime=2014-09-01/2014-09-08&having=pageViews-notgt[4e9],regBcookies-lt[1e8]) and break down what it means.

    GET https://digits3.data.yahoo.com:4443/v1/data/network/day?metrics=pageViews,regBcookies&dateTime=2014-09-01/2014-09-08&having=pageViews-notgt[4e9],regBcookies-lt[1e8]

What this having clause means is the following: 

    Return the page views and registered B cookies of all events aggregated at the day level from September 1 to 
    September 8, 2014 that
         have at most 400 million page views
         and 
         have strictly more than 100 million registered Bcookies
         
#### Caveats ####         

The having filter is only applied at the Druid level. Therefore, the results of a having filter are not guaranteed to
be accurate if Fili performs any post-Druid calculations on one of the metrics being filtered on.
         
### Dimension Field Selection ###

By default, a query's response will return the id and description for each dimension in the request. However, you may be
interested in more information about the dimensions, or less. To do this, you can [specify a `show` clause on the
relevant dimension path segment](https://digits3.data.yahoo.com:4443/v1/data/network/week/productRegion;show=desc/userCountry;show=id,regionId/?metrics=pageViews&dateTime=2014-09-01/2014-09-08):

    GET https://digits3.data.yahoo.com:4443/v1/data/network/week/productRegion;show=desc/userCountry;show=id,regionId/?metrics=pageViews&dateTime=2014-09-01/2014-09-08

The results for this query will only show the description field for the Product Region dimension, and both the id and 
region id fields for the User Country dimension. In general you add `show` to a dimension with a semicolon, then
`show=<fieldnames>`. Use commas to separate in multiple fields in the same show clause:

    /<dimension>;show=<field>,<field>,<field>

#### Field selection keywords ####

There are a couple of keywords that can be used when selecting fields to `show`:

- **All**: Include all fields for the dimension in the response
- **None**: Include only the key field in the dimension. 

The `none` keyword also simplifies the response to keep the size as small as possible. The simplifications applied to
the response depend on the format of the response:

##### JSON #####
Instead of the normal format for each requested field for a dimension (`"dimensionName|fieldName":"fieldValue"`), each 
record in the response will only have a single entry for the dimension who's value is the value of the key-field for
that dimension (`"dimensionName":"keyFieldValue"`)

##### CSV #####
Instead of the normal header format for each requested field for a dimension (`"dimensionName|fieldName":"fieldValue"`),
the headers of the response will only have a single entry for the dimension, which will be the dimension's name. The
values of the column for that dimension will be the key-field for that dimension.

##### JSON-API #####
The `none` keyword for a dimension prevents the sidecar object for that dimension from being included in the response.

            
### Sorting ###

Sorting of the records in a response [can be done](https://digits3.data.yahoo.com:4443/v1/data/network/day/gender?metrics=pageViews&dateTime=2014-09-01/2014-09-02&sort=pageViews|asc)
using the `sort` query parameter like so: 

    sort=myMetric

By default, sorting is _descending_, however Fili supports sorting in both descending or ascending order. To specify the
sort direction for a metric, you need to specify both the metric you want to sort on, as well as the direction, 
separated by a pipe (`|`) like so:

    sort=myMetric|asc

Sorting by multiple metrics, with a mixture of ascending and descending, can also be done by separating each sort with a
comma:

    sort=metric1|asc,metric2|desc,metric3|desc

#### Caveats ####         

There are, however, a few catches to this:

- Only the Data resource supports sorting
- Records are always sorted by `dateTime` first, and then by any sorts specified in the query, so records are always
  sorted _within_ a timestamp
- Sort is only supported on Metrics
- Sorting is only applied at the Druid level. Therefore, the results of a sort are not guaranteed to be accurate if Fili 
performs any post-Druid calculations on one of the metrics that you are sorting on.


Misc
----

### Dates and Times ###

More information is coming soon, but Wikipedia has a [great article](http://en.wikipedia.org/wiki/ISO_8601) on ISO 8601
dates and times if you want to dig deep.

### Case Sensitivity ###

Everything in the API is case-sensitive, which means `pageViews` is not the same as `pageviews` is not the same as 
`PaGeViEwS`.

### Rate Limiting ###

To prevent abuse of the system, the API only allows each user to have a certain number of data requests being processed
at any one time. If you try to make another request that would put you above the allowed limit, you will be given an
error response with an HTTP response status code of 429.  

### Errors ###

There are a number of different errors you may encounter when interacting with the API. All of them are indicated by the
HTTP response status code, and most of them have a helpful message indicating what went wrong.

#### 400 BAD REQUEST ####

You have some sort of syntax error in your request. We couldn't figure out what you were trying to ask.

#### 401 UNAUTHORIZED ####

We don't know who you are, so send your request again, but tell us who you are.

Usually this means you didn't include proper security authentication information in your request

#### 403 FORBIDDEN ####

We know who you are, but you can't come in.

This is usually because you're making a request with a headless user that isn't on the whitelist. If you need to access
the Digit 3 API with a headless user and are getting denied access, shoot us an email and we will happily add your user
to the list.

#### 404 NOT FOUND ####

We don't know what resource you're talking about. You probably have a typo in your URL path.

#### 416 REQUESTED RANGE NOT SATISFIABLE ####

We can't get that data for you from Druid.

#### 422 UNPROCESSABLE ENTITY ####

We understood the request (ie. syntax is correct), but something else about the query is wrong. Likely something like
a dimension mis-match, or a metric / dimension not being available in the logical table.

#### 429 TOO MANY REQUESTS ####

You've hit the rate limit. Wait a little while for any requests you may have sent to finish and try your request again.

#### 500 INTERNAL SERVER ERROR ####

Some other error on our end. We try to catch and investigate all of these, but we might miss them, so please let us know
if you get 500 errors.

#### 502 BAD GATEWAY ####

Bad Druid response

#### 503 SERVICE UNAVAILABLE ####

Druid can't be reached

#### 504 GATEWAY TIMEOUT ####

Druid query timeout

#### 507 INSUFFICIENT STORAGE ####

Too heavy of a query
