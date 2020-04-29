/* This file is based on code copied from project OneBusAway, see the LICENSE file for further information. */
package org.opentripplanner.api.model;

import java.io.Serializable;
import java.util.Objects;

public final class ApiRoute implements Serializable {
    private static final long serialVersionUID = 1L;

    public String id;
    public ApiAgency agency;
    public String shortName;
    public String longName;
    public String mode;
    public int type;
    public String desc;
    public String url;
    public String color;
    public String textColor;
    public int bikesAllowed = 0;
    public Integer sortOrder;
    public String brandingUrl;

    @Override
    public String toString() {
        return "<Route " + id + " " + shortName + ">";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }
        ApiRoute apiRoute = (ApiRoute) o;
        return Objects.equals(id, apiRoute.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
