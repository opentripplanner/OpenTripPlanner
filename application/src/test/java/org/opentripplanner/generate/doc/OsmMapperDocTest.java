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

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.framework.text.Table;
import org.opentripplanner.framework.text.TableBuilder;
import org.opentripplanner.generate.doc.framework.GeneratesDocumentation;
import org.opentripplanner.osm.tagmapping.OsmTagMapper;
import org.opentripplanner.osm.tagmapping.OsmTagMapperSource;
import org.opentripplanner.osm.wayproperty.SafetyFeatures;
import org.opentripplanner.osm.wayproperty.WayPropertySet;

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
    return Arrays
      .stream(OsmTagMapperSource.values())
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
        tableValues(prop.properties().bicycleSafety()),
        tableValues(prop.properties().walkSafety())
      );
    }
    return propTable.build();
  }

  private static Table mixinTable(WayPropertySet wps) {
    var propTable = new TableBuilder();
    propTable.withHeaders("matcher", "bicycle safety", "walk safety");

    for (var prop : wps.getMixins()) {
      propTable.addRow(
        "`%s`".formatted(prop.specifier().toDocString()),
        tableValues(prop.bicycleSafety()),
        tableValues(prop.walkSafety())
      );
    }
    return propTable.build();
  }

  private static String tableValues(SafetyFeatures safety) {
    if (!safety.modifies()) {
      return "";
    } else if (safety.isSymmetric()) {
      return Double.toString(safety.forward());
    } else {
      return "forward: %s <br> back: %s".formatted(safety.forward(), safety.back());
    }
  }
}
