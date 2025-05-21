package org.opentripplanner.standalone.config.routerequest;

import static org.opentripplanner.standalone.config.framework.json.EnumMapper.docEnumValueList;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_0;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_1;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_2;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_3;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_4;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_7;

import org.opentripplanner.routing.algorithm.filterchain.api.TransitGeneralizedCostFilterParams;
import org.opentripplanner.routing.api.request.preference.ItineraryFilterDebugProfile;
import org.opentripplanner.routing.api.request.preference.ItineraryFilterPreferences;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

public class ItineraryFiltersConfig {

  public static void mapItineraryFilterParams(
    String parameterName,
    NodeAdapter root,
    ItineraryFilterPreferences.Builder builder
  ) {
    NodeAdapter c = root
      .of(parameterName)
      .since(V2_0)
      .summary(
        "Configure itinerary filters that may modify itineraries, sort them, and filter away less preferable results."
      )
      .description(
        """
        The purpose of the itinerary filter chain is to post process the result returned by the routing
        search. The filters may modify itineraries, sort them, and filter away less preferable results.

        OTP2 may produce numerous _pareto-optimal_ results when using `time`, `number-of-transfers` and
        `generalized-cost` as criteria. Use the parameters listed here to reduce/filter the itineraries
        return by the search engine before returning the results to client. There is also a few mandatory
        non-configurable filters removing none optimal results. You may see these filters pop-up in the
        filter debugging.

        #### Group by similarity filters

        The group-by-filter is a bit complex, but should be simple to use. Set `debug=true` and experiment
        with `searchWindow` and the three group-by parameters (`groupSimilarityKeepOne`,
        `groupSimilarityKeepThree` and `groupedOtherThanSameLegsMaxCostMultiplier`).

        The group-by-filter work by grouping itineraries together and then reducing the number of
        itineraries in each group, keeping the itinerary/itineraries with the best itinerary
        _generalized-cost_. The group-by function first pick all transit legs that account for more than N%
        of the itinerary based on distance traveled. This become the group-key. Two keys are the same if all
        legs in one of the keys also exist in the other. Note, one key may have a larger set of legs than the
        other, but they can still be the same. When comparing two legs we compare the `tripId` and make sure
        the legs overlap in place and time. Two legs are the same if both legs ride at least a common
        subsection of the same trip. The `keepOne` filter will keep ONE itinerary in each group. The
        `keepThree` keeps 3 itineraries for each group.

        The grouped itineraries can be further reduced by using `groupedOtherThanSameLegsMaxCostMultiplier`.
        This parameter filters out itineraries, where the legs that are not common for all the grouped
        itineraries have a much higher cost, than the lowest in the group. By default, it filters out
        itineraries that are at least double in cost for the non-grouped legs.
        """
      )
      .asObject();

    if (c.isEmpty()) {
      return;
    }
    var dft = builder.original();

    builder
      .withDebug(
        c
          .of("debug")
          .since(V2_0)
          .summary(ItineraryFilterDebugProfile.OFF.typeDescription())
          .description(docEnumValueList(ItineraryFilterDebugProfile.values()))
          .asEnum(dft.debug())
      )
      .withGroupSimilarityKeepOne(
        c
          .of("groupSimilarityKeepOne")
          .since(V2_1)
          .summary(
            "Pick ONE itinerary from each group after putting itineraries that are 85% similar together."
          )
          .asDouble(dft.groupSimilarityKeepOne())
      )
      .withGroupSimilarityKeepThree(
        c
          .of("groupSimilarityKeepThree")
          .since(V2_1)
          .summary(
            "Reduce the number of itineraries to three itineraries by reducing each group of itineraries grouped by 68% similarity."
          )
          .asDouble(dft.groupSimilarityKeepThree())
      )
      .withGroupedOtherThanSameLegsMaxCostMultiplier(
        c
          .of("groupedOtherThanSameLegsMaxCostMultiplier")
          .since(V2_1)
          .summary(
            "Filter grouped itineraries, where the non-grouped legs are more expensive than in the lowest cost one."
          )
          .description(
            """
            Of the itineraries grouped to maximum of three itineraries, how much worse can the non-grouped legs
            be compared to the lowest cost. 2.0 means that they can be double the cost, and any itineraries
            having a higher cost will be filtered.
            """
          )
          .asDouble(dft.groupedOtherThanSameLegsMaxCostMultiplier())
      )
      .withTransitGeneralizedCostLimit(
        parseTransitGeneralizedCostLimit(
          c
            .of("transitGeneralizedCostLimit")
            .since(V2_1)
            .summary("A relative limit for the generalized-cost for transit itineraries.")
            .description(
              """
              The filter compares all itineraries against every other itinerary. If the generalized-cost plus a
              `transitGeneralizedCostLimit` is higher than the other generalized-cost, then the itinerary is
              dropped. The `transitGeneralizedCostLimit` is calculated using the `costLimitFunction` plus a
              *relative cost* for the distance in time between the itineraries. The *relative cost* is the
              `intervalRelaxFactor` multiplied with the interval in seconds. To set the `costLimitFunction` to be
              _1 hour plus 2 times cost_ use: `3600 + 2.0 x`. To set an absolute value (3000s) use: `3000 + 0x`
              """
            )
            .asObject(),
          dft.transitGeneralizedCostLimit()
        )
      )
      .withNonTransitGeneralizedCostLimit(
        c
          .of("nonTransitGeneralizedCostLimit")
          .since(V2_1)
          .summary(
            "The function define a max-limit for generalized-cost for non-transit itineraries."
          )
          .description(
            """
            The max-limit is applied to itineraries with *no transit legs*, however *all* itineraries
            (including those with transit legs) are considered when calculating the minimum cost. The smallest
            generalized-cost value is used as input to the function. The function is used to calculate a
            *max-limit*. The max-limit is then used to filter *non-transit* itineraries by
            *generalized-cost*. Itineraries with a cost higher than the max-limit are dropped from the result
            set.

            For example if the function is `f(x) = 30m + 2.0 x` and the smallest cost is `30m = 1800s`, then
            all non-transit itineraries with a cost larger than `1800 + 2 * 5000 = 11 800` are dropped.
            """
          )
          .asCostLinearFunction(dft.nonTransitGeneralizedCostLimit())
      )
      .withRemoveTransitWithHigherCostThanBestOnStreetOnly(
        c
          .of("removeTransitWithHigherCostThanBestOnStreetOnly")
          .since(V2_4)
          .summary(
            "Limit function for generalized-cost computed from street-only itineries applied to transit itineraries."
          )
          .description(
            """
            The max-limit is applied to itineraries with transit *legs*, and only itineraries
            without transit legs are considered when calculating the minimum cost. The smallest
            generalized-cost value is used as input to the function. The function is used to calculate a
            *max-limit*. The max-limit is then used to filter *transit* itineraries by
            *generalized-cost*. Itineraries with a cost higher than the max-limit are dropped from the result
            set. Walking is handled with a different logic: if a transit itinerary has higher cost than
            a plain walk itinerary, it will be removed even if the cost limit function would keep it.
            """
          )
          .asCostLinearFunction(dft.removeTransitWithHigherCostThanBestOnStreetOnly())
      )
      .withBikeRentalDistanceRatio(
        c
          .of("bikeRentalDistanceRatio")
          .since(V2_1)
          .summary(
            "Filter routes that consist of bike-rental and walking by the minimum fraction " +
            "of the bike-rental leg using _distance_."
          )
          .description(
            """
            This filters out results that consist of a long walk plus a relatively short bike rental leg. A
            value of `0.3` means that a minimum of 30% of the total distance must be spent on the bike in order
            for the result to be included.
            """
          )
          .asDouble(dft.bikeRentalDistanceRatio())
      )
      .withParkAndRideDurationRatio(
        c
          .of("parkAndRideDurationRatio")
          .since(V2_1)
          .summary(
            "Filter P+R routes that consist of driving and walking by the minimum fraction " +
            "of the driving using of _time_."
          )
          .description(
            """
            This filters out results that consist of driving plus a very long walk leg at the end. A value of
            `0.3` means that a minimum of 30% of the total time must be spent in the car in order for the
            result to be included. However, if there is only a single result, it is never filtered.
                        """
          )
          .asDouble(dft.parkAndRideDurationRatio())
      )
      .withFilterItinerariesWithSameFirstOrLastTrip(
        c
          .of("filterItinerariesWithSameFirstOrLastTrip")
          .since(V2_2)
          .summary(
            "If more than one itinerary begins or ends with same trip, filter out one of those " +
            "itineraries so that only one remains."
          )
          .description(
            """
            Trips are considered equal if they have same id and same service day. Non-transit legs are skipped
            during comparison. Before filtering, trips are sorted by their generalized cost. The algorithm loops
            through the list from top to bottom. If an itinerary matches from any other itinerary from above, it is
            removed from list.
                          """
          )
          .asBoolean(dft.filterItinerariesWithSameFirstOrLastTrip())
      )
      .withRemoveItinerariesWithSameRoutesAndStops(
        c
          .of("removeItinerariesWithSameRoutesAndStops")
          .since(V2_2)
          .summary(
            "Set to true if you want to list only the first itinerary  which goes through the " +
            "same stops and routes."
          )
          .description(
            "Itineraries visiting the same set of stops and riding the exact same routes, " +
            "departing later are removed from the result."
          )
          .asBoolean(dft.removeItinerariesWithSameRoutesAndStops())
      )
      .withAccessibilityScore(
        c
          .of("accessibilityScore")
          .since(V2_2)
          .summary(
            "An experimental feature contributed by IBI which adds a sandbox accessibility " +
            "*score* between 0 and 1 for each leg and itinerary."
          )
          .description(
            "This can be used by frontend developers to implement a simple traffic light UI."
          )
          .asBoolean(dft.useAccessibilityScore())
      )
      .withMinBikeParkingDistance(
        c
          .of("minBikeParkingDistance")
          .since(V2_3)
          .summary(
            "Filter out bike park+ride results that have fewer meters of cycling than this value."
          )
          .description(
            "Useful if you want to exclude those routes which have only a few meters of cycling before parking the bike and taking public transport."
          )
          .asDouble(dft.minBikeParkingDistance())
      )
      .withFilterDirectFlexBySearchWindow(
        c
          .of("filterDirectFlexBySearchWindow")
          .since(V2_7)
          .summary(
            """
            Filter direct flex results by the search window. The search-window is not used
            during flex routing, but we use one end to align it with transit results."""
          )
          .description(
            """
            When direct flex is mixed with a transit search in the same request, then the direct
            flex results are filtered by the search window of the transit results.

            Depart-at searches are filtered by latest-arrival-time and arrive-by searches are
            filtered by earliest-departure-time.

            Use this configuration to turn this feature off.
            """
          )
          .asBoolean(true)
      )
      .build();
  }

  private static TransitGeneralizedCostFilterParams parseTransitGeneralizedCostLimit(
    NodeAdapter node,
    TransitGeneralizedCostFilterParams transitGeneralizedCostLimit
  ) {
    if (node.isEmpty()) {
      return transitGeneralizedCostLimit;
    }

    return new TransitGeneralizedCostFilterParams(
      node
        .of("costLimitFunction")
        .since(V2_2)
        .summary("The base function used by the filter.")
        .description(
          "This function calculates the threshold for the filter, when the itineraries have " +
          "exactly the same arrival and departure times."
        )
        .asCostLinearFunction(transitGeneralizedCostLimit.costLimitFunction()),
      node
        .of("intervalRelaxFactor")
        .since(V2_2)
        .summary(
          "How much the filter should be relaxed for itineraries that do not overlap in time."
        )
        .description(
          """
          This value is used to increase the filter threshold for itineraries further away in
          time, compared to those, that have exactly the same arrival and departure times.

          The unit is cost unit per second of time difference."""
        )
        .asDouble(transitGeneralizedCostLimit.intervalRelaxFactor())
    );
  }
}
