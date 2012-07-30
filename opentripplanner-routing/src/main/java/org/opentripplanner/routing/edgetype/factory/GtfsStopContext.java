package org.opentripplanner.routing.edgetype.factory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.common.model.T2;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.TransitStopArrive;
import org.opentripplanner.routing.vertextype.TransitStopDepart;

public class GtfsStopContext {

    public HashSet<AgencyAndId> stops = new HashSet<AgencyAndId>();

    public Map<Stop, Vertex> stopNodes = new HashMap<Stop, Vertex>();

    public Map<Stop, TransitStopArrive> stopArriveNodes = new HashMap<Stop, TransitStopArrive>();

    public Map<Stop, TransitStopDepart> stopDepartNodes = new HashMap<Stop, TransitStopDepart>();

    public Map<T2<Stop, Trip>, Vertex> patternArriveNodes = new HashMap<T2<Stop, Trip>, Vertex>();

    public Map<T2<Stop, Trip>, Vertex> patternDepartNodes = new HashMap<T2<Stop, Trip>, Vertex>(); // exemplar
                                                                                                   // trip
    public HashMap<AgencyAndId, Integer> serviceIds = new HashMap<AgencyAndId, Integer>();
}
