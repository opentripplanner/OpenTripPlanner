package org.opentripplanner.routing.edgetype.factory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.Trip;
import org.opentripplanner.common.model.T2;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.TransitStationStop;
import org.opentripplanner.routing.vertextype.TransitStopArrive;
import org.opentripplanner.routing.vertextype.TransitStopDepart;

/**
 *  Retains graph-wide information between PatternHopFactory runs on different feeds.
 *  FIXME is there any legitimate reason to even do that? Wouldn't it be better to keep each GTFS completely isolated?
 */
public class GtfsStopContext {

    public HashSet<FeedScopedId> stops = new HashSet<FeedScopedId>();

    // "stationStopNodes" means nodes that are either a station or a stop TODO clarify this name
    public Map<Stop, TransitStationStop> stationStopNodes = new HashMap<Stop, TransitStationStop>();

    public Map<Stop, TransitStopArrive> stopArriveNodes = new HashMap<Stop, TransitStopArrive>(); // FIXME these are stored in the stop vertices now, can remove

    public Map<Stop, TransitStopDepart> stopDepartNodes = new HashMap<Stop, TransitStopDepart>(); // FIXME these are stored in the stop vertices now, can remove

    public Map<T2<Stop, Trip>, Vertex> patternArriveNodes = new HashMap<T2<Stop, Trip>, Vertex>();

    public Map<T2<Stop, Trip>, Vertex> patternDepartNodes = new HashMap<T2<Stop, Trip>, Vertex>(); // exemplar

    // Why?
    public HashMap<TripPattern, Integer> tripPatternIds = new HashMap<TripPattern, Integer>();

}
