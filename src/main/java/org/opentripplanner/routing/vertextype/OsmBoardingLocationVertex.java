package org.opentripplanner.routing.vertextype;

import java.util.Collection;
import java.util.Set;
import javax.annotation.Nullable;
import org.opentripplanner.routing.edgetype.AreaEdgeList;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.util.NonLocalizedString;
import org.opentripplanner.util.lang.ToStringBuilder;

/**
 * A vertex for an OSM node that represents a transit stop and has a tag to cross-reference this to
 * a stop. OTP will treat this as an authoritative statement on where the transit stop is located
 * within the street network.
 * <p>
 * The source of this location can be an OSM node (point) in which case the precise location is
 * used.
 * <p>
 * If the source is an area (way) then the centroid is computed and used.
 */
public class OsmBoardingLocationVertex extends OsmVertex {

  public final Set<String> references;
  public final AreaEdgeList edgeList;

  public OsmBoardingLocationVertex(
    Graph g,
    String label,
    double x,
    double y,
    long nodeId,
    @Nullable String name,
    Collection<String> references,
    AreaEdgeList edgeList
  ) {
    super(g, label, x, y, nodeId, NonLocalizedString.ofNullable(name));
    this.references = Set.copyOf(references);
    this.edgeList = edgeList;
  }

  public boolean isConnectedToStreetNetwork() {
    return (getOutgoing().size() + getIncoming().size()) > 0;
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(getClass()).addCol("references", references).toString();
  }
}
