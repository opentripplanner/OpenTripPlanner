package org.opentripplanner.graph_builder.module.osm.naming;

import gnu.trove.list.TLongList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.graph_builder.module.osm.StreetEdgePair;
import org.opentripplanner.graph_builder.services.osm.EdgeNamer;
import org.opentripplanner.osm.model.OsmEntity;
import org.opentripplanner.osm.model.OsmWay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A namer that assigns names to crosswalks using the name of the crossed street, if available.
 * <p>
 * The algorithm works as follows:
 *  - For each crosswalk, we find the intersecting street edge that shares a node.
 *  - (To be completed)
 * <p>
 * In North America, it is common for a street to have turn lanes (slip lanes)
 * that are their own OSM ways because they are separate from the main streets they join.
 * The resulting crosswalk name is "Crossing over turn lane" or equivalent in other locales.
 */
public class CrosswalkNamer implements EdgeNamer {

  private static final Logger LOG = LoggerFactory.getLogger(CrosswalkNamer.class);

  @Override
  public I18NString name(OsmEntity way) {
    return way.getAssumedName();
  }

  @Override
  public void recordEdges(OsmEntity way, StreetEdgePair pair) {

  }

  @Override
  public void postprocess() {

  }

  /** Gets the intersecting street, if any, for the given way and candidate streets. */
  public static Optional<OsmWay> getIntersectingStreet(OsmWay way, List<OsmWay> streets) {
    TLongList nodeRefs = way.getNodeRefs();
    if (nodeRefs.size() >= 3) {
      // There needs to be at least three nodes: 2 extremities that are on the sidewalk,
      // and one somewhere in the middle that joins the crossing with the street.
      // We exclude the first and last node which are on the sidewalk.
      long[] nodeRefsArray = nodeRefs.toArray(1, nodeRefs.size() - 2);
      return streets
        .stream()
        .filter(w -> Arrays.stream(nodeRefsArray).anyMatch(nid -> w.getNodeRefs().contains(nid)))
        .findFirst();
    }
    return Optional.empty();
  }
}
