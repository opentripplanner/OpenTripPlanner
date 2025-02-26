package org.opentripplanner.transit.api.model;

import java.util.Collection;

/**
 * {@link FilterValuesEmptyIsNothing} is a subclass of {@link FilterValues} that includes
 * everything if the values are null or empty.
 */
public class FilterValuesEmptyIsNothing<E> extends FilterValues<E> {

  FilterValuesEmptyIsNothing(String name, Collection<E> values) {
    super(name, values);
  }

  @Override
  public boolean includeEverything() {
    return false;
  }
}
