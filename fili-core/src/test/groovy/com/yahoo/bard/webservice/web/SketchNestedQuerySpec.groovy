// Copyright 2016 Yahoo Inc.
// Licensed under the terms of the Apache license. Please see LICENSE file distributed with this work for terms.
package com.yahoo.bard.webservice.web

import static com.yahoo.bard.webservice.config.BardFeatureFlag.INTERSECTION_REPORTING
import static com.yahoo.bard.webservice.druid.model.postaggregation.PostAggregation.DefaultPostAggregationType.SKETCH_ESTIMATE

import com.yahoo.bard.webservice.data.metric.LogicalMetric
import com.yahoo.bard.webservice.data.metric.TemplateDruidQuery
import com.yahoo.bard.webservice.druid.model.aggregation.Aggregation
import com.yahoo.bard.webservice.druid.model.aggregation.DoubleSumAggregation
import com.yahoo.bard.webservice.druid.model.postaggregation.ArithmeticPostAggregation
import com.yahoo.bard.webservice.druid.model.postaggregation.FieldAccessorPostAggregation
import com.yahoo.bard.webservice.druid.model.postaggregation.PostAggregation
import com.yahoo.bard.webservice.druid.model.postaggregation.FuzzySetPostAggregation
import com.yahoo.bard.webservice.druid.model.postaggregation.SketchSetOperationPostAggFunction
import com.yahoo.bard.webservice.druid.model.postaggregation.SketchSetOperationPostAggregation
import com.yahoo.bard.webservice.druid.util.FieldConverterSupplier

import spock.lang.Specification

/**
 * @deprecated  To consider the latest version of sketch Library.
 * This class is replaced by ThetaSketchNestedQuerySpec class
 */
class SketchNestedQuerySpec extends Specification {

    SketchIntersectionReportingResources resources
    boolean intersectionReportingState

    Map<String, Aggregation> aggNameToAggMap = [:]
    Map<String, String> nameMapper = [:]

    def setup() {
        intersectionReportingState = INTERSECTION_REPORTING.isOn()
        INTERSECTION_REPORTING.setOn(true)
        resources = new SketchIntersectionReportingResources().init()
    }

    def cleanup() {
        INTERSECTION_REPORTING.setOn(intersectionReportingState)
    }

    def "updateNestedQueryPostAggs appends the filter to the postAgg name if it is not of the type 'CONSTANT'"() {

        String filterSuffix = "-" + resources.filterObj.get("AND").toString().replaceAll("[^a-zA-Z0-9]", "");
        Set<PostAggregation> nestedQueryPostAggs = resources.dayAvgBcookiesTdq.nestedQuery.getPostAggregations()
        Set<PostAggregation> postAggregations = FieldConverterSupplier.metricsFilterSetBuilder.updateNestedQueryPostAggs(nestedQueryPostAggs, nameMapper, filterSuffix)

        expect:
        postAggregations.sort().first().getName() == "bcookies-countryidinUSINpropertyidin114125" || postAggregations.sort().first().getName() == "one"
        nameMapper.get("bcookies") == "bcookies-countryidinUSINpropertyidin114125"

    }

    def "updateQueryAggs updates the aggs from the outer query if the corresponding post aggs from inner query change"() {
        nameMapper.put("bcookies","bcookies-propertyidin114125countryidinUSIN")
        Set<Aggregation> aggregationSet = FieldConverterSupplier.metricsFilterSetBuilder.updateQueryAggs(resources.dayAvgBcookiesTdq.getAggregations(), nameMapper, aggNameToAggMap)

        expect:
        aggregationSet.first().getName() == "bcookies_sum-propertyidin114125countryidinUSIN"
        aggregationSet.first().getFieldName() == "bcookies-propertyidin114125countryidinUSIN"
        aggNameToAggMap.get("bcookies_sum").getName() == "bcookies_sum-propertyidin114125countryidinUSIN"
        aggNameToAggMap.get("bcookies_sum").getFieldName() == "bcookies-propertyidin114125countryidinUSIN"
    }

    def "replaceOuterPostAggregation replaces the FieldAccessor in the outer query post agg if the corresponding outer agg changes"() {
        aggNameToAggMap.put("bcookies_sum", new DoubleSumAggregation("bcookies_sum-propertyidin114125countryidinUSIN", "bcookies-propertyidin114125countryidinUSIN"))
        ArithmeticPostAggregation pa = FieldConverterSupplier.metricsFilterSetBuilder.replacePostAggWithPostAggFromMap(resources.dayAvgBcookiesTdq.getPostAggregations().first(), aggNameToAggMap)

        expect:
        pa.getFields().first().equals(new FieldAccessorPostAggregation(aggNameToAggMap.get("bcookies_sum")))
    }

    def "Intersection reporting when Logical Metric has nested query"(){
        LinkedHashSet<LogicalMetric> logicalMetrics =  new DataApiRequest().generateLogicalMetrics("dayAvgBcookies(AND(country|id-in[US,IN],property|id-in[114,125]))", resources.metricDict, resources.dimensionDict, resources.table)
        TemplateDruidQuery nestedQuery = logicalMetrics.first().getTemplateDruidQuery().getNestedQuery()

        Set<Aggregation> expectedNestedAggs = new HashSet<>()
        expectedNestedAggs.addAll(resources.bcookieNoYuidFilteredAggregationSet)
        expectedNestedAggs.addAll(resources.bcookieRegBcookiesFilteredAggregationSet)
        nestedQuery.getPostAggregations().first().getName() == "bcookies-propertyidin114125countryidinUSIN"

        PostAggregation updatedNestedPostAgg
        for (PostAggregation pa : nestedQuery.getPostAggregations()) {
            if(pa.getType() == SKETCH_ESTIMATE) {
                updatedNestedPostAgg = pa
                break
            }
        }

        expect:
        nestedQuery.getAggregations().sort().equals(expectedNestedAggs.sort())
        updatedNestedPostAgg.getName() == "bcookies-countryidinUSINpropertyidin114125"
        logicalMetrics.first().getTemplateDruidQuery().getAggregations().first().getName() == "bcookies_sum-countryidinUSINpropertyidin114125"
        logicalMetrics.first().getTemplateDruidQuery().getPostAggregations().first().getName() == "dayAvgBcookies"
    }

    def "metric filter on uniqueIdentifier metric and expect children of unRegBcookies have right sketch operation function"(){
        LinkedHashSet<LogicalMetric> logicalMetrics =  new DataApiRequest().generateLogicalMetrics("uniqueIdentifier(AND(country|id-in[US,IN],property|id-in[14,125]))", resources.metricDict, resources.dimensionDict, resources.table)
        ArithmeticPostAggregation postAggregation = logicalMetrics.first().templateDruidQuery.getPostAggregations().first()

        FuzzySetPostAggregation unRegBcookie1;
        for (PostAggregation pa : postAggregation.getFields()) {
            if(pa instanceof FuzzySetPostAggregation) {
                unRegBcookie1 = pa
                break
            }
        }
        List<SketchSetOperationPostAggregation> unRegBcookieChildren = ((SketchSetOperationPostAggregation) unRegBcookie1.getField()).getFields()

        expect:
        unRegBcookieChildren[0].getFunc().equals(SketchSetOperationPostAggFunction.INTERSECT)
        unRegBcookieChildren[1].getFunc().equals(SketchSetOperationPostAggFunction.UNION)
    }

    def "When metrics of Ratio category are filtered, BadApiException is thrown"() {
        when:
        new DataApiRequest().generateLogicalMetrics("ratioMetric(AND(country|id-in[US,IN],property|id-in[14,125]))", resources.metricDict, resources.dimensionDict, resources.table)

        then:
        Exception e = thrown(BadApiRequestException)
        e.getMessage() == "Metric filtering is not supported for metric 'ratioMetric' as it belongs to Ratios' category"
    }

    def "The dimensions returned from the filtered nested logical metric are correct"() {
        LinkedHashSet<LogicalMetric> logicalMetrics = new DataApiRequest().generateLogicalMetrics("dayAvgBcookies(AND(country|id-in[US,IN],property|id-in[114,125]))", resources.metricDict, resources.dimensionDict, resources.table)

        expect:
        logicalMetrics.first().templateDruidQuery.getMetricDimensions().sort() == [resources.propertyDim, resources.countryDim].sort()
    }
}
