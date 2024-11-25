package org.opentripplanner.transit.api.model;

import com.beust.jcommander.internal.Nullable;
import java.util.Collection;
import java.util.NoSuchElementException;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.service.TransitService;

/**
 * {@link FilterValueCollection} is meant to be used when filtering results from {@link TransitService}.
 * </p>
 * This abstraction over the Collection type lets us keep filter specific functionality separate
 * from interpretation of various states of a collection. For instance in which case the filter values
 * should match all entities they are meant to filter.
 * </p>
 * Note: Currently the only implementation of this class has functionality to include everything if the
 * collection is empty or null. Future implementations could not include everything if the collection
 * is empty or null, or alternatively contain more complex logic to determine if the collection should
 * include everything.
 * @param <E> - The type of the filter values. Typically, String or {@link FeedScopedId}.
 */
public class FilterValueCollection<E> {

  @Nullable
  private final Collection<E> filterValues;

  FilterValueCollection(@Nullable Collection<E> filterValues) {
    this.filterValues = filterValues;
  }

  /**
   * Returns a {@link FilterValueCollection} that matches everything if there are no filter values.
   * </p>
   * @param filterValues - The {@link Collection} of filter values.
   * @return FilterValueCollection
   * @param <E> - The type of the filter values. Typically, String or {@link FeedScopedId}.
   */
  public static <E> FilterValueCollection<E> ofEmptyIsEverything(
    @Nullable Collection<E> filterValues
  ) {
    return new FilterValueCollection<>(filterValues);
  }

  /**
   * Returns True if the collection of filter values matches everything that it could filter. If this
   * is the case, then the filter values should not be used to filter anything and filtering logic can
   * safely ignore it.
   * </p>
   * @return boolean
   */
  public boolean includeEverything() {
    return filterValues == null || filterValues.isEmpty();
  }

  public Collection<E> get() {
    if (includeEverything()) {
      throw new NoSuchElementException(
        "These filter values effectively don't filter, use includeEverything() before calling this method."
      );
    }
    return filterValues;
  }
}
