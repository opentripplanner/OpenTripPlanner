package org.opentripplanner.routing.vertextype;

import javax.annotation.Nullable;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.util.NonLocalizedString;

/**
 * A vertex for an OSM node that represents a transit stop and has a ref=(stop_code) tag. OTP will
 * treat this as an authoritative statement on where the transit stop is located within the street
 * network, and the GTFS stop vertex will be linked to exactly this location.
 */
public class OsmBoardingLocationVertex extends OsmVertex {

  public final String reference;

  public OsmBoardingLocationVertex(
    Graph g,
    String label,
    double x,
    double y,
    long nodeId,
    @Nullable String name,
    String reference
  ) {
    super(g, label, x, y, nodeId, NonLocalizedString.ofNullable(name));
    this.reference = reference;
  }
}
