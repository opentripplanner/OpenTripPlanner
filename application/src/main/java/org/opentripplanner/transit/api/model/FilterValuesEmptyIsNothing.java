package org.opentripplanner.transit.api.model;

import java.util.Collection;

/**
 * {@link FilterValuesEmptyIsNothing} is a subclass of {@link FilterValues} that includes
 * nothing if the values are null or empty.
 * <p>
 * This is useful if you have inclusion/exclusion logic: an empty inclusion list means that you
 * want nothing.
 */
public class FilterValuesEmptyIsNothing<E> extends FilterValues<E> {

  FilterValuesEmptyIsNothing(String name, Collection<E> values) {
    super(name, values);
  }

  /**
   * This always returns false because either you want to filter down a collection by setting a value
   * or you set an empty list which means "select nothing".
   */
  @Override
  public boolean includeEverything() {
    return false;
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof FilterValuesEmptyIsNothing<?> && super.equals(obj);
  }
}
