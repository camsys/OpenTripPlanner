package org.opentripplanner.ext.fares.model;

import java.util.Objects;

public final class RiderCategory {
    private final String id;
    private final String name;
    private final String url;

    public RiderCategory(String id, String name, String url) {
        this.id = id;
        this.name = name;
        this.url = url;
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String url() {
        return url;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (RiderCategory) obj;
        return Objects.equals(this.id, that.id) &&
                Objects.equals(this.name, that.name) &&
                Objects.equals(this.url, that.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, url);
    }

    @Override
    public String toString() {
        return "RiderCategory[" +
                "id=" + id + ", " +
                "name=" + name + ", " +
                "url=" + url + ']';
    }
}
