package org.opentripplanner.graph_builder.services.osm;

import com.fasterxml.jackson.databind.JsonNode;
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
 * For when CreativeNamePicker/WayPropertySet is just not powerful enough.
 *
 * @author novalis
 */
public interface WayNamer {
  I18NString name(OSMWithTags way);

  void nameWithEdge(OSMWithTags way, StreetEdge edge);

  void postprocess();

  default I18NString getNameForWay(OSMWithTags way, @Nonnull String id) {
    var name = name(way);

    if (name == null) {
      name = new NonLocalizedString(id);
    }
    return name;
  }

  class WayNamerFactory {

    /**
     * Create a custom namer if needed, return null if not found / by default.
     */
    public static WayNamer fromConfig(NodeAdapter root, String parameterName) {
      var osmNaming = root
        .of(parameterName)
        .summary("A custom OSM namer to use.")
        .since(OtpVersion.V2_0)
        .asObject();
      return fromConfig(osmNaming.rawNode());
    }

    /**
     * Create a custom namer if needed, return null if not found / by default.
     */
    public static WayNamer fromConfig(JsonNode config) {
      String type = null;
      if (config == null) {
        /* Empty block, fallback to default */
        return null;
      } else if (config.isTextual()) {
        /* Simplest form: { osmNaming : "portland" } */
        type = config.asText();
      } else if (config.has("type")) {
        /* Custom namer with a type: { osmNaming : { type : "foobar", param1 : 42 } } */
        type = config.path("type").asText(null);
      }
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
