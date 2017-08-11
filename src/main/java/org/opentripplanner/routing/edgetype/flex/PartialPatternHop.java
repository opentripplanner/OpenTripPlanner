/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.routing.edgetype.flex;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.linearref.LengthIndexedLine;
import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.api.resource.CoordinateArrayListSequence;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.edgetype.PatternHop;
import org.opentripplanner.routing.vertextype.PatternArriveVertex;
import org.opentripplanner.routing.vertextype.PatternDepartVertex;
import org.opentripplanner.routing.vertextype.PatternStopVertex;

public class PartialPatternHop extends PatternHop {

    private static final long serialVersionUID = 1L;

    private double startIndex;
    private double endIndex;
    private double originalHopLength;
    private double percentageOfHop;
    private PatternHop originalHop;
    private Geometry boardArea;
    private Geometry alightArea;
    private LineString displayGeometry;

    // if we have this, it's a deviated-route hop
    private int startVehicleTime = 0;
    private int endVehicleTime = 0;
    private LineString startGeometry;
    private LineString endGeometry;

    // constructor for flag stops
    // this could be merged into deviated-route service constructor
    public PartialPatternHop(PatternHop hop, PatternStopVertex from, PatternStopVertex to, Stop fromStop, Stop toStop, double startIndex, double endIndex, double buffer) {
        super(from, to, fromStop, toStop, hop.getStopIndex(), hop.getRequestStops(), hop.getServiceAreaRadius(), hop.getServiceArea(), false);
        LengthIndexedLine line = new LengthIndexedLine(hop.getGeometry());
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.percentageOfHop = (this.endIndex - this.startIndex) / line.getEndIndex();
        this.originalHop = hop;
        this.originalHopLength = line.getEndIndex();
        setGeometry(hop, line, buffer, buffer);
    }

    // constructor for deviated-route service
    public PartialPatternHop(PatternHop hop, PatternStopVertex from, PatternStopVertex to, Stop fromStop, Stop toStop, double startIndex, double endIndex,
                             LineString startGeometry, int startVehicleTime, LineString endGeometry, int endVehicleTime, double buffer) {
        super(from, to, fromStop, toStop, hop.getStopIndex(), hop.getRequestStops(), hop.getServiceAreaRadius(), hop.getServiceArea(), false);

        LengthIndexedLine line = new LengthIndexedLine(hop.getGeometry());
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.percentageOfHop = (this.endIndex - this.startIndex) / line.getEndIndex();
        this.originalHop = hop;
        this.originalHopLength = line.getEndIndex();
        this.startVehicleTime = startVehicleTime;
        this.endVehicleTime = endVehicleTime;
        this.startGeometry = startGeometry;
        this.endGeometry = endGeometry;

        // Only area on the route will be part of display geometry.
        boolean flagStopBoard = startIndex > 0 && startVehicleTime == 0;
        boolean flagStopAlight = endIndex < line.getEndIndex() && endVehicleTime == 0;
        if (!hop.getBeginStop().equals(hop.getEndStop())) {
            setGeometry(hop, line, flagStopBoard ? buffer : 0, flagStopAlight ? buffer : 0);
        } else {
            // If this hop has no geometry (entirely flexible deviated-route):
            Coordinate c = hop.getFromVertex().getCoordinate();
            displayGeometry = GeometryUtils.getGeometryFactory().createLineString(new Coordinate[]{c, c});
            percentageOfHop = 0;
        }

        LineString transitGeometry = (LineString) line.extractLine(startIndex, endIndex);
        CoordinateArrayListSequence coordinates = new CoordinateArrayListSequence();
        if (startGeometry != null) {
            coordinates.extend(startGeometry.getCoordinates());
        }
        coordinates.extend(transitGeometry.getCoordinates());
        if (endGeometry != null) {
            coordinates.extend(endGeometry.getCoordinates());
        }
        LineString geometry = GeometryUtils.getGeometryFactory().createLineString(coordinates);
        setGeometry(geometry);
    }

    private void setGeometry(PatternHop hop, LengthIndexedLine line, double boardBuffer, double alightBuffer) {
        double pointsPerMeter =  (line.getEndIndex() - line.getStartIndex()) / SphericalDistanceLibrary.fastLength(hop.getGeometry());
        double boardBufferPts = boardBuffer * pointsPerMeter;
        double alightBufferPts = alightBuffer * pointsPerMeter;
        double start = Math.max(line.getStartIndex(), startIndex - boardBufferPts);
        double end = Math.min(line.getEndIndex(), endIndex + alightBufferPts);
        displayGeometry = (LineString) line.extractLine(start, end);
        Geometry geom = line.extractLine(startIndex, endIndex);
        if (geom instanceof LineString) { // according to the javadocs, it is.
            setGeometry((LineString) geom);
        }
        if (startIndex > line.getStartIndex() && boardBuffer > 0) {
            boardArea = line.extractLine(start, Math.min(startIndex + boardBufferPts, end));
        }
        if (endIndex < line.getEndIndex() && alightBuffer > 0) {
            alightArea = line.extractLine(Math.max(endIndex - alightBufferPts, start), end);
        }
    }

    // given hop s0->s1 and a temporary position t, create a partial hop s0->t
    public static PartialPatternHop startHop(PatternHop hop, PatternArriveVertex to, Stop toStop) {
        LengthIndexedLine line = new LengthIndexedLine(hop.getGeometry());
        return new PartialPatternHop(hop, (PatternStopVertex) hop.getFromVertex(), to, hop.getBeginStop(), toStop, line.getStartIndex(), line.project(to.getCoordinate()), 0);
    }

    public static PartialPatternHop endHop(PatternHop hop, PatternDepartVertex from, Stop fromStop) {
        LengthIndexedLine line = new LengthIndexedLine(hop.getGeometry());
        return new PartialPatternHop(hop, from, (PatternStopVertex) hop.getToVertex(), fromStop, hop.getEndStop(), line.project(from.getCoordinate()), line.getEndIndex(), 0);
    }

    @Override
    public double timeLowerBound(RoutingRequest options) {
        return (percentageOfHop * super.timeLowerBound(options)) + startVehicleTime + endVehicleTime;
    }

    @Override
    public int getRunningTime(State s0) {
        return (int) Math.round(percentageOfHop * super.getRunningTime(s0)) + startVehicleTime + endVehicleTime;
    }

    @Override
    public LineString getDisplayGeometry() {
        return displayGeometry;
    }

    public boolean isDeviatedRouteService() {
        return startVehicleTime > 0 || endVehicleTime > 0;
    }

    public boolean isDeviatedRouteBoard() {
        return startVehicleTime > 0;
    }

    public boolean isDeviatedRouteAlight() {
        return endVehicleTime > 0;
    }

    public boolean isFlagStopBoard() {
        return startIndex > 0 && startVehicleTime == 0;
    }

    public boolean isFlagStopAlight() {
        return endIndex < originalHopLength && endVehicleTime == 0;
    }

    public boolean isOriginalHop(PatternHop hop) {
        return originalHop.getId() == hop.getId();
    }

    public boolean hasBoardArea() {
        return boardArea != null;
    }

    public boolean hasAlightArea() {
        return alightArea != null;
    }

    public PatternHop getOriginalHop() {
        return originalHop;
    }

    public double getPercentageOfHop() {
        return percentageOfHop;
    }

    public double getStartIndex() {
        return startIndex;
    }

    public double getEndIndex() {
        return endIndex;
    }

    public double getOriginalHopLength() {
        return originalHopLength;
    }

    public Geometry getBoardArea() {
        return boardArea;
    }

    public Geometry getAlightArea() {
        return alightArea;
    }

    public int getStartVehicleTime() {
        return startVehicleTime;
    }

    public int getEndVehicleTime() {
        return endVehicleTime;
    }

    public LineString getStartGeometry() {
        return startGeometry;
    }

    public LineString getEndGeometry() {
        return endGeometry;
    }

}

