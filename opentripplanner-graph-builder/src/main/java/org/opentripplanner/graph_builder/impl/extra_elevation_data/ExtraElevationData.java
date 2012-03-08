package org.opentripplanner.graph_builder.impl.extra_elevation_data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opentripplanner.routing.edgetype.EdgeWithElevation;

public class ExtraElevationData {
    public Map<EdgeWithElevation, List<ElevationPoint>> data = new HashMap<EdgeWithElevation, List<ElevationPoint>>();
}
