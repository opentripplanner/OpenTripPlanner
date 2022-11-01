package org.opentripplanner.standalone.config.buildconfig;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_2;

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
      .since(V2_2)
      .summary("Default properties for OpenStreetMap feeds.")
      .asObject();

    return new OsmDefaultParameters(mapTagMapping(osmDefaults), mapTimeZone(osmDefaults));
  }

  public static OsmExtractParametersList mapOsmConfig(NodeAdapter root, String parameterName) {
    return new OsmExtractParametersList(
      root
        .of(parameterName)
        .since(V2_2)
        .summary("Configure properties for a given OpenStreetMap feed.")
        .description(
          """
          The osm section of build-config.json allows you to override the default behavior of scanning
          for OpenStreetMap files in the base directory. You can specify data located outside the 
          local filesystem (including cloud storage services) or at various different locations around 
          the local filesystem.
          """
        )
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
    return config
      .of("source")
      .since(V2_2)
      .summary("The unique URI pointing to the data file.")
      .asUri();
  }

  private static ZoneId mapTimeZone(NodeAdapter config) {
    return config
      .of("timeZone")
      .since(V2_2)
      .summary(
        "The timezone used to resolve opening hours in OSM data. Overrides the value specified in osmDefaults."
      )
      .asZoneId(null);
  }

  private static OsmTagMapper mapTagMapping(NodeAdapter node) {
    return node
      .of("osmTagMapping")
      .since(V2_2)
      .summary("The named set of mapping rules applied when parsing OSM tags.")
      .asEnum(OsmTagMapper.Source.DEFAULT)
      .getInstance();
  }
}
