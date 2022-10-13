package org.opentripplanner.standalone.config.feed;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.NA;

import java.net.URI;
import java.time.ZoneId;
import org.opentripplanner.graph_builder.module.osm.WayPropertySetSource;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

/**
 * This class is responsible for mapping OSM configuration into OSM parameters.
 */
public class OsmConfig {

  public static OsmExtractsConfig mapOsmConfig(NodeAdapter root, String parameterName) {
    return new OsmExtractsConfig(
      root
        .of(parameterName)
        .withDoc(NA, /*TODO DOC*/"TODO")
        .withExample(/*TODO DOC*/"TODO")
        .withDescription(/*TODO DOC*/"TODO")
        .asObjects(OsmConfig::mapOsmExtractConfig)
    );
  }

  public static OsmExtractConfig mapOsmExtractConfig(NodeAdapter config) {
    var builder = new OsmExtractConfigBuilder();
    builder.withSource(mapSource(config));
    builder.withTimeZone(mapTimeZone(config));
    builder.withOsmWayPropertySet(mapOsmWayPropertySet(config));
    return builder.build();
  }

  private static URI mapSource(NodeAdapter config) {
    return config
      .of("source")
      .withDoc(NA, /*TODO DOC*/"TODO")
      .withExample(/*TODO DOC*/"TODO")
      .asUri();
  }

  private static ZoneId mapTimeZone(NodeAdapter config) {
    return config.of("timeZone").withDoc(NA, /*TODO DOC*/"TODO").asZoneId(null);
  }

  private static WayPropertySetSource mapOsmWayPropertySet(NodeAdapter config) {
    return config
      .of("osmTagMapping")
      .withDoc(NA, /*TODO DOC*/"TODO")
      .withExample(/*TODO DOC*/"TODO")
      .asEnum(WayPropertySetSource.Source.DEFAULT)
      .getInstance();
  }
}
