package org.opentripplanner.routing.util;

import java.util.ArrayList;

public class DiffList<T> extends ArrayList<DiffEntry<T>> {

  /* All elements exist in both collections, hence no differences. */
  public boolean isEqual() {
    return stream().allMatch(DiffEntry::isEqual);
  }
}
