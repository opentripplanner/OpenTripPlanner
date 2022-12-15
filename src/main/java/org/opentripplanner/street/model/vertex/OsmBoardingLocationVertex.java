package org.opentripplanner.street.model.vertex;

import java.util.Collection;
import java.util.Set;
import javax.annotation.Nullable;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.framework.tostring.ToStringBuilder;
import org.opentripplanner.routing.graph.Graph;

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
public class OsmBoardingLocationVertex extends IntersectionVertex {

  public final Set<String> references;

  public OsmBoardingLocationVertex(
    Graph g,
    String label,
    double x,
    double y,
    @Nullable I18NString name,
    Collection<String> references
  ) {
    super(g, label, x, y, name);
    this.references = Set.copyOf(references);
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(getClass()).addCol("references", references).toString();
  }

  public boolean isConnectedToStreetNetwork() {
    return (getOutgoing().size() + getIncoming().size()) > 0;
  }
}
