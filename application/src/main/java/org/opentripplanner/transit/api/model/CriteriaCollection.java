package org.opentripplanner.transit.api.model;

import com.beust.jcommander.internal.Nullable;
import java.util.Collection;
import java.util.NoSuchElementException;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.service.TransitService;

/**
 * CriteriaCollection is meant to be used when filtering results from {@link TransitService}.
 * </p>
 * This abstraction over the Collection type lets us keep filter specific functionality separate
 * from interpretation of various states of a collection. For instance in which case the criteria
 * should match all entities it's meant to filter.
 * @param <E> - The type of the criteria. Typically, String or {@link FeedScopedId}.
 */
public class CriteriaCollection<E> {

  private Collection<E> criteria;

  CriteriaCollection(Collection<E> criteria) {
    this.criteria = criteria;
  }

  public static <E> CriteriaCollection<E> ofEmptyIsEverything(@Nullable Collection<E> criteria) {
    return new CriteriaCollection<>(criteria);
  }

  public static <E> CriteriaCollection<E> ofEmptyIsEverything(
    @Nullable CriteriaCollection<E> criteria
  ) {
    if (criteria == null) {
      return new CriteriaCollection<>(null);
    }
    return criteria;
  }

  public boolean includeEverything() {
    return criteria == null || criteria.isEmpty();
  }

  public Collection<E> get() {
    if (criteria == null) {
      throw new NoSuchElementException(
        "No values present, use matchesAll() before calling this method."
      );
    }
    return criteria;
  }
}
