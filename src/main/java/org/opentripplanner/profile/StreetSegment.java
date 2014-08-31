package org.opentripplanner.profile;

import com.google.common.collect.Lists;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.api.model.Itinerary;
import org.opentripplanner.api.model.Leg;
import org.opentripplanner.api.model.WalkStep;
import org.opentripplanner.api.resource.PlanGenerator;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.util.PolylineEncoder;
import org.opentripplanner.util.model.EncodedPolylineBean;

import java.util.Collection;
import java.util.List;

/** A response object describing a non-transit part of an option (usually access/egress). */
public class StreetSegment {

    public TraverseMode mode;
    public int time;
    public EncodedPolylineBean geometry;
    public List<WalkStep> walkSteps;

    /** Build the walksteps from the final State of a path. */
    public StreetSegment (State state) {
        GraphPath path = new GraphPath(state, false);
        PlanGenerator pgen = new PlanGenerator(null, null);
        Itinerary itin = pgen.generateItinerary(path, false);
        Leg leg = itin.legs.get(0);
        walkSteps = leg.walkSteps;
        geometry = leg.legGeometry;
        time = (int) (state.getElapsedTimeSeconds());
    }

    /** A StreetSegment is very similar to a StopAtDistance but it's a response object so the State has to be rendered into walksteps. */
    public StreetSegment (StopAtDistance sd) {
        this(sd.state);
        mode = sd.mode; // Intended mode is known more reliably in a StopAtDistance than from a State.
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