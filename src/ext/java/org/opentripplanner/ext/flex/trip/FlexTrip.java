package org.opentripplanner.ext.flex.trip;

import org.opentripplanner.ext.flex.FlexServiceDate;
import org.opentripplanner.ext.flex.flexpathcalculator.FlexPath;
import org.opentripplanner.ext.flex.flexpathcalculator.FlexPathCalculator;
import org.opentripplanner.ext.flex.template.FlexAccessTemplate;
import org.opentripplanner.ext.flex.template.FlexEgressTemplate;
import org.opentripplanner.model.BookingInfo;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.model.TransitEntity;
import org.opentripplanner.model.Trip;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.graphfinder.NearbyStop;

import java.util.Arrays;
import java.util.List;

/**
 * This class represents the different variations of what is considered flexible transit, and its
 * subclasses encapsulates the different business logic, which the different types of services
 * adhere to.
 */
public abstract class FlexTrip extends TransitEntity {

  private static final long serialVersionUID = 8819000771336287893L;

  protected final Trip trip;
  
  protected FlexTripStopTime[] stopTimes;

  public FlexTrip(Trip trip) {
    super(trip.getId());
    this.trip = trip;
  }
  
  public abstract List<FlexAccessTemplate> getFlexAccessTemplates(
      NearbyStop access, FlexServiceDate servicedate, FlexPathCalculator calculator, RoutingRequest request
  );

  public abstract List<FlexEgressTemplate> getFlexEgressTemplates(
      NearbyStop egress, FlexServiceDate servicedate, FlexPathCalculator calculator, RoutingRequest request
  );

  public abstract List<FlexEgressTemplate> getFlexEgressTemplatesForRaptor(
          NearbyStop egress, FlexServiceDate servicedate, FlexPathCalculator calculator, RoutingRequest request
  );

  public FlexTripStopTime getStopTime(int i) {
    return stopTimes[i];
  }

  public FlexTripStopTime[] getStopTimes() {
    return stopTimes;
  }

  public Trip getTrip() {
    return trip;
  }

  public abstract List<StopLocation> getStops();

  public abstract BookingInfo getDropOffBookingInfo(int i);

  public abstract BookingInfo getPickupBookingInfo(int i);
  
  // The 95% CI for travel time on this trip. Use this for connections and other things that 
  // need more certainty about the arrival/departure time.
  public abstract float getSafeTotalTime(FlexPath streetPath, int fromStopIndex, int toStopIndex);

  // The "usual" travel time on this trip. Use this for display and other things that 
  // are supposed to be more the norm vs. the "worst case" scenario.
  public abstract float getMeanTotalTime(FlexPath streetPath, int fromStopIndex, int toStopIndex);

  // Note: This method returns seconds since midnight. departureTime is also seconds since midnight/service date
  public abstract int earliestDepartureTime(int departureTime, int fromStopIndex, int toStopIndex);

  // Note: This method returns seconds since midnight. departureTime is also seconds since midnight/service date
  public abstract int latestArrivalTime(int arrivalTime, int fromStopIndex, int toStopIndex);

  @Override
  public String toString(){
    String out = trip.toString();
    out+= Arrays.stream(stopTimes).map(x->x.stop.getId().toString()).reduce("", (subtotal, element) -> subtotal + element);
    return out;
  }
}
