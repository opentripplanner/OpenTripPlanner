package org.opentripplanner.standalone.config.buildconfig;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.NA;

import java.net.URI;
import java.time.ZoneId;
import org.opentripplanner.graph_builder.module.osm.parameters.OsmDefaultParameters;
import org.opentripplanner.graph_builder.module.osm.parameters.OsmExtractParameters;
import org.opentripplanner.graph_builder.module.osm.parameters.OsmExtractParametersBuilder;
import org.opentripplanner.graph_builder.module.osm.parameters.OsmExtractParametersList;
import org.opentripplanner.graph_builder.module.osm.tagmapping.OsmTagMapper;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

/**
 * This class is responsible for mapping OSM configuration into OSM parameters.
 */
public class OsmConfig {

  public static OsmDefaultParameters mapOsmDefaults(NodeAdapter root, String parameterName) {
    var osmDefaults = root
      .of(parameterName)
      .since(NA)
      .summary("TODO")
      .description(/*TODO DOC*/"TODO")
      .asObject();

    return new OsmDefaultParameters(mapTagMapping(osmDefaults), mapTimeZone(osmDefaults));
  }

  public static OsmExtractParametersList mapOsmConfig(NodeAdapter root, String parameterName) {
    return new OsmExtractParametersList(
      root
        .of(parameterName)
        .since(NA)
        .summary("TODO")
        .description(/*TODO DOC*/"TODO")
        .asObjects(OsmConfig::mapOsmExtractConfig)
    );
  }

  private static OsmExtractParameters mapOsmExtractConfig(NodeAdapter config) {
    var builder = new OsmExtractParametersBuilder();
    builder.withSource(mapSource(config));
    builder.withTimeZone(mapTimeZone(config));
    builder.withOsmTagMapper(mapTagMapping(config));
    return builder.build();
  }

  private static URI mapSource(NodeAdapter config) {
    return config.of("source").since(NA).summary("TODO").asUri();
  }

  private static ZoneId mapTimeZone(NodeAdapter config) {
    return config.of("timeZone").since(NA).summary("TODO").asZoneId(null);
  }

  private static OsmTagMapper mapTagMapping(NodeAdapter node) {
    return node
      .of("osmTagMapping")
      .since(NA)
      .summary("TODO")
      .asEnum(OsmTagMapper.Source.DEFAULT)
      .getInstance();
  }
}
