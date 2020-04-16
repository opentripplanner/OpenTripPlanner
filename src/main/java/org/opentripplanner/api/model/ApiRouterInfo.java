package org.opentripplanner.api.model;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.api.mapping.TraverseModeMapper;
import org.opentripplanner.common.geometry.GeometrySerializer;
import org.opentripplanner.routing.bike_rental.BikeRentalStationService;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.util.TravelOption;
import org.opentripplanner.util.TravelOptionsMaker;
import org.opentripplanner.util.WorldEnvelope;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public class ApiRouterInfo {

    public String routerId;
    
    @JsonSerialize(using= GeometrySerializer.class)
    public Geometry polygon;

    public Date buildTime;

    public long transitServiceStarts;

    public long transitServiceEnds;

    public List<String> transitModes;

    private WorldEnvelope envelope;

    public double centerLatitude;

    public double centerLongitude;

    public boolean hasParkRide;

    public boolean hasBikeSharing;

    public boolean hasBikePark;

    public List<TravelOption> travelOptions;


    public ApiRouterInfo(String routerId, Graph graph) {
        BikeRentalStationService service = graph.getService(
                BikeRentalStationService.class, false
        );

        this.routerId = routerId;
        this.polygon = graph.getConvexHull();
        this.buildTime = graph.buildTime;
        this.transitServiceStarts = graph.getTransitServiceStarts();
        this.transitServiceEnds = graph.getTransitServiceEnds();
        this.transitModes = TraverseModeMapper.mapToApi(graph.getTransitModes());
        this.envelope = graph.getEnvelope();
        this.hasParkRide = graph.hasParkRide;
        this.hasBikeSharing = mapHasBikeSharing(service);
        this.hasBikePark = mapHasBikePark(service);
        this.travelOptions = TravelOptionsMaker.makeOptions(graph);
        addCenter(graph.getCenter());
    }

    public boolean mapHasBikeSharing(BikeRentalStationService service) {
        if (service == null) {
            return false;
        }

        //at least 2 bike sharing stations are needed for useful bike sharing
        return service.getBikeRentalStations().size() > 1;
    }

    public boolean mapHasBikePark(BikeRentalStationService service) {
        if (service == null) {
            return false;
        }
        return !service.getBikeParks().isEmpty();
    }

    /**
     * Set center coordinate from transit center in {@link Graph#calculateTransitCenter()} if transit is used
     * or as mean coordinate if not
     *
     * It is first called when OSM is loaded. Then after transit data is loaded.
     * So that center is set in all combinations of street and transit loading.
     */
    public void addCenter(Optional<Coordinate> center) {
        //Transit data was loaded and center was calculated with calculateTransitCenter
        if(center.isPresent()) {
            centerLongitude = center.get().x;
            centerLatitude = center.get().y;
        } else {
            // Does not work around 180th parallel.
            centerLatitude = (getUpperRightLatitude() + getLowerLeftLatitude()) / 2;
            centerLongitude = (getUpperRightLongitude() + getLowerLeftLongitude()) / 2;
        }
    }

    public double getLowerLeftLatitude() {
        return envelope.getLowerLeftLatitude();
    }

    public double getLowerLeftLongitude() {
        return envelope.getLowerLeftLongitude();
    }

    public double getUpperRightLatitude() {
        return envelope.getUpperRightLatitude();
    }

    public double getUpperRightLongitude() {
        return envelope.getUpperRightLongitude();
    }
}
