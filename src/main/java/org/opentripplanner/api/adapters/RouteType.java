package org.opentripplanner.api.adapters;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.opentripplanner.api.mapping.FeedScopedIdMapper;
import org.opentripplanner.api.model.ApiFeedScopedId;
import org.opentripplanner.model.Route;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
   Holds data about a GTFS route from routes.txt.  Data includes id,
   short name, long name, color, etc.
*/

@XmlRootElement(name = "route")
public class RouteType {

    public RouteType() {
    }

    public RouteType(Route route) {
        this.id = FeedScopedIdMapper.mapToApi(route.getId());
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
        this.id = FeedScopedIdMapper.mapToApi(route.getId());
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
    

    @JsonSerialize
    public ApiFeedScopedId id;

    @JsonSerialize
    public ApiFeedScopedId serviceId;

    @XmlAttribute
    @JsonSerialize
    public String routeShortName;

    @XmlAttribute
    @JsonSerialize
    public String routeLongName;

    @XmlAttribute
    @JsonSerialize
    public String routeDesc;

    @XmlAttribute
    @JsonSerialize
    public String routeUrl;

    @XmlAttribute
    @JsonSerialize
    public String routeColor;

    @XmlAttribute
    @JsonSerialize
    public Integer routeType;

    @XmlAttribute
    @JsonSerialize
    public String routeTextColor;

    @XmlAttribute
    @JsonSerialize
    public Integer routeBikesAllowed;
}
