package org.opentripplanner.routing.edgetype;

import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.routing.vertextype.TransitEntranceVertex;

/** 
 * This represents the connection between a street vertex and a transit vertex belonging the street
 * network.
 */
public class StreetTransitEntranceLink extends StreetTransitEntityLink<TransitEntranceVertex> {

    public StreetTransitEntranceLink(StreetVertex fromv, TransitEntranceVertex tov) {
        super(fromv, tov, tov.isWheelchairEntrance());
    }

    public StreetTransitEntranceLink(TransitEntranceVertex fromv, StreetVertex tov) {
        super(fromv, tov, fromv.isWheelchairEntrance());
    }

    protected int getStreetToStopTime() {
        return 0;
    }

    public String toString() {
        return "StreetTransitEntranceLink(" + fromv + " -> " + tov + ")";
    }
}
