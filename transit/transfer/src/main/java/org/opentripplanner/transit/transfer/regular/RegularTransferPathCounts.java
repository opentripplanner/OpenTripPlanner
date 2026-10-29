package org.opentripplanner.transit.transfer.regular;

/**
 * The number of paths of the generated regular transfers.
 *
 * @param paths    the paths of all the profiles, a path that a profile reuses from its parent is
 *                 counted for both
 * @param stored   the paths that are stored, the reused paths are stored once
 * @param profiles the number of profiles
 */
public record RegularTransferPathCounts(long paths, long stored, int profiles) {
  /** The paths that a profile reuses from its parent. */
  public long reused() {
    return paths - stored;
  }
}
