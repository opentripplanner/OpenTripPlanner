package org.opentripplanner.raptor.spi;

import java.util.Collection;
import java.util.function.BiFunction;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.api.path.RaptorPath;

/**
 * This interface is used to run two multi-criteria searches and merging the result. Raptor will
 * run the heuristics as normal. Then create two multi-criteria searches, the main search and the
 * alternative search. The caller must provide a {@code merger} and
 * {@link RaptorTransitDataProvider}. The transit data is used for the alternative search. This
 * allows the caller to filter the transit data or change the cost-calculator.
 * <p>
 * When changing the transit data, you may also invalidate the heuristics created by Raptor. If this
 * is the case, you need to turn off the {@link org.opentripplanner.raptor.api.request.Optimization#PARETO_CHECK_AGAINST_DESTINATION}.
 * For the heuristics to work, you may add extra cost or filter away data. But you cannot decrease
 * the cost, add transfer or add new trips.
 * <p>
 * This will alter the multi-criteria search, if only a standard search is requested any extra
 * multi-criteria search is ignored.
 * <p>
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public interface ExtraMcRouterSearch<T extends RaptorTripSchedule> {
  /**
   * The returned transit-data is used in the ALTERNATIVE search. The given transit data is used in
   * the main search. It is the same data passed into Raptor.
   */
  RaptorTransitDataProvider<T> createTransitDataAlternativeSearch(
    RaptorTransitDataProvider<T> transitDataMainSearch
  );

  /**
   * You must provide a merge strategy to merge the main result (first argument) with the
   * alternative result(second argument). Make sure the end result does not have any duplicates.
   */
  BiFunction<
    Collection<RaptorPath<T>>,
    Collection<RaptorPath<T>>,
    Collection<RaptorPath<T>>
  > merger();
}
