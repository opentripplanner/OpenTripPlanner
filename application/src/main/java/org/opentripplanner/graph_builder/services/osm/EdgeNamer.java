package org.opentripplanner.graph_builder.services.osm;

import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.graph_builder.module.osm.StreetEdgePair;
import org.opentripplanner.graph_builder.module.osm.naming.DefaultNamer;
import org.opentripplanner.graph_builder.module.osm.naming.PortlandCustomNamer;
import org.opentripplanner.graph_builder.module.osm.naming.SidewalkNamer;
import org.opentripplanner.osm.model.OsmEntity;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.standalone.config.framework.json.OtpVersion;

/**
 * Interface responsible for naming edges of the street graph. It allows you to write your own
 * implementation if the default is for some reason not powerful enough.
 */
public interface EdgeNamer {
  /**
   * Get the edge name from an OSM way.
   */
  I18NString name(OsmEntity way);

  /**
   * Callback function for each way/edge combination so that more complicated names can be built
   * in the post-processing step.
   */
  void recordEdges(OsmEntity way, StreetEdgePair edge);

  /**
   * Called after each edge has been named to build a more complex name out of the relationships
   * tracked in {@link EdgeNamer#recordEdges(OsmEntity, StreetEdgePair)}.
   */
  void postprocess();

  default I18NString getNameForWay(OsmEntity way, String id) {
    var name = name(way);

    if (name == null) {
      name = new NonLocalizedString(id);
    }
    return name;
  }

  class EdgeNamerFactory {

    /**
     * Create a custom namer if needed, return null if not found / by default.
     */
    public static EdgeNamer fromConfig(NodeAdapter root, String parameterName) {
      var osmNaming = root
        .of(parameterName)
        .summary("A custom OSM namer to use.")
        .since(OtpVersion.V1_5)
        .asEnum(EdgeNamerType.DEFAULT);
      return fromConfig(osmNaming);
    }

    /**
     * Create a custom namer if needed, return null if not found / by default.
     */
    public static EdgeNamer fromConfig(EdgeNamerType type) {
      if (type == null) {
        return new DefaultNamer();
      }
      return switch (type) {
        case PORTLAND -> new PortlandCustomNamer();
        case SIDEWALKS -> new SidewalkNamer();
        case DEFAULT -> new DefaultNamer();
      };
    }
  }

  enum EdgeNamerType {
    DEFAULT,
    PORTLAND,
    SIDEWALKS,
  }
}
