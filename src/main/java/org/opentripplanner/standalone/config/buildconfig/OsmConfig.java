package org.opentripplanner.standalone.config.buildconfig;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_2;

import org.opentripplanner.graph_builder.module.osm.parameters.OsmExtractParameters;
import org.opentripplanner.graph_builder.module.osm.parameters.OsmExtractParametersBuilder;
import org.opentripplanner.graph_builder.module.osm.parameters.OsmExtractParametersList;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

/**
 * This class is responsible for mapping OSM configuration into OSM parameters.
 */
public class OsmConfig {

  public static OsmExtractParameters mapOsmDefaults(NodeAdapter root, String parameterName) {
    var osmDefaults = root
      .of(parameterName)
      .since(V2_2)
      .summary("Default properties for OpenStreetMap feeds.")
      .asObject();

    return mapOsmGenericParameters(osmDefaults);
  }

  public static OsmExtractParametersList mapOsmConfig(
    NodeAdapter root,
    String parameterName,
    OsmExtractParameters defaults
  ) {
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
        .asObjects(nodeAdapter -> mapOsmParameters(nodeAdapter, defaults))
    );
  }

  public static OsmExtractParameters mapOsmParameters(
    NodeAdapter node,
    OsmExtractParameters defaults
  ) {
    return defaults
      .copyOf()
      .withSource(
        node.of("source").since(V2_2).summary("The unique URI pointing to the data file.").asUri()
      )
      .withOsmTagMapper(
        node
          .of("osmTagMapping")
          .since(V2_2)
          .summary(
            "The named set of mapping rules applied when parsing OSM tags. Overrides the value specified in `osmDefaults`."
          )
          .asEnum(defaults.osmTagMapper())
      )
      .withTimeZone(
        node
          .of("timeZone")
          .since(V2_2)
          .summary(
            "The timezone used to resolve opening hours in OSM data. Overrides the value specified in `osmDefaults`."
          )
          .asZoneId(defaults.timeZone().orElse(null))
      )
      .build();
  }

  public static OsmExtractParameters mapOsmGenericParameters(NodeAdapter node) {
    return new OsmExtractParametersBuilder()
      .withOsmTagMapper(
        node
          .of("osmTagMapping")
          .since(V2_2)
          .summary("The named set of mapping rules applied when parsing OSM tags.")
          .asEnum(OsmExtractParameters.DEFAULT_OSM_TAG_MAPPER)
      )
      .withTimeZone(
        node
          .of("timeZone")
          .since(V2_2)
          .summary("The timezone used to resolve opening hours in OSM data.")
          .asZoneId(null)
      )
      .build();
  }
}
