package org.opentripplanner.routing.edgetype.flex;

import org.locationtech.jts.geom.LineString;
import org.opentripplanner.model.Stop;
import org.opentripplanner.routing.edgetype.TemporaryEdge;
import org.opentripplanner.routing.vertextype.PatternStopVertex;

public class TemporaryPartialPatternHop extends PartialPatternHop implements TemporaryEdge {
    public TemporaryPartialPatternHop(FlexPatternHop hop, PatternStopVertex from, PatternStopVertex to, Stop fromStop, Stop toStop, double startIndex, double endIndex, double buffer) {
        super(hop, from, to, fromStop, toStop, startIndex, endIndex, buffer);
    }

    public TemporaryPartialPatternHop(FlexPatternHop hop, PatternStopVertex from, PatternStopVertex to, Stop fromStop, Stop toStop, double startIndex, double endIndex,
                                      LineString startGeometry, int startVehicleTime, LineString endGeometry, int endVehicleTime, double buffer) {
        super(hop, from, to, fromStop, toStop, startIndex, endIndex, startGeometry, startVehicleTime, endGeometry, endVehicleTime, buffer);
    }

    // pass-thru for TemporaryDirectPatternHop
    public TemporaryPartialPatternHop(FlexPatternHop hop, PatternStopVertex from, PatternStopVertex to, Stop fromStop, Stop toStop) {
        super(hop, from, to, fromStop, toStop);
    }
}
