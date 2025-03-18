package org.opentripplanner.standalone.config.buildconfig;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_2;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_7;

import org.opentripplanner.graph_builder.module.osm.parameters.OsmExtractParameters;
import org.opentripplanner.graph_builder.module.osm.parameters.OsmExtractParametersBuilder;
import org.opentripplanner.graph_builder.module.osm.parameters.OsmExtractParametersList;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

/**
 * This class is responsible for mapping OSM configuration into OSM parameters.
 */
public class OsmConfig {

  public static OsmExtractParameters mapOsmDefaults(NodeAdapter root, String parameterName) {
    var baseDefaults = OsmExtractParameters.DEFAULT;
    var osmDefaults = root
      .of(parameterName)
      .since(V2_2)
      .summary("Default properties for OpenStreetMap feeds.")
      .asObject();

    return mapOsmGenericParameters(osmDefaults, baseDefaults, "").build();
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
    String documentationAddition = " Overrides the value specified in `osmDefaults`.";
    return mapOsmGenericParameters(node, defaults, documentationAddition)
      .withSource(
        node.of("source").since(V2_2).summary("The unique URI pointing to the data file.").asUri()
      )
      .build();
  }

  public static OsmExtractParametersBuilder mapOsmGenericParameters(
    NodeAdapter node,
    OsmExtractParameters defaults,
    String documentationAddition
  ) {
    var docDefaults = OsmExtractParameters.DEFAULT;
    return defaults
      .copyOf()
      .withOsmTagMapper(
        node
          .of("osmTagMapping")
          .since(V2_2)
          .summary(
            "The named set of mapping rules applied when parsing OSM tags." + documentationAddition
          )
          .docDefaultValue(docDefaults.osmTagMapper())
          .asEnum(defaults.osmTagMapper())
      )
      .withTimeZone(
        node
          .of("timeZone")
          .since(V2_2)
          .summary(
            "The timezone used to resolve opening hours in OSM data." + documentationAddition
          )
          .docDefaultValue(docDefaults.timeZone())
          .asZoneId(defaults.timeZone())
      )
      .withIncludeOsmSubwayEntrances(
        node
          .of("includeOsmSubwayEntrances")
          .since(V2_7)
          .summary("Whether to include subway entrances from the OSM data." + documentationAddition)
          .docDefaultValue(docDefaults.includeOsmSubwayEntrances())
          .asBoolean(defaults.includeOsmSubwayEntrances())
      );
  }
}
