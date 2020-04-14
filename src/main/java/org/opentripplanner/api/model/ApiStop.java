/* This file is based on code copied from project OneBusAway, see the LICENSE file for further information. */
package org.opentripplanner.api.model;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Objects;

public class ApiStop implements Serializable {
    private static final long serialVersionUID = 1L;

    public String id;
    public String name;
    public Double lat;
    public Double lon;
    public String code;
    public String desc;
    public String zoneId;
    public String url;
    public Integer locationType;

    /** The fully qualified parent station id including the feedId. */
    public String stationId;

    /** @deprecated Use "stationId" instead */
    @Deprecated
    public String parentStation;
    public Integer wheelchairBoarding;
    public String direction;
    private String timezone;
    private Integer vehicleType;
    private String platformCode;

    @Override
    public String toString() {
        return "<Stop " + this.id + ">";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }
        ApiStop apiStop = (ApiStop) o;
        return id.equals(apiStop.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
