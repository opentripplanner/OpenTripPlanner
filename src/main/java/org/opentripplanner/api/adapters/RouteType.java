package org.opentripplanner.api.adapters;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Route;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
   Holds data about a GTFS route from routes.txt.  Data includes id,
   short name, long name, color, etc.
*/

@XmlRootElement(name = "route")
public class RouteType {

    public RouteType() {
    }

    public RouteType(Route route) {
        this.id = route.getId();
        this.routeShortName = route.getShortName();
        this.routeLongName = route.getLongName();
        this.routeDesc = route.getDesc();
        this.routeType = route.getType();
        this.routeUrl = route.getUrl();
        this.routeColor = route.getColor();
        this.routeTextColor = route.getTextColor();
        this.routeBikesAllowed = route.getBikesAllowed();
    }

    public RouteType(Route route, Boolean extended) {
        this.id = route.getId();
        this.routeShortName = route.getShortName();
        this.routeType = route.getType();
        this.routeLongName = route.getLongName();
        if (extended != null && extended.equals(true)) {
            this.routeDesc = route.getDesc();
            this.routeType = route.getType();
            this.routeUrl = route.getUrl();
            this.routeColor = route.getColor();
            this.routeTextColor = route.getTextColor();
        }
    }
    
    public FeedScopedId getId(){
        return this.id;
    }

    @XmlJavaTypeAdapter(AgencyAndIdAdapter.class)
    @JsonSerialize
    FeedScopedId id;

    @XmlJavaTypeAdapter(AgencyAndIdAdapter.class)
    @JsonSerialize
    FeedScopedId serviceId;

    @XmlAttribute
    @JsonSerialize
    String routeShortName;

    @XmlAttribute
    @JsonSerialize
    String routeLongName;

    @XmlAttribute
    @JsonSerialize
    String routeDesc;

    @XmlAttribute
    @JsonSerialize
    String routeUrl;

    @XmlAttribute
    @JsonSerialize
    String routeColor;

    @XmlAttribute
    @JsonSerialize
    Integer routeType;

    @XmlAttribute
    @JsonSerialize
    String routeTextColor;

    @XmlAttribute
    @JsonSerialize
    Integer routeBikesAllowed;

}
