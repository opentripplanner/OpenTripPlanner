package org.opentripplanner.profile;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.google.common.collect.Lists;
import com.vividsolutions.jts.geom.LineString;
import org.opentripplanner.api.model.Itinerary;
import org.opentripplanner.api.model.Leg;
import org.opentripplanner.api.model.WalkStep;
import org.opentripplanner.api.parameter.QualifiedMode;
import org.opentripplanner.api.resource.CoordinateArrayListSequence;
import org.opentripplanner.api.resource.GraphPathToTripPlanConverter;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.error.TrivialPathException;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.spt.GraphPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

/** 
 * A response object describing a non-transit part of an Option. This is either an access/egress leg of a transit
 * trip, or a direct path to the destination that does not use transit.
 */
public class StreetSegment {

    private static final Logger LOG = LoggerFactory.getLogger(StreetSegment.class);

    @JsonSerialize(using = ToStringSerializer.class) // as a string (e.g. "BICYCLE_RENT" instead of a nested object)
    public QualifiedMode mode;
    public int time;
    public List<StreetEdgeInfo> streetEdges = Lists.newArrayList();

    /**
     * Build the walksteps from the final State of a path.
     * The path may contain more than one leg, because you may need to walk the bike.
     * Therefore accumulate the geometries into one long polyline, and accumulate the walksteps.
     */
    public StreetSegment (State state) {
        GraphPath path = new GraphPath(state, false);
        CoordinateArrayListSequence coordinates = new CoordinateArrayListSequence();
        for (Edge edge : path.edges) {
            LineString geometry = edge.getGeometry();
            if (geometry != null) {
                if (coordinates.size() == 0) {
                    coordinates.extend(geometry.getCoordinates());
                } else {
                    coordinates.extend(geometry.getCoordinates(), 1); // Avoid duplications
                }
            }
        }
        //TODO: localize
        try {
            Itinerary itin = GraphPathToTripPlanConverter.generateItinerary(path, false, new Locale("en"));
            for (Leg leg : itin.legs) {
                // populate the streetEdges array
                for (WalkStep walkStep : leg.walkSteps) {
                    int i = 0;
                    // TODO this initialization logic seems like it should be in the walkStep constructor,
                    // or walkstep and edgeInfo should be merged. We are also iterating over the edges twice (see above)
                    // to build up the geometry separately.
                    for (Edge edge : walkStep.edges) {
                        StreetEdgeInfo edgeInfo = new StreetEdgeInfo(edge);
                        if (i == 0) {
                            edgeInfo.mode = walkStep.newMode;
                            edgeInfo.streetName = walkStep.streetName;
                            edgeInfo.absoluteDirection = walkStep.absoluteDirection;
                            edgeInfo.relativeDirection = walkStep.relativeDirection;
                            edgeInfo.stayOn = walkStep.stayOn;
                            edgeInfo.area = walkStep.area;
                            edgeInfo.bogusName = walkStep.bogusName;
                            edgeInfo.bikeRentalOffStation = walkStep.bikeRentalOffStation;
                        }
                        if (i == walkStep.edges.size() - 1) {
                            edgeInfo.bikeRentalOnStation = walkStep.bikeRentalOnStation;
                        }
                        streetEdges.add(edgeInfo);
                        i++;
                    }
                }
            }
        } catch (TrivialPathException e) {
            // a trivial path exception implies that this path contains no edges (i.e. it starts and ends at the same vertex).
            // this is possible when the origin and destination are coincident.
            LOG.warn("Path from {} to {} was trivial, if these points are coincident this is not unexpected; otherwise, you might want to look into it.",
                    path.states.get(0).getVertex(),
                    path.states.get(path.states.size() - 1).getVertex());
        }

        time = (int) (state.getElapsedTimeSeconds());
    }

    /** A StreetSegment is very similar to a StopAtDistance but it's a response object so the State has to be rendered into walksteps. */
    public StreetSegment (StopAtDistance sd) {
        this(sd.state);
        mode = sd.qmode; // Intended mode is known more reliably in a StopAtDistance than from a State.
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