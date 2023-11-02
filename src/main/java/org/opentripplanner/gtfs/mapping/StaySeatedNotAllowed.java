package org.opentripplanner.gtfs.mapping;

import org.opentripplanner.model.Trip;

import java.util.Objects;

public final class StaySeatedNotAllowed {
    public final Trip fromTrip;
    public final Trip toTrip;

    StaySeatedNotAllowed(Trip fromTrip, Trip toTrip) {
        this.fromTrip = fromTrip;
        this.toTrip = toTrip;
    }

    public Trip fromTrip() {
        return fromTrip;
    }

    public Trip toTrip() {
        return toTrip;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (StaySeatedNotAllowed) obj;
        return Objects.equals(this.fromTrip, that.fromTrip) &&
                Objects.equals(this.toTrip, that.toTrip);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fromTrip, toTrip);
    }

    @Override
    public String toString() {
        return "StaySeatedNotAllowed[" +
                "fromTrip=" + fromTrip + ", " +
                "toTrip=" + toTrip + ']';
    }
}
