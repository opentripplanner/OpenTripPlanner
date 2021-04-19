package org.opentripplanner.api.resource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.ws.rs.core.UriInfo;

import org.opentripplanner.api.model.RentalInfo;
import org.opentripplanner.api.model.TripPlan;
import org.opentripplanner.api.model.error.PlannerError;
import org.opentripplanner.routing.bike_rental.BikeRentalStationService;
import org.opentripplanner.routing.car_rental.CarRentalStationService;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalStationService;
import org.opentripplanner.standalone.Router;
import org.opentripplanner.updater.RentalUpdaterError;
import org.opentripplanner.updater.vehicle_rental.GBFSMappings.SystemInformation;

/** Represents a trip planner response, will be serialized into XML or JSON by Jersey */
public class Response {

    /** A dictionary of the parameters provided in the request that triggered this response. */
    public HashMap<String, String> requestParameters;
    private TripPlan plan;
    private PlannerError error = null;

    /** Debugging and profiling information */
    public DebugOutput debugOutput = null;

    public ElevationMetadata elevationMetadata = null;

    /**
     * Rental information about the bike rental networks used in the itinerary if applicable. This will be a map
     * where keys are network names and the values are the information associated with that particular network.
     */
    public Map<String, RentalInfo> bikeRentalInfo = null;

    /**
     * Rental information about the car rental networks used in the itinerary if applicable. This will be a map
     * where keys are network names and the values are the information associated with that particular network.
     */
    public Map<String, RentalInfo> carRentalInfo = null;

    /**
     * Rental information about the vehicle rental networks used in the itinerary if applicable. This will be a map
     * where keys are network names and the values are the information associated with that particular network.
     */
    public Map<String, RentalInfo> vehicleRentalInfo = null;

    /** This no-arg constructor exists to make JAX-RS happy. */ 
    @SuppressWarnings("unused")
    private Response() {};

    /** Construct an new response initialized with all the incoming query parameters. */
    public Response(UriInfo info) {
        this.requestParameters = new HashMap<String, String>();
        if (info == null) { 
            // in tests where there is no HTTP request, just leave the map empty
            return;
        }
        for (Entry<String, List<String>> e : info.getQueryParameters().entrySet()) {
            // include only the first instance of each query parameter
            requestParameters.put(e.getKey(), e.getValue().get(0));
        }
    }

    // NOTE: the order the getter methods below is semi-important, in that Jersey will use the
    // same order for the elements in the JS or XML serialized response. The traditional order
    // is request params, followed by plan, followed by errors.

    /** The actual trip plan. */
    public TripPlan getPlan() {
        return plan;
    }

    public void setPlan(TripPlan plan) {
        this.plan = plan;
    }

    /** The error (if any) that this response raised. */
    public PlannerError getError() {
        return error;
    }

    public void setError(PlannerError error) {
        this.error = error;
    }

    /**
     * Adds overall information about all rental systems that might have been available for this particular request.
     */
    public void setRentalInfo(RoutingRequest request, Router router) {
        Graph graph = router.graph;
        BikeRentalStationService bikeRentalStationService = graph.getService(BikeRentalStationService.class);
        CarRentalStationService carRentalStationService = graph.getService(CarRentalStationService.class);
        VehicleRentalStationService vehicleRentalStationService = graph.getService(VehicleRentalStationService.class);

        if (bikeRentalStationService != null && request.allowBikeRental) {
            bikeRentalInfo = makeRentalInfo(
                bikeRentalStationService.getErrorsByNetwork(),
                bikeRentalStationService.getSystemInformationDataByNetwork()
            );
        }

        if (carRentalStationService != null && request.allowCarRental) {
            carRentalInfo = makeRentalInfo(
                carRentalStationService.getErrorsByNetwork(),
                carRentalStationService.getSystemInformationDataByNetwork()
            );
        }

        if (vehicleRentalStationService != null && request.allowVehicleRental) {
            vehicleRentalInfo = makeRentalInfo(
                vehicleRentalStationService.getErrorsByNetwork(),
                vehicleRentalStationService.getSystemInformationDataByNetwork()
            );
        }
    }

    private static Map<String, RentalInfo> makeRentalInfo(
        Map<String, List<RentalUpdaterError>> errorsByNetwork,
        Map<String, SystemInformation.SystemInformationData> systemInformationDataByNetwork
    ) {
        Map<String, RentalInfo> rentalInfo = new HashMap<>();
        for (String network : errorsByNetwork.keySet()) {
            rentalInfo.put(
                network,
                new RentalInfo(errorsByNetwork.get(network), systemInformationDataByNetwork.get(network))
            );
        }
        return rentalInfo;
    }
}