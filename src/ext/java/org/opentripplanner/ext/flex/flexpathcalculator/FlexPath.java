package org.opentripplanner.ext.flex.flexpathcalculator;

import org.locationtech.jts.geom.LineString;

public class FlexPath {

  public int distanceMeters;

  public int durationSeconds;
  
  public LineString geometry;

  public FlexPath(int distanceMeters, int durationSeconds, LineString geometry) {
    this.distanceMeters = distanceMeters;
    this.durationSeconds = durationSeconds;
    this.geometry = geometry;
  }
}
