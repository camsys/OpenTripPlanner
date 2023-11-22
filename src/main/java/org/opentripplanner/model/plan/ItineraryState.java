package org.opentripplanner.model.plan;

/**
 * State and Configuration that augments Itinerary.
 */
public class ItineraryState {
  /**
   * The dateTime the search was request to arrive at or depart by.  Useful
   * for sorting itineraries relative to requested time.
   */
  public long dateTime;
  public final float waitWeight;
  public final float transferWeight;
  public final float walkingWeight;
  public final float walkingSpeed;

  public ItineraryState(long dateTime,
                        float waitWeight,
                        float transferWeight,
                        float walkingWeight,
                        float walkingSpeed) {
    this.dateTime = dateTime;
    this.waitWeight = waitWeight;
    this.transferWeight = transferWeight;
    this.walkingWeight = walkingWeight;
    this.walkingSpeed = walkingSpeed;
  }
}
