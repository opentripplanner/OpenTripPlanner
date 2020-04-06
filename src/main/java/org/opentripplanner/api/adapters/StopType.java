package org.opentripplanner.api.adapters;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Station;
import org.opentripplanner.model.Stop;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.List;

@XmlRootElement(name = "Stop")
public class StopType {

    public StopType() {
    }

    public StopType(Stop stop) {
        this.id = stop.getId();
        this.stopLat = stop.getLat();
        this.stopLon = stop.getLon();
        this.stopCode = stop.getCode();
        this.stopName = stop.getName();
        this.stopDesc = stop.getDescription();
        this.zoneId = stop.getZone();
        this.stopUrl = stop.getUrl();
        this.locationType = 0;
        this.parentStation = stop.getParentStation().getId().getId();
        this.wheelchairBoarding = stop.getWheelchairBoarding().gtfsCode;
    }

    public StopType(Station station) {
        this.id = station.getId();
        this.stopLat = station.getLat();
        this.stopLon = station.getLon();
        this.stopCode = station.getCode();
        this.stopName = station.getName();
        this.stopDesc = station.getDescription();
        this.stopUrl = station.getUrl();
        this.locationType = 1;
    }

    public StopType(Stop stop, Boolean extended) {
        this.id = stop.getId();
        this.stopLat = stop.getLat();
        this.stopLon = stop.getLon();
        this.stopCode = stop.getCode();
        this.stopName = stop.getName();
        if (extended != null && extended.equals(true)) {
            this.stopDesc = stop.getDescription();
            this.zoneId = stop.getZone();
            this.stopUrl = stop.getUrl();
            this.locationType = 0;
            this.parentStation = stop.getParentStation().getId().getId();
            this.wheelchairBoarding = stop.getWheelchairBoarding().gtfsCode;
        }
    }

    @XmlJavaTypeAdapter(AgencyAndIdAdapter.class)
    @JsonSerialize
    public FeedScopedId id;

    @XmlAttribute
    @JsonSerialize
    public String stopName;

    @XmlAttribute
    @JsonSerialize
    public Double stopLat;

    @XmlAttribute
    @JsonSerialize
    public Double stopLon;

    @XmlAttribute
    @JsonSerialize
    public String stopCode;

    @XmlAttribute
    @JsonSerialize
    public String stopDesc;

    @XmlAttribute
    @JsonSerialize
    public String zoneId;

    @XmlAttribute
    @JsonSerialize
    public String stopUrl;

    @XmlAttribute
    @JsonSerialize
    public Integer locationType;

    @XmlAttribute
    @JsonSerialize
    public String parentStation;

    @XmlAttribute
    @JsonSerialize
    public Integer wheelchairBoarding;

    @XmlAttribute
    @JsonSerialize
    public String direction;

    @XmlElements(value = @XmlElement(name = "route"))
    public List<FeedScopedId> routes;

}
