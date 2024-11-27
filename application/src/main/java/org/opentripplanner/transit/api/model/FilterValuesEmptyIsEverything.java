package org.opentripplanner.transit.api.model;

import java.util.Collection;

/**
 * {@link FilterValuesEmptyIsEverything} is a subclass of {@link FilterValues} that includes
 * everything if the values are null or empty.
 */
public class FilterValuesEmptyIsEverything<E> extends FilterValues<E> {

  FilterValuesEmptyIsEverything(String name, Collection<E> values) {
    super(name, values);
  }

  @Override
  public boolean includeEverything() {
    return values == null || values.isEmpty();
  }
}
