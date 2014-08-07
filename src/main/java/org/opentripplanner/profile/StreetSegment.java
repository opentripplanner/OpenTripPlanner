package org.opentripplanner.profile;

import com.google.common.collect.Lists;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.api.model.WalkStep;
import org.opentripplanner.api.resource.PlanGenerator;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.spt.GraphPath;

import java.util.Collection;
import java.util.List;

/** A response object describing a non-transit part of an option (usually access/egress). */
public class StreetSegment {

    public TraverseMode mode;
    public int time;
    public List<WalkStep> walkSteps;

    /** Build the walksteps from the final State of a path. */
    public StreetSegment (State state) {
        GraphPath path = new GraphPath(state, false);
        walkSteps = PlanGenerator.generateWalkSteps(path.states.toArray(new State[0]), null);
    }

    /** A StreetSegment is very similar to a StopAtDistance but it's a response object so the State has to be rendered into walksteps. */
    public StreetSegment (StopAtDistance sd) {
        //this(sd.state); // FIXME NPEs due to routing contexts being torn down (temp edges are disconnected)
        walkSteps = sd.walkSteps;
        mode = sd.mode;
        time = sd.etime;
    }

    /** Make a collections of StreetSegments from a collection of StopAtDistance. */
    public static List<StreetSegment> list(Collection<StopAtDistance> sds) {
        if (sds == null || sds.isEmpty()) return null;
        List<StreetSegment> ret = Lists.newArrayList();
        for (StopAtDistance sd : sds) {
            ret.add(new StreetSegment(sd));
        }
        return ret;
    }

}