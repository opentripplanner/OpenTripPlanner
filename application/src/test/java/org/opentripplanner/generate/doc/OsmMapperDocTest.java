package org.opentripplanner.generate.doc;

import static org.opentripplanner.framework.io.FileUtils.assertFileEquals;
import static org.opentripplanner.framework.io.FileUtils.readFile;
import static org.opentripplanner.framework.io.FileUtils.writeFile;
import static org.opentripplanner.generate.doc.framework.DocsTestConstants.TEMPLATE_PATH;
import static org.opentripplanner.generate.doc.framework.DocsTestConstants.USER_DOC_PATH;
import static org.opentripplanner.generate.doc.framework.TemplateUtil.replaceSection;
import static org.opentripplanner.osm.tagmapping.OsmTagMapperSource.ATLANTA;
import static org.opentripplanner.osm.tagmapping.OsmTagMapperSource.CONSTANT_SPEED_FINLAND;
import static org.opentripplanner.osm.tagmapping.OsmTagMapperSource.HAMBURG;
import static org.opentripplanner.osm.tagmapping.OsmTagMapperSource.HOUSTON;
import static org.opentripplanner.osm.tagmapping.OsmTagMapperSource.PORTLAND;
import static org.opentripplanner.street.model.StreetTraversalPermission.NONE;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.generate.doc.framework.GeneratesDocumentation;
import org.opentripplanner.osm.tagmapping.OsmTagMapper;
import org.opentripplanner.osm.tagmapping.OsmTagMapperSource;
import org.opentripplanner.osm.wayproperty.WayPropertySet;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.utils.text.Table;
import org.opentripplanner.utils.text.TableBuilder;

@GeneratesDocumentation
public class OsmMapperDocTest {

  private static final String FILE_NAME = "OsmMapper.md";
  private static final File TEMPLATE = new File(TEMPLATE_PATH, FILE_NAME);
  private static final Set<OsmTagMapperSource> SKIP_MAPPERS = Set.of(
    ATLANTA,
    HOUSTON,
    PORTLAND,
    HAMBURG,
    CONSTANT_SPEED_FINLAND
  );

  public static List<OsmTagMapperSource> mappers() {
    return Arrays.stream(OsmTagMapperSource.values())
      .filter(m -> !SKIP_MAPPERS.contains(m))
      .toList();
  }

  @ParameterizedTest
  @MethodSource("mappers")
  public void updateDocs(OsmTagMapperSource source) {
    var mapper = source.getInstance();
    var wps = new WayPropertySet();
    mapper.populateProperties(wps);

    var outFile = outputFile(mapper);

    // Read and close input file (same as output file)
    String template = readFile(TEMPLATE);
    String original = readFile(outFile);

    var propTable = propTable(wps);
    var mixinTable = mixinTable(wps);

    template = replaceSection(template, "props", propTable.toMarkdownTable());
    template = replaceSection(template, "mixins", mixinTable.toMarkdownTable());
    writeFile(outFile, template);
    assertFileEquals(original, outFile);
  }

  private static File outputFile(OsmTagMapper mapper) {
    var name = mapper.getClass().getSimpleName().replaceAll("Mapper", ".md");
    return new File("%s/osm/".formatted(USER_DOC_PATH), name);
  }

  private static Table propTable(WayPropertySet wps) {
    var propTable = new TableBuilder();
    propTable.withHeaders("specifier", "permission", "bike safety", "walk safety");

    for (var prop : wps.getWayProperties()) {
      propTable.addRow(
        "`%s`".formatted(prop.specifier().toDocString()),
        "`%s`".formatted(prop.properties().getPermission()),
        tableValues(
          prop.properties().bicycleSafety(),
          prop.forwardProperties().bicycleSafety(),
          prop.backwardProperties().bicycleSafety()
        ),
        tableValues(
          prop.properties().walkSafety(),
          prop.forwardProperties().walkSafety(),
          prop.backwardProperties().walkSafety()
        )
      );
    }
    return propTable.build();
  }

  private static Table mixinTable(WayPropertySet wps) {
    var propTable = new TableBuilder();
    propTable.withHeaders(
      "matcher",
      "add permission",
      "remove permission",
      "bicycle safety",
      "walk safety"
    );

    for (var prop : wps.getMixins()) {
      propTable.addRow(
        "`%s`".formatted(prop.specifier().toDocString()),
        tableValues(
          prop.directionlessProperties().addedPermission(),
          prop.forwardProperties().addedPermission(),
          prop.backwardProperties().addedPermission()
        ),
        tableValues(
          prop.directionlessProperties().removedPermission(),
          prop.forwardProperties().removedPermission(),
          prop.backwardProperties().removedPermission()
        ),
        tableValues(
          prop.directionlessProperties().bicycleSafety(),
          prop.forwardProperties().bicycleSafety(),
          prop.backwardProperties().bicycleSafety()
        ),
        tableValues(
          prop.directionlessProperties().walkSafety(),
          prop.forwardProperties().walkSafety(),
          prop.backwardProperties().walkSafety()
        )
      );
    }
    return propTable.build();
  }

  private static String tableValues(
    StreetTraversalPermission value,
    StreetTraversalPermission forward,
    StreetTraversalPermission backward
  ) {
    if (value == NONE && forward == NONE && backward == NONE) {
      return "";
    } else if (value == forward && value == backward) {
      return value.toString();
    } else {
      StringBuilder result = new StringBuilder();
      if (value != NONE) {
        result.append("no direction: ").append(value);
      }
      if (forward != NONE) {
        if (!result.isEmpty()) {
          result.append("<br>");
        }
        result.append("forward: ").append(forward);
      }
      if (backward != NONE) {
        if (!result.isEmpty()) {
          result.append("<br>");
        }
        result.append("backward: ").append(backward);
      }
      return result.toString();
    }
  }

  private static String tableValues(double value, double forward, double backward) {
    if (value == 1.0 && forward == 1.0 && backward == 1.0) {
      return "";
    } else if (value == forward && value == backward) {
      return Double.toString(value);
    } else {
      return "no direction: %s <br> forward: %s <br> back: %s".formatted(value, forward, backward);
    }
  }
}
