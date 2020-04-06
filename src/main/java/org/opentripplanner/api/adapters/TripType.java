package org.opentripplanner.api.adapters;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.Trip;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

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
    public FeedScopedId id;

    @XmlJavaTypeAdapter(AgencyAndIdAdapter.class)
    @JsonSerialize
    public FeedScopedId serviceId;

    @XmlAttribute
    @JsonSerialize
    public String tripShortName;

    @XmlAttribute
    @JsonSerialize
    public String tripHeadsign;

    @XmlJavaTypeAdapter(AgencyAndIdAdapter.class)
    @JsonSerialize
    public FeedScopedId routeId;

    @XmlAttribute
    @JsonSerialize
    public String directionId;

    @XmlAttribute
    @JsonSerialize
    public String blockId;

    @XmlJavaTypeAdapter(AgencyAndIdAdapter.class)
    @JsonSerialize
    public FeedScopedId shapeId;

    @XmlAttribute
    @JsonSerialize
    public Integer wheelchairAccessible;

    @XmlAttribute
    @JsonSerialize
    public Integer tripBikesAllowed;
    
    @XmlAttribute
    @JsonSerialize
    public Integer bikesAllowed;

    public Route route;
}
