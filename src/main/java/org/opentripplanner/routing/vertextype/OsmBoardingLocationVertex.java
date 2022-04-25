package org.opentripplanner.routing.vertextype;

import java.util.Collection;
import java.util.Set;
import javax.annotation.Nullable;
import org.opentripplanner.model.base.ToStringBuilder;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.util.NonLocalizedString;

/**
 * A vertex for an OSM node that represents a transit stop and has a tag to cross-reference this to
 * a stop. OTP will treat this as an authoritative statement on where the transit stop is located
 * within the street network.
 *
 * The source of this location can be an OSM node (point) in which case the precise location is used.
 *
 * If the source is an area (way) then the centroid is computed and used.
 */
public class OsmBoardingLocationVertex extends OsmVertex {

  public final Set<String> references;

  /**
   * area centroids need to be linked separately
   */
  public final boolean isAlreadyLinked;

  public OsmBoardingLocationVertex(
    Graph g,
    String label,
    double x,
    double y,
    long nodeId,
    @Nullable String name,
    Collection<String> references,
    boolean isAlreadyLinked
  ) {
    super(g, label, x, y, nodeId, NonLocalizedString.ofNullable(name));
    this.references = Set.copyOf(references);
    this.isAlreadyLinked = isAlreadyLinked;
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(getClass()).addCol("references", references).toString();
  }
}
