package org.opentripplanner.routing.edgetype;

import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.routing.vertextype.TransitStopVertex;

/** 
 * This represents the connection between a street vertex and a transit vertex
 * where going from the street to the vehicle is immediate -- such as at a 
 * curbside bus stop.
 */
public class StreetTransitStopLink extends StreetTransitEntityLink<TransitStopVertex> {

    public StreetTransitStopLink(StreetVertex fromv, TransitStopVertex tov) {
    	super(fromv, tov, tov.hasWheelchairEntrance());
    }

    public StreetTransitStopLink(TransitStopVertex fromv, StreetVertex tov) {
        super(fromv, tov, fromv.hasWheelchairEntrance());
    }

    protected int getStreetToStopTime() {
        return getTransitEntityVertex().hasPathways() ? 0 : getTransitEntityVertex().getStreetToStopTime();
    }

    public String toString() {
        return "StreetTransitStopLink(" + fromv + " -> " + tov + ")";
    }
}
