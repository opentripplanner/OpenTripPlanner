package org.opentripplanner.api.adapters;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

@XmlRootElement(name = "Headsign")
public class HeadsignsInfo implements Serializable {

    private static final long serialVersionUID = -4853941297409355512L;

    public HeadsignsInfo(String headsign, Integer number, String calendarId) {
        this.headsign = headsign;
        this.numberOfTrips = number;
        this.calendarId = calendarId;
    }

    public HeadsignsInfo(String headsign) {
        this.headsign = headsign;
        this.numberOfTrips = 0;
    }

    public HeadsignsInfo() {
    }

    @XmlAttribute
    @JsonSerialize
    public String headsign;

    @XmlAttribute
    @JsonSerialize
    public Integer numberOfTrips;

    @XmlAttribute
    @JsonSerialize
    public String calendarId;
}