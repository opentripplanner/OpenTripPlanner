package org.opentripplanner.api.adapters;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.Trip;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@XmlRootElement(name = "trip")
public class TripType {

    @SuppressWarnings("deprecation")
    public TripType(Trip obj) {
        this.id = obj.getId();
        this.serviceId = obj.getServiceId();
        this.tripShortName = obj.getTripShortName();
        this.tripHeadsign = obj.getTripHeadsign();
        this.routeId = obj.getRoute().getId();
        this.directionId = obj.getDirectionId();
        this.blockId = obj.getBlockId();
        this.shapeId = obj.getShapeId();
        this.wheelchairAccessible = obj.getWheelchairAccessible();
        this.tripBikesAllowed = obj.getTripBikesAllowed();
        this.bikesAllowed = obj.getBikesAllowed();
        this.route = obj.getRoute();
    }

    @SuppressWarnings("deprecation")
    public TripType(Trip obj, Boolean extended) {
        this.id = obj.getId();
        this.tripShortName = obj.getTripShortName();
        this.tripHeadsign = obj.getTripHeadsign();
        if (extended != null && extended.equals(true)) {
            this.route = obj.getRoute();
            this.serviceId = obj.getServiceId();
            this.routeId = obj.getRoute().getId();
            this.directionId = obj.getDirectionId();
            this.blockId = obj.getBlockId();
            this.shapeId = obj.getShapeId();
            this.wheelchairAccessible = obj.getWheelchairAccessible();
            this.tripBikesAllowed = obj.getTripBikesAllowed();
            this.bikesAllowed = obj.getBikesAllowed();
        }
    }

    public TripType() {
    }

    @XmlJavaTypeAdapter(AgencyAndIdAdapter.class)
    @JsonSerialize
    FeedScopedId id;

    @XmlJavaTypeAdapter(AgencyAndIdAdapter.class)
    @JsonSerialize
    FeedScopedId serviceId;

    @XmlAttribute
    @JsonSerialize
    String tripShortName;

    @XmlAttribute
    @JsonSerialize
    String tripHeadsign;

    @XmlJavaTypeAdapter(AgencyAndIdAdapter.class)
    @JsonSerialize
    FeedScopedId routeId;

    @XmlAttribute
    @JsonSerialize
    String directionId;

    @XmlAttribute
    @JsonSerialize
    String blockId;

    @XmlJavaTypeAdapter(AgencyAndIdAdapter.class)
    @JsonSerialize
    FeedScopedId shapeId;

    @XmlAttribute
    @JsonSerialize
    Integer wheelchairAccessible;

    @XmlAttribute
    @JsonSerialize
    Integer tripBikesAllowed;
    
    @XmlAttribute
    @JsonSerialize
    Integer bikesAllowed;

    Route route;

    public Route getRoute() {
        return route;
    }
}
