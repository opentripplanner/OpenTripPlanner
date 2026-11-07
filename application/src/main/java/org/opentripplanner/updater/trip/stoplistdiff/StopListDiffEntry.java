package org.opentripplanner.updater.trip.stoplistdiff;

/** An entry in the result of {@link StopListDiff#diff}. */
public sealed interface StopListDiffEntry {
  /** The stop is in both lists. */
  record Kept(int originalIndex, int replacementIndex) implements StopListDiffEntry {}

  /** The stop is only in the original (scheduled) list, i.e. it is canceled. */
  record Removed(int originalIndex) implements StopListDiffEntry {}

  /** The stop is only in the replacement list, i.e. it is added. */
  record Added(int replacementIndex) implements StopListDiffEntry {}
}
