package org.opentripplanner.api.model;

import java.util.Objects;

/**
 * Qualifiers for Fares V2 fare products. Qualifiers can be rider categories (youth, senior, veteran) or
 * a fare container (smart card, app...).
 *
 * @param id
 * @param name
 */
public final class ApiFareQualifier {
    private final String id;
    private final String name;

    public ApiFareQualifier(String id, String name) {
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
        var that = (ApiFareQualifier) obj;
        return Objects.equals(this.id, that.id) &&
                Objects.equals(this.name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }

    @Override
    public String toString() {
        return "ApiFareQualifier[" +
                "id=" + id + ", " +
                "name=" + name + ']';
    }
}
