package org.opentripplanner.routing.edgetype.flex;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.linearref.LengthIndexedLine;
import org.opentripplanner.api.resource.CoordinateArrayListSequence;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.model.Stop;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.edgetype.PatternHop;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.opentripplanner.routing.vertextype.PatternArriveVertex;
import org.opentripplanner.routing.vertextype.PatternDepartVertex;
import org.opentripplanner.routing.vertextype.PatternStopVertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PartialPatternHop extends FlexPatternHop {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(PartialPatternHop.class);

    private double startIndex;
    private double endIndex;
    private double originalHopLength;
    private double percentageOfHop;
    private FlexPatternHop originalHop;
    private Geometry boardArea;
    private Geometry alightArea;
    private LineString displayGeometry;

    // if we have this, it's a deviated-route hop
    // these are "direct" times, ie drive times without DRT service parameteers applied
    private int startVehicleTime = 0;
    private int endVehicleTime = 0;
    private LineString startGeometry;
    private LineString endGeometry;

    // constructor for flag stops
    // this could be merged into deviated-route service constructor
    public PartialPatternHop(FlexPatternHop hop, PatternStopVertex from, PatternStopVertex to, Stop fromStop, Stop toStop, double startIndex, double endIndex, double buffer) {
        super(from, to, fromStop, toStop, hop.getStopIndex(), false);
        setRequestPickup(hop.getRequestPickup());
        setRequestDropoff(hop.getRequestDropoff());
        setServiceAreaRadius(hop.getServiceAreaRadius());
        setServiceArea(hop.getServiceArea());
        LengthIndexedLine line = new LengthIndexedLine(hop.getGeometry());
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.percentageOfHop = (this.endIndex - this.startIndex) / line.getEndIndex();
        this.originalHop = hop;
        this.originalHopLength = line.getEndIndex();
        setGeometry(hop, line, buffer, buffer);
    }

    // constructor for deviated-route service
    public PartialPatternHop(FlexPatternHop hop, PatternStopVertex from, PatternStopVertex to, Stop fromStop, Stop toStop, double startIndex, double endIndex,
                             LineString startGeometry, int startVehicleTime, LineString endGeometry, int endVehicleTime, double buffer) {
        super(from, to, fromStop, toStop, hop.getStopIndex(), false);
        setRequestPickup(hop.getRequestPickup());
        setRequestDropoff(hop.getRequestDropoff());
        setServiceAreaRadius(hop.getServiceAreaRadius());
        setServiceArea(hop.getServiceArea());
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

    // pass-thru for TemporaryDirectPatternHop
    public PartialPatternHop(FlexPatternHop hop, PatternStopVertex from, PatternStopVertex to, Stop fromStop, Stop toStop) {
        super(from, to, fromStop, toStop, hop.getStopIndex(), false);
        setRequestPickup(hop.getRequestPickup());
        setRequestDropoff(hop.getRequestDropoff());
        setServiceAreaRadius(hop.getServiceAreaRadius());
        setServiceArea(hop.getServiceArea());
        this.originalHop = hop;
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
    public static PartialPatternHop startHop(FlexPatternHop hop, PatternArriveVertex to, Stop toStop) {
        LengthIndexedLine line = new LengthIndexedLine(hop.getGeometry());
        return new PartialPatternHop(hop, (PatternStopVertex) hop.getFromVertex(), to, hop.getBeginStop(), toStop, line.getStartIndex(), line.project(to.getCoordinate()), 0);
    }

    public static PartialPatternHop endHop(FlexPatternHop hop, PatternDepartVertex from, Stop fromStop) {
        LengthIndexedLine line = new LengthIndexedLine(hop.getGeometry());
        return new PartialPatternHop(hop, from, (PatternStopVertex) hop.getToVertex(), fromStop, hop.getEndStop(), line.project(from.getCoordinate()), line.getEndIndex(), 0);
    }

    @Override
    public double timeLowerBound(RoutingRequest options) {
        return Math.floor(percentageOfHop * super.timeLowerBound(options)) + startVehicleTime + endVehicleTime;
    }

    @Override
    public int getRunningTime(State s0) {
        TripTimes tt = s0.getTripTimes();
        int vehicleTime = startVehicleTime + endVehicleTime;
        if (vehicleTime > 0) {
            vehicleTime = tt.getDemandResponseMaxTime(vehicleTime);
        }
        if (originalHopLength == 0) {
            return vehicleTime;
        }
        double startPct = startIndex / originalHopLength;
        double endPct = endIndex / originalHopLength;
        // necessary so rounding happens using the same coefficients as in FlexTransitBoardAlight
        int arr = tt.getArrivalTime(stopIndex + 1) - (int) Math.round((1 - endPct) * (tt.getRunningTime(stopIndex)));
        int dep = tt.getDepartureTime(stopIndex) + (int) Math.round(startPct * (tt.getRunningTime(stopIndex)));
        return (arr - dep) + vehicleTime;
    }

    @Override
    public LineString getDisplayGeometry() {
        if (displayGeometry != null) {
            return displayGeometry;
        }
        return getGeometry();
    }

    // is this hop too not-different to care about? for now lets say should be > 50 m shorter than original hop
    public boolean isTrivial(RoutingRequest options) {
        if ((isDeviatedRouteBoard() && getStartVehicleTime() < 5) || (isDeviatedRouteAlight() && getEndVehicleTime() < 5))
            return true;
        double length = SphericalDistanceLibrary.fastLength(getGeometry());
        double parentLength = SphericalDistanceLibrary.fastLength(getOriginalHop().getGeometry());
        if (length == 0 || length < options.flexMinPartialHopLength) {
            return true;
        }
        if (parentLength == 0) {
            return length < 5d; // deviated route
        }
        // Test for bad transit edges.
        double fromDist = SphericalDistanceLibrary.distance(getFromVertex().getCoordinate(),
                getGeometry().getStartPoint().getCoordinate());
        double toDist = SphericalDistanceLibrary.distance(getToVertex().getCoordinate(),
                getGeometry().getEndPoint().getCoordinate());
        if (fromDist > 400.0 || toDist > 400.0) {
            LOG.info("Discarding edge: mismatch between endpoints and street geometry. This "
                            + "indicates bad transit stop linking at {} or {}",
                    getOriginalHop().getBeginStop(), getOriginalHop().getEndStop());
            return true;
        }
        return length + 50 >= parentLength;
    }

    /**
     * Return true if "unscheduled" ie call-n-ride
     */
    public boolean isUnscheduled() {
        return false;
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

    public FlexPatternHop getOriginalHop() {
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

    public int getDirectVehicleTime() {
        return startVehicleTime + endVehicleTime;
    }

    @Override
    public String getFeedId() {
        return originalHop.getFeedId();
    }
}

