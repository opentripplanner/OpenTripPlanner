package org.opentripplanner.ext.edgenaming;

import org.opentripplanner.graph_builder.services.osm.DefaultNamer;
import org.opentripplanner.graph_builder.services.osm.EdgeNamer;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.standalone.config.framework.json.OtpVersion;

public class EdgeNamerFactory {

  /**
   * Create a custom namer if needed, return null if not found / by default.
   */
  public static EdgeNamer.EdgeNamerType fromConfig(NodeAdapter root, String parameterName) {
    return root
      .of(parameterName)
      .summary("A custom OSM namer to use.")
      .since(OtpVersion.V1_5)
      .asEnum(EdgeNamer.EdgeNamerType.DEFAULT);
  }

  /**
   * Create a custom namer if needed, return null if not found / by default.
   */
  public static EdgeNamer fromConfig(EdgeNamer.EdgeNamerType type) {
    if (type == null) {
      return new DefaultNamer();
    }
    return switch (type) {
      case PORTLAND -> new PortlandCustomNamer();
      case SIDEWALKS -> new SidewalkNamer();
      case SIDEWALKS_CROSSWALKS -> new SidewalkCrosswalkNamer();
      case DEFAULT -> new DefaultNamer();
    };
  }
}
