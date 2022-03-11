package org.opentripplanner.model.plan;

import org.opentripplanner.model.StopLocation;
import org.opentripplanner.model.WgsCoordinate;
import org.opentripplanner.model.base.ToStringBuilder;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalPlace;
import org.opentripplanner.routing.vertextype.VehicleParkingEntranceVertex;
import org.opentripplanner.routing.vertextype.VehicleRentalStationVertex;
import org.opentripplanner.util.I18NString;
import org.opentripplanner.util.NonLocalizedString;

/** 
* A Place is where a journey starts or ends, or a transit stop along the way.
*/
public class Place {

    /** 
     * For transit stops, the name of the stop.  For points of interest, the name of the POI.
     */
    public final I18NString name;

    /**
     * The coordinate of the place.
     */
    public final WgsCoordinate coordinate;

    /**
     * Type of vertex. (Normal, Bike sharing station, Bike P+R, Transit stop)
     * Mostly used for better localization of bike sharing and P+R station names
     */
    public final VertexType vertexType;

    /**
     * Reference to the stop if the type is {@link VertexType#TRANSIT}.
     */
    public final StopLocation stop;

    /**
     * The vehicle rental place if the type is {@link VertexType#VEHICLERENTAL}.
     */
    public final VehicleRentalPlace vehicleRentalPlace;

    /**
     * The vehicle parking entrance if the type is {@link VertexType#VEHICLEPARKING}.
     */
    public final VehicleParkingWithEntrance vehicleParkingWithEntrance;

    private Place(
            I18NString name,
            WgsCoordinate coordinate,
            VertexType vertexType,
            StopLocation stop,
            VehicleRentalPlace vehicleRentalPlace,
            VehicleParkingWithEntrance vehicleParkingWithEntrance
    ) {
        this.name = name;
        this.coordinate = coordinate;
        this.vertexType = vertexType;
        this.stop = stop;
        this.vehicleRentalPlace = vehicleRentalPlace;
        this.vehicleParkingWithEntrance = vehicleParkingWithEntrance;
    }

    /**
     * Test if the place is likely to be at the same location. First check the coordinates
     * then check the stopId [if it exist].
     */
    public boolean sameLocation(Place other) {
        if(this == other) { return true; }
        if(coordinate != null) {
            return coordinate.sameLocation(other.coordinate);
        }
        return stop != null && stop.equals(other.stop);
    }

    /**
     * Return a short version to be used in other classes toStringMethods. Should return
     * just the necessary information for a human to identify the place in a given the context.
     */
    public String toStringShort() {
        StringBuilder buf = new StringBuilder(name.toString());
        if(stop != null) {
            buf.append(" (").append(stop.getId()).append(")");
        } else {
            buf.append(" ").append(coordinate.toString());
        }

        return buf.toString();
    }

    @Override
    public String toString() {
        return ToStringBuilder.of(Place.class)
                .addStr("name", name.toString())
                .addObj("stop", stop)
                .addObj("coordinate", coordinate)
                .addEnum("vertexType", vertexType)
                .addObj("vehicleRentalPlace", vehicleRentalPlace)
                .addObj("vehicleParkingEntrance", vehicleParkingWithEntrance)
                .toString();
    }

    public static Place normal(Double lat, Double lon, I18NString name) {
        return new Place(
                name,
                WgsCoordinate.creatOptionalCoordinate(lat, lon),
                VertexType.NORMAL,
                null, null, null
        );
    }

    public static Place normal(Vertex vertex, I18NString name) {
        return new Place(
                name,
                WgsCoordinate.creatOptionalCoordinate(vertex.getLat(), vertex.getLon()),
                VertexType.NORMAL,
                null, null, null
        );
    }

    public static Place forStop(StopLocation stop) {
        return new Place(
                new NonLocalizedString(stop.getName()),
                stop.getCoordinate(),
                VertexType.TRANSIT,
                stop,
                null,
                null
        );
    }

    public static Place forFlexStop(StopLocation stop, Vertex vertex) {
        // The actual vertex is used because the StopLocation coordinates may not be equal to the vertex's
        // coordinates.
        return new Place(
                new NonLocalizedString(stop.getName()),
                WgsCoordinate.creatOptionalCoordinate(vertex.getLat(), vertex.getLon()),
                VertexType.TRANSIT,
                stop,
                null,
                null
        );
    }

    public static Place forVehicleRentalPlace(VehicleRentalStationVertex vertex) {
        return new Place(
                vertex.getName(),
                WgsCoordinate.creatOptionalCoordinate(vertex.getLat(), vertex.getLon()),
                VertexType.VEHICLERENTAL,
                null,
                vertex.getStation(),
                null
        );
    }

    public static Place forVehicleParkingEntrance(VehicleParkingEntranceVertex vertex, RoutingRequest request) {
        TraverseMode traverseMode = null;
        if (request.streetSubRequestModes.getCar()) {
            traverseMode = TraverseMode.CAR;
        } else if (request.streetSubRequestModes.getBicycle()) {
            traverseMode = TraverseMode.BICYCLE;
        }

        boolean realTime = request.useVehicleParkingAvailabilityInformation
                && vertex.getVehicleParking().hasRealTimeDataForMode(traverseMode, request.wheelchairAccessible);
        return new Place(
                vertex.getName(),
                WgsCoordinate.creatOptionalCoordinate(vertex.getLat(), vertex.getLon()),
                VertexType.VEHICLEPARKING,
                null,
                null,
                VehicleParkingWithEntrance.builder()
                        .vehicleParking(vertex.getVehicleParking())
                        .entrance(vertex.getParkingEntrance())
                        .realtime(realTime)
                        .build()
        );
    }
}
