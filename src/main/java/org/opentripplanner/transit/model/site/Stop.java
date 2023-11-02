/* This file is based on code copied from project OneBusAway, see the LICENSE file for further information. */
package org.opentripplanner.transit.model.site;

import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.model.*;
import org.opentripplanner.transit.model.framework.FeedScopedId;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.TimeZone;
import javax.annotation.Nonnull;
import javax.validation.constraints.NotNull;

/**
 * A place where actual boarding/departing happens. It can be a bus stop on one side of a road or a
 * platform at a train station. Equivalent to GTFS stop location 0 or NeTEx quay.
 */
public final class Stop extends StationElement implements StopLocation {

  public static LocationType fromLocationType(int i) {
    switch (i) {
      case 0:
        return LocationType.STOP;
      case 1:
        return LocationType.STATION;
      case 2:
        return LocationType.ENTRANCE_EXIT;
      case 3:
        return LocationType.NODE;
      case 4:
        return LocationType.BOARDING_AREA;
      default:
        return LocationType.UNSET;
    }
  }

  public enum LocationType {UNSET, STOP, STATION, ENTRANCE_EXIT, NODE, BOARDING_AREA};
  private static final long serialVersionUID = 2L;

  private final Collection<FareZone> fareZones;

  /**
   * Platform identifier for a platform/stop belonging to a station. This should be just the
   * platform identifier (eg. "G" or "3").
   */
  private final String platformCode;

  /**
   * URL to a web page containing information about this particular stop.
   */
  private final String url;

  public LocationType locationType;

  private final TimeZone timeZone;

  /**
   * Used for describing the type of transportation used at the stop. This can be used eg. for
   * deciding how to render a stop when it is used by multiple routes with different vehicle types.
   */
  private final TransitMode vehicleType;

  private HashSet<BoardingArea> boardingAreas;

  public Stop(
      FeedScopedId id,
      String name,
      String code,
      String description,
      WgsCoordinate coordinate,
      WheelChairBoarding wheelchairBoarding,
      StopLevel level,
      String platformCode,
      Collection<FareZone> fareZones,
      String url,
      LocationType locationType,
      TimeZone timeZone,
      TransitMode vehicleType
  ) {
    super(id, name, code, description, coordinate, wheelchairBoarding, level);
    this.platformCode = platformCode;
    this.fareZones = fareZones;
    this.url = url;
    this.locationType = locationType;
    this.timeZone = timeZone;
    this.vehicleType = vehicleType;
  }

  /**
   * Create a minimal Stop object for unit-test use, where the test only care about id, name and
   * coordinate. The feedId is static set to "TEST"
   */
  public static Stop stopForTest(String idAndName, double lat, double lon) {
    return new Stop(
        new FeedScopedId("F", idAndName),
        idAndName,
        idAndName,
        null,
        new WgsCoordinate(lat, lon),
        null,
        null,
        null,
        null,
        null,
        LocationType.STOP,
        null,
        null
    );
  }


  public void addBoardingArea(BoardingArea boardingArea) {
    if (boardingAreas == null) {
      boardingAreas = new HashSet<>();
    }
    boardingAreas.add(boardingArea);
  }

  @Override
  public String toString() {
    return "<Stop " + getId() + ">";
  }

  public String getPlatformCode() {
    return platformCode;
  }

  /**
   * This is to ensure backwards compatibility with the REST API, which expects the GTFS zone_id
   * which only permits one zone per stop.
   */
  public String getFirstZoneAsString() {
    return fareZones.stream().map(t -> t.getId().getId()).findFirst().orElse(null);
  }

  public String getUrl() {
    return url;
  }

  public LocationType getLocationType() {
    return locationType;
  }

  public TimeZone getTimeZone() {
    return timeZone;
  }

  public TransitMode getVehicleType() {
    return vehicleType;
  }

  public Collection<BoardingArea> getBoardingAreas() {
    return boardingAreas != null ? boardingAreas : Collections.emptySet();
  }

  /**
   * Get the transfer cost priority for Stop. This will fetch the value from the parent
   * [if parent exist] or return the default value.
   */
  @NotNull
  public StopTransferPriority getPriority() {
    return isPartOfStation() ? getParentStation().getPriority() : StopTransferPriority.ALLOWED;
  }

  public Collection<FareZone> getFareZones() {
    return Collections.unmodifiableCollection(fareZones);
  }

  @Override
  public boolean isLine() {
	return false;
  }
	
  @Override
  public boolean isArea() {
	return false;
  }

  @Override
  @Nonnull
  public Geometry getGeometry() {
    return GeometryUtils.getGeometryFactory().createPoint(getCoordinate().asJtsCoordinate());
  }
}
