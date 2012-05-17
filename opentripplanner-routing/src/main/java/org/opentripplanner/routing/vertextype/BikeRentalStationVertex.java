package org.opentripplanner.routing.vertextype;

import org.opentripplanner.common.MavenVersion;
import org.opentripplanner.routing.graph.AbstractVertex;
import org.opentripplanner.routing.graph.Graph;

/**
 * A vertex for a bike rental station.
 * 
 * @author laurent
 * 
 */
public class BikeRentalStationVertex extends AbstractVertex {

    private static final long serialVersionUID = MavenVersion.VERSION.getUID();

    private int bikesAvailable;

    private int spacesAvailable;

    private String id;

    public BikeRentalStationVertex(Graph g, String id, String label, double x, double y, String name,
            int capacity) {
        super(g, label, x, y, name);
        this.setId(id);
        this.bikesAvailable = capacity / 2;
        this.spacesAvailable = capacity / 2;
    }

    public BikeRentalStationVertex(Graph g, String id, String label, double x, double y, String name,
            int bikes, int spaces) {
        super(g, label, x, y, name);
        this.setId(id);
        this.bikesAvailable = bikes;
        this.spacesAvailable = spaces;
    }

    public int getBikesAvailable() {
        return bikesAvailable;
    }

    public int getSpacesAvailable() {
        return spacesAvailable;
    }

    public void setBikesAvailable(int bikes) {
        this.bikesAvailable = bikes;
    }

    public void setSpacesAvailable(int spaces) {
        this.spacesAvailable = spaces;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
