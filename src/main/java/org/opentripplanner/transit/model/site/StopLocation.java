package org.opentripplanner.transit.model.site;

import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.model.WgsCoordinate;
import org.opentripplanner.transit.model.framework.FeedScopedId;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;

/**
 * A StopLocation describes a place where a vehicle can be boarded or alighted, which is not
 * necessarily a marked stop, but can be of other shapes, such as a service area for flexible
 * transit. StopLocations are referred toin stop times.
 */
public interface StopLocation {

  /** The ID for the StopLocation */
  FeedScopedId getId();

  /** Name of the StopLocation, if provided */
  String getName();

  /** Public facing stop code (short text or number). */
  String getCode();

  /**
   * Representative location for the StopLocation. Can either be the actual location of the stop, or
   * the centroid of an area or line.
   */
  WgsCoordinate getCoordinate();

  /**
   * This is to ensure backwards compatibility with the REST API, which expects the GTFS zone_id
   * which only permits one zone per stop.
   */
  @Nullable
  default String getFirstZoneAsString() {
    for (FareZone t : getFareZones()) {
      return t.getId().getId();
    }
    return null;
  }

  @Nonnull
  default Collection<FareZone> getFareZones() {
    return List.of();
  }

  @Nullable
  Geometry getGeometry();
  
  boolean isLine();

  boolean isArea();
}
