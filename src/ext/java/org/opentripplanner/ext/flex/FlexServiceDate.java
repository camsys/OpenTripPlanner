package org.opentripplanner.ext.flex;

import gnu.trove.set.TIntSet;

import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.routing.graph.Graph;

/**
 * A lightweight wrapper around a serviceDate to hold a services index
 */
public class FlexServiceDate {

  public final ServiceDate serviceDate;

  public final TIntSet servicesRunning;

  FlexServiceDate(ServiceDate serviceDate, TIntSet servicesRunning) {
    this.serviceDate = serviceDate;
    this.servicesRunning = servicesRunning;
  }

  boolean isFlexTripRunning(FlexTrip flexTrip, Graph graph) {
    return servicesRunning != null
        && servicesRunning.contains(graph.getServiceCodes().get(flexTrip.getTrip().getServiceId()));
  }  
}