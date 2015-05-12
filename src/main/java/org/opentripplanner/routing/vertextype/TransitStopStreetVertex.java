package org.opentripplanner.routing.vertextype;

import org.opentripplanner.routing.graph.Graph;

/**
 * A vertex for an OSM node that represents a transit stop and has a ref=(stop_code) tag.
 * OTP will treat this as an authoritative statement on where the transit stop is located within the street network,
 * and the GTFS stop vertex will be linked to exactly this location.
 */
public class TransitStopStreetVertex extends IntersectionVertex {

    public String stopCode;

    public TransitStopStreetVertex(Graph g, String label, double x, double y, String name, String stopCode) {
        super(g, label, x, y, name);
        this.stopCode = stopCode;
    }

}
