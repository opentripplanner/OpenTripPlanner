package org.opentripplanner.graph_builder.services.osm;

import java.util.Optional;
import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.graph_builder.module.osm.OsmDatabase;
import org.opentripplanner.graph_builder.module.osm.StreetEdgePair;
import org.opentripplanner.osm.model.OsmEntity;
import org.opentripplanner.osm.model.OsmWay;

/**
 * Interface responsible for naming edges of the street graph. It allows you to write your own
 * implementation if the default is for some reason not powerful enough.
 */
public interface EdgeNamer {
  /**
   * Get the edge name from an OSM relation or way.
   */
  I18NString name(OsmEntity entity);

  /**
   * Callback function for each way/edge combination so that more complicated names can be built
   * in the post-processing step.
   */
  void recordEdges(OsmWay way, StreetEdgePair edge, OsmDatabase osmdb);

  /**
   * Called after each edge has been named to build a more complex name out of the relationships
   * tracked in {@link EdgeNamer#recordEdges(OsmWay, StreetEdgePair, OsmDatabase)}.
   */
  void finalizeNames();

  /**
   * Get the edge name.
   *
   * @param entity relation or way from which to get the name
   */
  default Optional<I18NString> getName(OsmEntity entity) {
    return Optional.ofNullable(name(entity));
  }

  enum EdgeNamerType {
    DEFAULT,
    PORTLAND,
    SIDEWALKS,
    SIDEWALKS_CROSSWALKS,
  }
}
