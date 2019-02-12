package org.opentripplanner.api.adapters;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

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
    String headsign;

    @XmlAttribute
    @JsonSerialize
    Integer numberOfTrips;

    @XmlAttribute
    @JsonSerialize
    String calendarId;
}