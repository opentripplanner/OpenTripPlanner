package org.opentripplanner.model.plan;

import org.opentripplanner.model.StopLocation;
import org.opentripplanner.model.WgsCoordinate;
import org.opentripplanner.model.base.ToStringBuilder;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalPlace;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.TransitStopVertex;
import org.opentripplanner.routing.vertextype.VehicleParkingEntranceVertex;
import org.opentripplanner.routing.vertextype.VehicleRentalStationVertex;

/** 
* A Place is where a journey starts or ends, or a transit stop along the way.
*/
public class Place {

    /** 
     * For transit stops, the name of the stop.  For points of interest, the name of the POI.
     */
    public final String name;

    public final String orig;

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
     * For transit trips, the stop index (numbered from zero from the start of the trip).
     */
    public final Integer stopIndex;

    /**
     * For transit trips, the sequence number of the stop. Per GTFS, these numbers are increasing.
     */
    public final Integer stopSequence;

    /**
     * The vehicle rental place if the type is {@link VertexType#VEHICLERENTAL}.
     */
    public final VehicleRentalPlace vehicleRentalPlace;

    /**
     * The vehicle parking entrance if the type is {@link VertexType#VEHICLEPARKING}.
     */
    public final VehicleParkingWithEntrance vehicleParkingWithEntrance;

    private Place(
            String name,
            String orig,
            WgsCoordinate coordinate,
            VertexType vertexType,
            StopLocation stop,
            Integer stopIndex,
            Integer stopSequence,
            VehicleRentalPlace vehicleRentalPlace,
            VehicleParkingWithEntrance vehicleParkingWithEntrance
    ) {
        this.name = name;
        this.orig = orig;
        this.coordinate = coordinate;
        this.vertexType = vertexType;
        this.stop = stop;
        this.stopIndex = stopIndex;
        this.stopSequence = stopSequence;
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
        StringBuilder buf = new StringBuilder(name);
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
                .addStr("name", name)
                .addObj("stop", stop)
                .addObj("coordinate", coordinate)
                .addStr("orig", orig)
                .addNum("stopIndex", stopIndex)
                .addNum("stopSequence", stopSequence)
                .addEnum("vertexType", vertexType)
                .addObj("vehicleRentalPlace", vehicleRentalPlace)
                .addObj("vehicleParkingEntrance", vehicleParkingWithEntrance)
                .toString();
    }

    public static Place normal(double lat, double lon, String name) {
        return new Place(
                name,
                null,
                WgsCoordinate.creatOptionalCoordinate(lat, lon),
                VertexType.NORMAL,
                null, null, null, null, null
        );
    }

    public static Place normal(Vertex vertex, String name) {
        return new Place(
                name,
                null,
                WgsCoordinate.creatOptionalCoordinate(vertex.getLat(), vertex.getLon()),
                VertexType.NORMAL,
                null, null, null, null, null
        );
    }

    public static Place forStop(StopLocation stop, Integer stopIndex, Integer stopSequence) {
        return new Place(
                stop.getName(),
                null,
                stop.getCoordinate(),
                VertexType.TRANSIT,
                stop,
                stopIndex,
                stopSequence,
                null,
                null
        );
    }

    public static Place forFlexStop(
            StopLocation stop,
            Vertex vertex,
            Integer stopIndex,
            Integer stopSequence
    ) {
        // The actual vertex is used because the StopLocation coordinates may not be equal to the vertex's
        // coordinates.
        return new Place(
                stop.getName(),
                null,
                WgsCoordinate.creatOptionalCoordinate(vertex.getLat(), vertex.getLon()),
                VertexType.TRANSIT,
                stop,
                stopIndex,
                stopSequence,
                null,
                null
        );
    }

    public static Place forStop(TransitStopVertex vertex, String name) {
        return new Place(
                name,
                null,
                WgsCoordinate.creatOptionalCoordinate(vertex.getLat(), vertex.getLon()),
                VertexType.TRANSIT,
                vertex.getStop(),
                null,
                null,
                null,
                null
        );
    }

    public static Place forVehicleRentalPlace(VehicleRentalStationVertex vertex, String name) {
        return new Place(
                name,
                null,
                WgsCoordinate.creatOptionalCoordinate(vertex.getLat(), vertex.getLon()),
                VertexType.VEHICLERENTAL,
                null,
                null,
                null,
                vertex.getStation(),
                null
        );
    }

    public static Place forVehicleParkingEntrance(VehicleParkingEntranceVertex vertex, String name) {
        return new Place(
                name,
                null,
                WgsCoordinate.creatOptionalCoordinate(vertex.getLat(), vertex.getLon()),
                VertexType.VEHICLEPARKING,
                null,
                null,
                null,
                null,
                VehicleParkingWithEntrance.builder()
                        .vehicleParking(vertex.getVehicleParking())
                        .entrance(vertex.getParkingEntrance())
                        .build()
        );
    }
}
