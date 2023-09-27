package org.opentripplanner.ext.fares.model;

import java.util.Objects;

public final class FareContainer {
    private final String id;
    private final String name;

    FareContainer(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (FareContainer) obj;
        return Objects.equals(this.id, that.id) &&
                Objects.equals(this.name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }

    @Override
    public String toString() {
        return "FareContainer[" +
                "id=" + id + ", " +
                "name=" + name + ']';
    }
}
