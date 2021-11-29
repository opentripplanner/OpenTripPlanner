package org.opentripplanner.api.model;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.api.mapping.TraverseModeMapper;
import org.opentripplanner.common.geometry.GeometrySerializer;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalStationService;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vehicle_parking.VehicleParkingService;
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

    public final boolean hasBikePark;

    public final boolean hasCarPark;

    public final boolean hasVehicleParking;

    public List<TravelOption> travelOptions;


    /** TODO: Do not pass in the graph here, do this in a mapper instead. */
    public ApiRouterInfo(String routerId, Graph graph) {
        VehicleRentalStationService vehicleRentalService = graph.getService(
                VehicleRentalStationService.class, false
        );
        VehicleParkingService vehicleParkingService = graph.getService(
            VehicleParkingService.class, false
        );

        this.routerId = routerId;
        this.polygon = graph.getConvexHull();
        this.buildTime = graph.buildTime;
        this.transitServiceStarts = graph.getTransitServiceStarts();
        this.transitServiceEnds = graph.getTransitServiceEnds();
        this.transitModes = TraverseModeMapper.mapToApi(graph.getTransitModes());
        this.envelope = graph.getEnvelope();
        this.hasParkRide = graph.hasParkRide;
        this.hasBikeSharing = mapHasBikeSharing(vehicleRentalService);
        this.hasBikePark = mapHasBikePark(vehicleParkingService);
        this.hasCarPark = mapHasCarPark(vehicleParkingService);
        this.hasVehicleParking = mapHasVehicleParking(vehicleParkingService);
        this.travelOptions = TravelOptionsMaker.makeOptions(graph);
        addCenter(graph.getCenter());
    }

    public boolean mapHasBikeSharing(VehicleRentalStationService service) {
        if (service == null) {
            return false;
        }

        //at least 2 bike sharing stations are needed for useful bike sharing
        return service.getVehicleRentalPlaces().size() > 1;
    }

    public boolean mapHasBikePark(VehicleParkingService service) {
        if (service == null) {
            return false;
        }
        return service.getBikeParks().findAny().isPresent();
    }

    public boolean mapHasCarPark(VehicleParkingService service) {
        if (service == null) {
            return false;
        }
        return service.getCarParks().findAny().isPresent();
    }

    public boolean mapHasVehicleParking(VehicleParkingService service) {
        if (service == null) {
            return false;
        }
        return service.getVehicleParkings().findAny().isPresent();
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
