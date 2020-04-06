package org.opentripplanner.api.adapters;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.opentripplanner.api.mapping.FeedScopedIdMapper;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.Trip;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "trip")
public class TripType {

    @SuppressWarnings("deprecation")
    public TripType(Trip obj) {
        this.id = FeedScopedIdMapper.mapToApi(obj.getId());
        this.serviceId = FeedScopedIdMapper.mapToApi(obj.getServiceId());
        this.tripShortName = obj.getTripShortName();
        this.tripHeadsign = obj.getTripHeadsign();
        this.routeId = FeedScopedIdMapper.mapToApi(obj.getRoute().getId());
        this.directionId = obj.getDirectionId();
        this.blockId = obj.getBlockId();
        this.shapeId = FeedScopedIdMapper.mapToApi(obj.getShapeId());
        this.wheelchairAccessible = obj.getWheelchairAccessible();
        this.tripBikesAllowed = obj.getTripBikesAllowed();
        this.bikesAllowed = obj.getBikesAllowed();
        this.route = obj.getRoute();
    }

    @SuppressWarnings("deprecation")
    public TripType(Trip obj, Boolean extended) {
        this.id = FeedScopedIdMapper.mapToApi(obj.getId());
        this.tripShortName = obj.getTripShortName();
        this.tripHeadsign = obj.getTripHeadsign();
        if (extended != null && extended.equals(true)) {
            this.route = obj.getRoute();
            this.serviceId = FeedScopedIdMapper.mapToApi(obj.getServiceId());
            this.routeId = FeedScopedIdMapper.mapToApi(obj.getRoute().getId());
            this.directionId = obj.getDirectionId();
            this.blockId = obj.getBlockId();
            this.shapeId = FeedScopedIdMapper.mapToApi(obj.getShapeId());
            this.wheelchairAccessible = obj.getWheelchairAccessible();
            this.tripBikesAllowed = obj.getTripBikesAllowed();
            this.bikesAllowed = obj.getBikesAllowed();
        }
    }

    public TripType() {
    }

    @JsonSerialize
    public String id;

    @JsonSerialize
    public String serviceId;

    @XmlAttribute
    @JsonSerialize
    public String tripShortName;

    @XmlAttribute
    @JsonSerialize
    public String tripHeadsign;

    @JsonSerialize
    public String routeId;

    @XmlAttribute
    @JsonSerialize
    public String directionId;

    @XmlAttribute
    @JsonSerialize
    public String blockId;

    @JsonSerialize
    public String shapeId;

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
