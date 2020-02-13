package org.opentripplanner.util.aws.cloudwatch.metrics;

import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.MetricDatum;
import com.amazonaws.services.cloudwatch.model.StandardUnit;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class CountMetrics implements CloudWatchMetrics {

    private Set<MetricDatum> metrics = new HashSet<>();

    public void addCountMetric(String metricName, Integer count){
        double countAsDouble = count;
        final MetricDatum datum = new MetricDatum()
                .withMetricName(metricName)
                .withUnit(StandardUnit.Count)
                .withValue(countAsDouble);
        metrics.add(datum);
    }

    public void addCountMetric(String metricName, Integer count, String dimensionName, String dimensionVal){
        Dimension dim = new Dimension()
                .withName(dimensionName)
                .withValue(dimensionVal);
        MetricDatum datum = new MetricDatum()
                .withMetricName(metricName)
                .withValue((double)count)
                .withUnit(StandardUnit.Count)
                .withDimensions(dim);
        metrics.add(datum);
    }

    @Override
    public Set<MetricDatum> getMetrics() {
        return metrics;
    }

}
