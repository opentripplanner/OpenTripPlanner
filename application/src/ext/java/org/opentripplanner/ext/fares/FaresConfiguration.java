package org.opentripplanner.ext.fares;

import com.fasterxml.jackson.databind.JsonNode;
import org.opentripplanner.ext.fares.impl.AtlantaFareServiceFactory;
import org.opentripplanner.ext.fares.impl.CombineInterlinedLegsFactory;
import org.opentripplanner.ext.fares.impl.DefaultFareServiceFactory;
import org.opentripplanner.ext.fares.impl.HSLFareServiceFactory;
import org.opentripplanner.ext.fares.impl.HighestFareInFreeTransferWindowFareServiceFactory;
import org.opentripplanner.ext.fares.impl.NoopFareServiceFactory;
import org.opentripplanner.ext.fares.impl.OrcaFareFactory;
import org.opentripplanner.routing.fares.FareServiceFactory;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.standalone.config.framework.json.OtpVersion;

public class FaresConfiguration {

  public static JsonNode fromConfig(NodeAdapter root, String parameterName) {
    // Fares uses the raw node, not the types-safe adapter, but defining the fares root here
    // will cause fares to be added to the build-config configuration document with a link to the
    // Fares.md.
    return root
      .of(parameterName)
      .summary("Fare configuration.")
      .since(OtpVersion.V2_0)
      .asObject()
      .rawNode();
  }

  /**
   * Build a specific FareServiceFactory given the config node, or fallback to the default if none
   * specified.
   * <p>
   * Accept different formats. Examples:
   *
   * <pre>
   * { fares : "seattle" }
   * --------------------------
   * { fares : {} } // Fallback to default
   * --------------------------
   * { fares : {
   *       type : "foobar",
   *       param1 : 42
   * } }
   * </pre>
   */
  public static FareServiceFactory fromConfig(JsonNode config) {
    String type = null;
    if (config == null) {
      /* Empty block, fallback to default */
      type = null;
    } else if (config.isTextual()) {
      /* Simplest form: { fares : "seattle" } */
      type = config.asText();
    } else if (config.has("type")) {
      /* Fare with a type: { fares : { type : "foobar", param1 : 42 } } */
      type = config.path("type").asText(null);
    }

    if (type == null) {
      type = "default";
    }

    FareServiceFactory factory = createFactory(type);
    factory.configure(config);
    return factory;
  }

  private static FareServiceFactory createFactory(String type) {
    return switch (type) {
      case "default" -> new DefaultFareServiceFactory();
      case "off" -> new NoopFareServiceFactory();
      case "highest-fare-in-free-transfer-window",
        "highestFareInFreeTransferWindow" -> new HighestFareInFreeTransferWindowFareServiceFactory();
      case "hsl" -> new HSLFareServiceFactory();
      case "atlanta" -> new AtlantaFareServiceFactory();
      case "orca" -> new OrcaFareFactory();
      case "combine-interlined-legs" -> new CombineInterlinedLegsFactory();
      default -> throw new IllegalArgumentException(String.format("Unknown fare type: '%s'", type));
    };
  }
}
