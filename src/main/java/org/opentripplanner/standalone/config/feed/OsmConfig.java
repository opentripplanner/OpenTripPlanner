package org.opentripplanner.standalone.config.feed;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.NA;

import org.opentripplanner.graph_builder.module.osm.WayPropertySetSource;
import org.opentripplanner.graph_builder.module.osm.parameters.OsmExtractConfig;
import org.opentripplanner.graph_builder.module.osm.parameters.OsmExtractConfigBuilder;
import org.opentripplanner.graph_builder.module.osm.parameters.OsmExtractsConfig;
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

    builder.withSource(
      config.of("source").withDoc(NA, /*TODO DOC*/"TODO").withExample(/*TODO DOC*/"TODO").asUri()
    );
    builder.withOsmWayPropertySet(
      config
        .of("osmTagMapping")
        .withDoc(NA, /*TODO DOC*/"TODO")
        .withExample(/*TODO DOC*/"TODO")
        .asEnum(WayPropertySetSource.Source.DEFAULT)
        .getInstance()
    );
    builder.withTimeZone(config.of("timeZone").withDoc(NA, /*TODO DOC*/"TODO").asZoneId(null));

    return builder.build();
  }
}
