package org.opentripplanner.transit.model.filter.selector;

import java.util.List;
import javax.annotation.Nullable;

/**
 * A filter for using select/not semantics.
 * <p>
 * Select: an item must match at least one select criterion (OR between selects).
 * Not: an item is excluded if it matches any not criterion.
 * A filter with no select and no not is not allowed.
 */
public interface SelectorBasedFilterRequest<TSelectRequest> {
  @Nullable
  List<TSelectRequest> select();

  @Nullable
  List<TSelectRequest> not();
}
