package org.opentripplanner.graph_builder.module.extra_elevation_data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opentripplanner.routing.edgetype.StreetEdge;

public class ExtraElevationData {
    public Map<StreetEdge, List<ElevationPoint>> data = new HashMap<StreetEdge, List<ElevationPoint>>();
}
