package org.opentripplanner.routing.vertextype;

import org.opentripplanner.model.FlexStopLocation;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;

import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.util.I18NString;
import org.opentripplanner.util.LocalizedString;
import org.opentripplanner.util.NonLocalizedString;

import java.util.*;

/**
 * Abstract base class for vertices in the street layer of the graph.
 * This includes both vertices representing intersections or points (IntersectionVertices) 
 * and Elevator*Vertices.
 */
public abstract class StreetVertex extends Vertex {

    private static final long serialVersionUID = 1L;

    /** All locations for flex transit, which this vertex is part of */
    public Set<FlexStopLocation> flexStopLocations;

    public StreetVertex(Graph g, String label, Coordinate coord, I18NString streetName) {
        this(g, label, coord.x, coord.y, streetName);
    }

    public StreetVertex(Graph g, String label, double x, double y, I18NString streetName) {
        super(g, label, x, y, streetName);
    }

    /**
     * Creates intersection name out of all outgoing names
     *
     * This can be:
     *  - name of the street if it is only 1
     *  - unnamedStreed (localized in requested language) if it doesn't have a name
     *  - corner of 0 and 1 (localized corner of zero and first street in the corner)
     *
     * @return already localized street names and non-localized corner of x and unnamedStreet
     */
    public I18NString getIntersectionName() {
        I18NString calculatedName = null;
        // generate names for corners when no name was given
        Set<I18NString> uniqueNameSet = new HashSet<>();
        for (Edge e : getOutgoing()) {
            if (e instanceof StreetEdge) {
                uniqueNameSet.add(e.getName());
            }
        }
        List<I18NString> uniqueNames = new ArrayList<>(uniqueNameSet);

        if (uniqueNames.size() > 1) {
            calculatedName = locale -> new LocalizedString("corner", new String[]{
                    uniqueNames.get(0).toString(locale),
                    uniqueNames.get(1).toString(locale)
            }).toString(locale);
        } else if (uniqueNames.size() == 1) {
            calculatedName = uniqueNames.get(0);
        } else {
            calculatedName = new LocalizedString("unnamedStreet");
        }
        return calculatedName;
    }

    public boolean isConnectedToWalkingEdge() {
        return this.getOutgoing().stream().anyMatch(edge ->
            edge instanceof StreetEdge && ((StreetEdge) edge).getPermission().allows(TraverseMode.WALK));
    }

    public boolean isConnectedToDriveableEdge() {
        return this.getOutgoing().stream().anyMatch(edge ->
            edge instanceof StreetEdge && ((StreetEdge) edge).getPermission().allows(TraverseMode.CAR));
    }

    public boolean isEligibleForCarPickupDropoff() {
        return isConnectedToDriveableEdge() && isConnectedToWalkingEdge();
    }
}
