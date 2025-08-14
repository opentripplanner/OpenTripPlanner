package org.opentripplanner.routing.linking;

/**
 * Whether so-called "visibility lines" should be computed, which are used when navigating through
 * open areas.
 */
public enum VisibilityMode {
  /**
   *
   * "Visibility lines" are computed and the path crosses "through" the area in the shortest way
   * possible while also taking obstacles into account. This is slower to compute.
   */
  COMPUTE_AREA_VISIBILITY_LINES,
  /**
   * Areas are not crossed, but the edges at around the borders are traversed instead, leading to results
   * that might be seen as detours. However, this mode is faster to compute.
   */
  TRAVERSE_AREA_EDGES;

  /**
   * Returns {@link VisibilityMode#COMPUTE_AREA_VISIBILITY_LINES} if true,
   * {@link VisibilityMode#TRAVERSE_AREA_EDGES} otherwise.
   */
  public static VisibilityMode ofBoolean(boolean computeVisibility) {
    if (computeVisibility) {
      return COMPUTE_AREA_VISIBILITY_LINES;
    } else {
      return TRAVERSE_AREA_EDGES;
    }
  }
}
