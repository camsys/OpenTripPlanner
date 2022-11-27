package org.opentripplanner.api.model;

public class ApiRouteShort {
    public String id;
    public String shortName;
    public String longName;
    public String mode;
    public String color;
    public String agencyName;
    public String agencyId;

    @Override
    public String toString() {
        return "<Route " + id + " " + shortName + ">";
    }
}
