package org.opentripplanner.routing.vertextype;

import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalStation;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;

/**
 * A vertex for a vehicle rental station.
 * It is connected to the streets by a StreetVehicleRentalLink.
 * To allow transitions on and off a bike, it has VehicleRentalEdge loop edges.
 *
 * @author laurent
 *
 */
public class VehicleRentalStationVertex extends Vertex {

    private static final long serialVersionUID = 2L;

    private VehicleRentalStation station;

    public VehicleRentalStationVertex(Graph g, VehicleRentalStation station) {
        //FIXME: raw_name can be null if bike station is made from graph updater
        super(g, "bike rental station " + station.id, station.longitude, station.latitude, station.name);
        this.station = station;
    }

    public VehicleRentalStation getStation() {
        return station;
    }

    public void setStation(VehicleRentalStation station) {
        this.station = station;
    }

    /**
     * Tell the routing algorithm what kind of vehicle is being rented or dropped off here.
     * Some car rental systems and flex transit systems work exactly like bike rental, but with cars.
     * We can model them as bike rental systems by changing only this one detail.
     */
    public TraverseMode getVehicleMode () {
         return station.isCarStation ? TraverseMode.CAR : TraverseMode.BICYCLE;
    }

}
