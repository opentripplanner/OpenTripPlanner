package org.opentripplanner.graph_builder.services.osm;

import javax.annotation.Nonnull;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.graph_builder.module.osm.naming.DefaultNamer;
import org.opentripplanner.graph_builder.module.osm.naming.PortlandCustomNamer;
import org.opentripplanner.openstreetmap.model.OSMWithTags;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.standalone.config.framework.json.OtpVersion;
import org.opentripplanner.street.model.edge.StreetEdge;

/**
 * Interface responsible for naming edges of the street graph. It allows you to write your own
 * implementation if the default is for some reason not powerful enough.
 */
public interface EdgeNamer {
  /**
   * Get the edge name from an OSM way.
   */
  I18NString name(OSMWithTags way);

  /**
   * Callback function for each way/edge combination so that more complicated names can be built
   * in the post-processing step.
   */
  void recordEdge(OSMWithTags way, StreetEdge edge);

  /**
   * Called after each edge has been named to build a more complex name out of the relationships
   * tracked in {@link EdgeNamer#recordEdge(OSMWithTags, StreetEdge)}.
   */
  void postprocess();

  default I18NString getNameForWay(OSMWithTags way, @Nonnull String id) {
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
        .since(OtpVersion.V2_0)
        .asString(null);
      return fromConfig(osmNaming);
    }

    /**
     * Create a custom namer if needed, return null if not found / by default.
     */
    public static EdgeNamer fromConfig(String type) {
      if (type == null) {
        return new DefaultNamer();
      }

      return switch (type) {
        case "portland" -> new PortlandCustomNamer();
        default -> throw new IllegalArgumentException(
          String.format("Unknown osmNaming type: '%s'", type)
        );
      };
    }
  }
}
