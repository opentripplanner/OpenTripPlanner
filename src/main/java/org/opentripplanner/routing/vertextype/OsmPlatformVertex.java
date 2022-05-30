package org.opentripplanner.routing.vertextype;

import java.util.Collection;
import java.util.Set;
import javax.annotation.Nullable;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.util.I18NString;
import org.opentripplanner.util.lang.ToStringBuilder;

public class OsmPlatformVertex extends OsmVertex {

  public final Set<String> references;

  public OsmPlatformVertex(
    Graph g,
    String label,
    double x,
    double y,
    long nodeId,
    @Nullable I18NString name,
    Collection<String> references
  ) {
    super(g, label, x, y, nodeId, name);
    this.references = Set.copyOf(references);
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(getClass()).addCol("references", references).toString();
  }
}
