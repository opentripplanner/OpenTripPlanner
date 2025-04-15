package org.opentripplanner.transit.api.model;

import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.Objects;
import javax.annotation.Nullable;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * {@link FilterValues} is meant to be used when filtering results from {@link TransitService}.
 * </p>
 * This abstraction over the Collection type lets us keep filter specific functionality separate
 * from interpretation of various states of a collection. For instance in which case the filter values
 * should match all entities they are meant to filter.
 * </p>
 * @param <E> - The type of the filter values. Typically, String or {@link FeedScopedId}.
 */
public abstract class FilterValues<E> {

  @Nullable
  protected final Collection<E> values;

  private final String name;

  FilterValues(String name, @Nullable Collection<E> values) {
    this.name = name;
    this.values = values;
  }

  /**
   * Returns a {@link FilterValues} that matches everything if there are no filter values.
   * </p>
   * @param name   - The name of the filter.
   * @param <E>    - The type of the filter values. Typically, String or {@link FeedScopedId}.
   * @param values - The {@link Collection} of filter values.
   * @return FilterValues
   */
  public static <E> FilterValues<E> ofEmptyIsEverything(
    String name,
    @Nullable Collection<E> values
  ) {
    return new EmptyIsEverythingFilter<>(name, values);
  }

  /**
   * Returns a {@link FilterValues} that matches everything if the filter values are null.
   * </p>
   * @param name   - The name of the filter.
   * @param <E>    - The type of the filter values. Typically, String or {@link FeedScopedId}.
   * @param values - The {@link Collection} of filter values.
   * @return FilterValues
   */
  public static <E> FilterValues<E> ofNullIsEverything(
    String name,
    @Nullable Collection<E> values
  ) {
    return new NullIsEverythingFilter<>(name, values);
  }

  /**
   * Returns a {@link RequiredFilterValues} that throws an exception at creation time if the filter
   * values is null or empty.
   * </p>
   * @param name   - The name of the filter.
   * @param <E>    - The type of the filter values. Typically, String or {@link FeedScopedId}.
   * @param values - The {@link Collection} of filter values.
   * @return RequiredFilterValues
   */
  public static <E> RequiredFilterValues<E> ofRequired(
    String name,
    @Nullable Collection<E> values
  ) {
    return new RequiredFilterValues<>(name, values);
  }

  /**
   * Returns True if the collection of filter values matches everything that it could filter. If this
   * is the case, then the filter values should not be used to filter anything and filtering logic can
   * safely ignore it.
   * </p>
   * @return boolean
   */
  public abstract boolean includeEverything();

  /**
   * Returns the collection of filter values. If the filter values effectively don't filter anything,
   * an exception is thrown.
   * </p>
   * @return Collection<E> - The values of the filter.
   */
  public Collection<E> get() {
    if (includeEverything()) {
      throw new NoSuchElementException(
        "Filter values for filter %s effectively don't filter, use includeEverything() before calling this method.".formatted(
            name
          )
      );
    }
    return values;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof FilterValues<?> that)) return false;
    return Objects.equals(values, that.values) && Objects.equals(name, that.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(values, name);
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(getClass()).addStr("name", name).addCol("values", values).toString();
  }
}
