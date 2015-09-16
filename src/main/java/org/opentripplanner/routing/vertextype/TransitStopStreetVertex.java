package org.opentripplanner.routing.vertextype;

import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.util.NonLocalizedString;

/**
 * A vertex for an OSM node that represents a transit stop and has a ref=(stop_code) tag.
 * OTP will treat this as an authoritative statement on where the transit stop is located within the street network,
 * and the GTFS stop vertex will be linked to exactly this location.
 */
public class TransitStopStreetVertex extends OsmVertex {

    public String stopCode;

    public TransitStopStreetVertex(Graph g, String label, double x, double y, long nodeId, String name, String stopCode) {
        //Stop code is always non localized
        super(g, label, x, y, nodeId, new NonLocalizedString(name));
        this.stopCode = stopCode;
    }

}
