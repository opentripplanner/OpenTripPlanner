package org.opentripplanner.api.model;


import java.io.Serializable;
import java.util.Objects;

public final class ApiFeedInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    public String id;
    public String publisherName;
    public String publisherUrl;
    public String lang;
    public String startDate;
    public String endDate;
    public String version;

    public String toString() {
        return "<FeedInfo " + id + ">";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }
        ApiFeedInfo that = (ApiFeedInfo) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
