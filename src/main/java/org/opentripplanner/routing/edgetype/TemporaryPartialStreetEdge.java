package org.opentripplanner.routing.edgetype;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.linearref.LocationIndexedLine;
import org.opentripplanner.common.TurnRestriction;
import org.opentripplanner.common.geometry.PackedCoordinateSequence;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.util.ElevationUtils;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.routing.vertextype.TemporaryVertex;
import org.opentripplanner.util.I18NString;

import java.util.LinkedList;
import java.util.List;


final public class TemporaryPartialStreetEdge extends StreetWithElevationEdge implements TemporaryEdge {

    private static final long serialVersionUID = 1L;

    /**
     * The edge on which this lies.
     */
    private StreetEdge parentEdge;


    /**
     * Create a new partial street edge along the given 'parentEdge' from 'v1' to 'v2'.
     * If the length is negative, a new length is calculated from the geometry.
     * The elevation data is calculated using the 'parentEdge' and given 'length'.
     */
    public TemporaryPartialStreetEdge(StreetEdge parentEdge, StreetVertex v1, StreetVertex v2,
            LineString geometry, I18NString name, double length) {
        super(v1, v2, geometry, name, length, parentEdge.getPermission(), false);
        this.parentEdge = parentEdge;
        setCarSpeed(parentEdge.getCarSpeed());
        setElevationProfileUsingParents();

        // Assert that the edge is going in the right direction [only possible if vertex is temporary]
        assertEdgeIsNotDirectedAwayFromTemporaryEndVertex(v1);
        assertEdgeIsDirectedTowardsTemporaryEndVertex(v2);
    }

    /**
     * Create a new partial street edge along the given 'parentEdge' from 'v1' to 'v2'.
     * The length is calculated using the provided geometry.
     * The elevation data is calculated using the 'parentEdge' and the calculated 'length'.
     */
    TemporaryPartialStreetEdge(StreetEdge parentEdge, StreetVertex v1, StreetVertex v2,
            LineString geometry, I18NString name) {
        super(v1, v2, geometry, name, 0, parentEdge.getPermission(), false);
        this.parentEdge = parentEdge;
        setCarSpeed(parentEdge.getCarSpeed());

        // No length is known, so we use the provided geometry to estimate it
        calculateLengthFromGeometry();
        setElevationProfileUsingParents();

        // Assert that the edge is going in the right direction [only possible if vertex is temporary]
        assertEdgeIsNotDirectedAwayFromTemporaryEndVertex(v1);
        assertEdgeIsDirectedTowardsTemporaryEndVertex(v2);
    }

    /**
     * Partial edges are always partial.
     */
    @Override
    public boolean isPartial() {
        return true;
    }

    /**
     * Return a subset of the parent elevation profile.
     */
    @Override
    public PackedCoordinateSequence getElevationProfile() {
        PackedCoordinateSequence parentElev = super.getElevationProfile();
        if (parentElev == null) return null;

        // Compute the linear-reference bounds of the partial edge as fractions of the parent edge
        LocationIndexedLine line = new LocationIndexedLine(parentEdge.getGeometry());
        double startFraction =line.indexOf(this.getGeometry().getStartPoint().getCoordinate()).getSegmentFraction();
        double endFraction = line.indexOf(this.getGeometry().getEndPoint().getCoordinate()).getSegmentFraction();
        if (endFraction == 0) endFraction = 1;

        double parentDistance = parentEdge.getDistance();
        double distanceAdjust = this.getDistance() / ((endFraction - startFraction) * parentDistance);

        // Iterate through each entry of the elevation profile for the full parent edge
        Coordinate parentElevCoords[] = parentElev.toCoordinateArray();
        List<Coordinate> partialElevCoords = new LinkedList<>();
        boolean inPartialEdge = false;
        double startOffset = startFraction * parentDistance;
        for (int i = 1; i < parentElevCoords.length; i++) {
            // compute the fraction range covered by this entry in the elevation profile
            double x1 = parentElevCoords[i - 1].x;
            double x2 = parentElevCoords[i].x;
            double y1 = parentElevCoords[i - 1].y;
            double y2 = parentElevCoords[i].y;
            double f1 = x1 / parentDistance;
            double f2 = x2 / parentDistance;
            if (f2 > 1) f2 = 1;

            // Check if the partial edge begins in current section of the elevation profile
            if (startFraction >= f1 && startFraction < f2) {
                // Compute and add the interpolated elevation coordinate
                double pct = (startFraction - f1) / (f2 - f1);
                double x = x1 + pct * (x2 - x1);
                double y = y1 + pct * (y2 - y1);
                partialElevCoords.add(new Coordinate((x - startOffset) * distanceAdjust, y));

                // We are now "in" the partial-edge portion of the parent edge
                inPartialEdge = true;
            }

            // Check if the partial edge ends in current section of the elevation profile
            if (endFraction >= f1 && endFraction < f2) {
                // Compute and add the interpolated elevation coordinate
                double pct = (endFraction - f1) / (f2 - f1);
                double x = x1 + pct * (x2 - x1);
                double y = y1 + pct * (y2 - y1);
                partialElevCoords.add(new Coordinate((x - startOffset) * distanceAdjust, y));

                // This is the end of the partial edge, so we can end the iteration
                break;
            }

            if (inPartialEdge) {
                Coordinate c = new Coordinate((x2 - startOffset) * distanceAdjust, y2);
                partialElevCoords.add(c);
            }

        }

        Coordinate coords[] = partialElevCoords.toArray(new Coordinate[partialElevCoords.size()]);
        return new PackedCoordinateSequence.Double(coords);
    }

    /**
     * Have the ID of their parent.
     */
    @Override
    public int getId() {
        return parentEdge.getId();
    }

    /**
     * Have the inbound angle of  their parent.
     */
    @Override
    public int getInAngle() {
        return parentEdge.getInAngle();
    }

    /**
     * Have the outbound angle of  their parent.
     */
    @Override
    public int getOutAngle() {
        return parentEdge.getInAngle();
    }

    /**
     * Have the turn restrictions of  their parent.
     */
    @Override
    protected List<TurnRestriction> getTurnRestrictions(Graph graph) {
        return graph.getTurnRestrictions(parentEdge);
    }

    /**
     * This implementation makes it so that TurnRestrictions on the parent edge are applied to this edge as well.
     */
    @Override
    public boolean isEquivalentTo(Edge e) {
        return (e == this || e == parentEdge);
    }

    @Override
    public boolean isReverseOf(Edge e) {
        Edge other = e;
        if (e instanceof TemporaryPartialStreetEdge) {
            other = ((TemporaryPartialStreetEdge) e).parentEdge;
        }

        // TODO(flamholz): is there a case where a partial edge has a reverse of its own?
        return parentEdge.isReverseOf(other);
    }

    @Override
    public boolean isRoundabout() {
        return parentEdge.isRoundabout();
    }

    /**
     * Returns true if this edge is trivial - beginning and ending at the same point.
     */
    public boolean isTrivial() {
        Coordinate fromCoord = this.getFromVertex().getCoordinate();
        Coordinate toCoord = this.getToVertex().getCoordinate();
        return fromCoord.equals(toCoord);
    }

    public StreetEdge getParentEdge() {
        return parentEdge;
    }

    @Override
    public String toString() {
        return "TemporaryPartialStreetEdge(" + this.getName() + ", " + this.getFromVertex() + " -> "
                + this.getToVertex() + " length=" + this.getDistance() + " carSpeed="
                + this.getCarSpeed() + " parentEdge=" + parentEdge + ")";
    }

    private void assertEdgeIsNotDirectedAwayFromTemporaryEndVertex(StreetVertex v1) {
        if(v1 instanceof TemporaryVertex) {
            if (((TemporaryVertex)v1).isEndVertex()) {
                throw new IllegalStateException("A temporary edge is directed away from an end vertex");
            }
        }
    }

    private void assertEdgeIsDirectedTowardsTemporaryEndVertex(StreetVertex v2) {
        if(v2 instanceof TemporaryVertex) {
            if (!((TemporaryVertex)v2).isEndVertex()) {
                throw new IllegalStateException("A temporary edge is directed towards a start vertex");
            }
        }
    }

    private void setElevationProfileUsingParents() {
        setElevationProfile(
                ElevationUtils.getPartialElevationProfile(
                        getParentEdge().getElevationProfile(), 0, getDistance()
                ),
                false
        );
    }
}
