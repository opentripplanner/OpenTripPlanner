package org.opentripplanner.routing.algorithm.filterchain.framework.spi;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.opentripplanner.model.SystemNotice;
import org.opentripplanner.model.plan.Itinerary;

/**
 * ItineraryDeletionFlagger is used to flag itineraries which should not be presented to the user.
 * <p>
 * Override one of the default methods in this interface to make a filter:
 * <ul>
 *  <li>{@link #shouldBeFlaggedForRemoval()} - If filtering is done based on looking at one itinerary at the time.</li>
 *  <li>{@link #flagForRemoval(List)}}) - If you need more than one itinerary to decide which to delete.</li>
 * </ul>
 */
public interface RemoveItineraryFlagger {
  /**
   * A name used for debugging the itinerary list filter chain.
   * <p>
   * Use '-' so separate words like: {@code sort-on-duration-filter}
   */
  String name();

  // Override one:

  /**
   * Override this to create a simple filter, which flags all itineraries for deletion where the
   * predicate returns {@code true}.
   */
  default Predicate<Itinerary> shouldBeFlaggedForRemoval() {
    return null;
  }

  /**
   * Override this if you need to compare itineraries - all at once, for deciding which should get
   * flagged for removal. All itineraries returned from this function will be flagged for deletion
   * using {@link Itinerary#flagForDeletion(SystemNotice)}.
   */
  default List<Itinerary> flagForRemoval(List<Itinerary> itineraries) {
    return itineraries.stream().filter(shouldBeFlaggedForRemoval()).collect(Collectors.toList());
  }

  // Tagging options:

  /**
   * Should itineraries already marked for deletion by previous deletionflagger be removed from the
   * list passed to {@link RemoveItineraryFlagger#flagForRemoval(List)}. The default value
   * is true, as usually the already removed itineraries are not needed further in the filter
   * chain.
   */
  default boolean skipAlreadyFlaggedItineraries() {
    return true;
  }

  /**
   * Filter given {@code input} and remove itineraries which should be flagged for removal. This
   * can be used in unit-tests - either testing the filter or using the filter in a test.
   * <p>
   * This method should be used in unit-tests only.
   */
  default List<Itinerary> removeMatchesForTest(List<Itinerary> input) {
    var res = new ArrayList<>(input);
    res.removeAll(flagForRemoval(input));
    return res;
  }
}
