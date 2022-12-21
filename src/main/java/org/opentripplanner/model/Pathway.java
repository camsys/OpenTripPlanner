/* This file is based on code copied from project OneBusAway, see the LICENSE file for further information. */
package org.opentripplanner.model;

public final class Pathway extends TransitEntity {

    private static final long serialVersionUID = -2404871423254094109L;

    private int pathwayMode;

    private StationElement fromStop;

    private StationElement toStop;

    private String name;

    private String reversedName;

    private int traversalTime;

    private double length;

    private int stairCount;

    private double slope;

    private boolean isBidirectional;

    private int wheelchairTraversalTime;

    public Pathway(FeedScopedId id) {
        super(id);
    }

    public void setPathwayMode(int pathwayMode) {
        this.pathwayMode = pathwayMode;
    }

    public int getPathwayMode() {
        return pathwayMode;
    }

    public void setFromStop(StationElement fromStop) {
        this.fromStop = fromStop;
    }

    public StationElement getFromStop() {
        return fromStop;
    }

    public void setToStop(StationElement toStop) {
        this.toStop = toStop;
    }

    public StationElement getToStop() {
        return toStop;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getReversedName() {
        return reversedName;
    }

    public void setReversedName(String reversedName) {
        this.reversedName = reversedName;
    }

    public void setTraversalTime(int traversalTime) {
        this.traversalTime = traversalTime;
    }

    public int getTraversalTime() {
        return traversalTime;
    }

    public double getLength() {
        return length;
    }

    public void setLength(double length) {
        this.length = length;
    }

    public boolean isBidirectional() {
        return isBidirectional;
    }

    public void setBidirectional(boolean bidirectional) {
        isBidirectional = bidirectional;
    }

    public int getStairCount() {
        return stairCount;
    }

    public void setStairCount(int stairCount) {
        this.stairCount = stairCount;
    }

    public double getSlope() {
        return slope;
    }

    public void setSlope(double slope) {
        this.slope = slope;
    }

    public int getWheelchairTraversalTime() {
        return wheelchairTraversalTime;
    }

    public void setWheelchairTraversalTime(int wheelchairTraversalTime) {
        this.wheelchairTraversalTime = wheelchairTraversalTime;
    }

    @Override
    public String toString() {
        return "<Pathway " + getId() + ">";
    }

    public boolean isPathwayModeWheelchairAccessible() {
        final int STAIRS = org.onebusaway.gtfs.model.Pathway.MODE_STAIRS;
        final int MODE_MOVING_SIDEWALK = org.onebusaway.gtfs.model.Pathway.MODE_MOVING_SIDEWALK;
        final int ESCALATOR = org.onebusaway.gtfs.model.Pathway.MODE_ESCALATOR;

        // legacy wheelchairTraversalTime asserts stop pathway is accessible!
        return getPathwayMode() != STAIRS && getPathwayMode() != ESCALATOR && getPathwayMode() != MODE_MOVING_SIDEWALK
                || getWheelchairTraversalTime() >= 0;
    }
}
