package org.opentripplanner.util.aws.cloudwatch.metrics;

import com.amazonaws.services.cloudwatch.model.MetricDatum;

import java.util.Set;

public interface CloudWatchMetrics {
    Set<MetricDatum> getMetrics();
}
