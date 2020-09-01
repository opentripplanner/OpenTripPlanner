package org.opentripplanner.routing.algorithm.mapping;

import org.opentripplanner.routing.algorithm.filterchain.FilterChainParameters;
import org.opentripplanner.routing.api.request.RoutingRequest;

import java.util.ArrayList;
import java.util.List;

public class RoutingRequestToFilterChainParametersMapper {
  /** Filter itineraries down to this limit, but not below. */
  private static final int MIN_NUMBER_OF_ITINERARIES = 3;

  /** Never return more that this limit of itineraries. */
  private static final int MAX_NUMBER_OF_ITINERARIES = 200;

    public static FilterChainParameters mapRequestToFilterChainParameters(final RoutingRequest request) {
    return new FilterChainParameters() {
      @Override public boolean arriveBy() { return request.arriveBy; }
      @Override public List<GroupBySimilarity> groupBySimilarity() {
        List<GroupBySimilarity> list = new ArrayList<>();
        if(request.groupBySimilarityKeepOne >= 0.5) {
          list.add(new GroupBySimilarity(request.groupBySimilarityKeepOne, 1));
        }
        if(request.groupBySimilarityKeepNumOfItineraries >= 0.5) {
          int minLimit = Math.min(request.numItineraries, MIN_NUMBER_OF_ITINERARIES);
          list.add(new GroupBySimilarity(request.groupBySimilarityKeepNumOfItineraries, minLimit));
        }
        return list;
      }
      @Override public int maxNumberOfItineraries() {
        return Math.min(request.numItineraries, MAX_NUMBER_OF_ITINERARIES);
      }
      @Override public boolean debug() { return request.debugItineraryFilter; }
    };
  }
}
