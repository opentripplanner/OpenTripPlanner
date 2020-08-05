package org.opentripplanner.graph_builder.linking;

import jersey.repackaged.com.google.common.collect.Lists;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.opentripplanner.graph_builder.linking.LinkingGeoTools.RADIUS_DEG;

/**
 * Filters candidates to linking and returns best ones
 */
public class BestCandidatesGetter {

    /**
     * If there are two ways and the distances to them differ by less than this value, we link to both of them
     */
    private static final double DUPLICATE_WAY_EPSILON_DEGREES = SphericalDistanceLibrary.metersToDegrees(0.001);

    /**
     * Returns edges/vertexes closest to a given vertex
     */
    public <T> List<T> getBestCandidates(List<T> candidates, Function<T, Double> distanceObtainer) {
        List<Map.Entry<T, Double>> sortedCandidates = getSortedCandidatesWithDistances(candidates, distanceObtainer);
        if (sortedCandidates.isEmpty() || sortedCandidates.get(0).getValue() > RADIUS_DEG) {
            return emptyList();
        }
        return filterBestCandidates(sortedCandidates);
    }

    /**
     * Returns candidate edges/vertices with distances to given vertex sorted by this distance
     */
    private <T> List<Map.Entry<T, Double>> getSortedCandidatesWithDistances(List<T> candidates,
                                                                            Function<T, Double> distanceObtainer) {
        return candidates.stream()
                .collect(Collectors.toMap(Function.identity(), distanceObtainer))
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue())
                .collect(toList());
    }

    /**
     * Get edges/vertices with lowest distance
     */
    private <T> List<T> filterBestCandidates(List<Map.Entry<T, Double>> sortedCandidates) {

        // Find the best edges
        List<T> bestEdges = Lists.newArrayList();

        // Add edges until there is a break of epsilon meters.
        // We do this to enforce determinism. if there are a lot of edges that are all extremely close to each other,
        // We want to be sure that we deterministically link to the same ones every time. Any hard cutoff means things can
        // fall just inside or beyond the cutoff depending on floating-point operations.
        int i = 0;
        do {
            bestEdges.add(sortedCandidates.get(i++).getKey());
        } while (i < sortedCandidates.size() && shouldAddNextCandidate(sortedCandidates, i));

        return bestEdges;
    }

    private <T> boolean shouldAddNextCandidate(List<Map.Entry<T, Double>> sortedCandidates, int i) {
        return sortedCandidates.get(i).getValue() - sortedCandidates.get(i - 1).getValue() < DUPLICATE_WAY_EPSILON_DEGREES;
    }
}
