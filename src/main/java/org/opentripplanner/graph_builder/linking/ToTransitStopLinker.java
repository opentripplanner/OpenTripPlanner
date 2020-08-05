package org.opentripplanner.graph_builder.linking;

import org.locationtech.jts.index.SpatialIndex;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.location.TemporaryStreetLocation;
import org.opentripplanner.routing.vertextype.TransitStop;

import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

/**
 * Links given vertex to closest transit stop(s) if there are any in close distance
 */
public class ToTransitStopLinker {

    private final SpatialIndex transitStopIndex;

    private final LinkingGeoTools linkingGeoTools;

    private final EdgesMaker edgesMaker;

    private final BestCandidatesGetter bestCandidatesGetter;

    public ToTransitStopLinker(SpatialIndex transitStopIndex, LinkingGeoTools linkingGeoTools, EdgesMaker edgesMaker,
                               BestCandidatesGetter bestCandidatesGetter) {
        this.transitStopIndex = transitStopIndex;
        this.linkingGeoTools = linkingGeoTools;
        this.edgesMaker = edgesMaker;
        this.bestCandidatesGetter = bestCandidatesGetter;
    }

    /**
     * Tries to temporarily link given vertex to closest transit stop(s), returns true if a link was made
     */
    public boolean tryLinkVertexToStop(TemporaryStreetLocation vertex) {
        List<TransitStop> transitStops = findTransitStopsToLink(vertex);
        transitStops.forEach(stop -> edgesMaker.makeTemporaryEdges(vertex, stop));
        return !transitStops.isEmpty();
    }

    /**
     *  Finds all closest transit stops in graph that given vertex could be linked to
     */
    private List<TransitStop> findTransitStopsToLink(Vertex vertex) {
        List<TransitStop> candidateStops = getCandidateStops(vertex);
        return bestCandidatesGetter.getBestCandidates(candidateStops, stop -> linkingGeoTools.distance(vertex, stop));
    }

    private List<TransitStop> getCandidateStops(Vertex vertex) {
        // We only link to stops if we are searching for origin/destination and for that we need transitStopIndex.
        if (transitStopIndex == null) {
            return emptyList();
        }
        // We search for closest stops (since this is only used in origin/destination linking if no edges were found)
        // in the same way the closest edges are found.
        return ((List<Vertex>) transitStopIndex.query(linkingGeoTools.createEnvelope(vertex))).stream()
                .filter(TransitStop.class::isInstance)
                .map(TransitStop.class::cast)
                .collect(toList());
    }
}
