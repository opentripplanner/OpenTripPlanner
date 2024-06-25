package org.opentripplanner.generate.doc;

import static org.opentripplanner.framework.io.FileUtils.assertFileEquals;
import static org.opentripplanner.framework.io.FileUtils.readFile;
import static org.opentripplanner.framework.io.FileUtils.writeFile;
import static org.opentripplanner.framework.text.MarkdownFormatter.bold;
import static org.opentripplanner.generate.doc.framework.DocsTestConstants.DOCS_ROOT;
import static org.opentripplanner.generate.doc.framework.DocsTestConstants.TEMPLATE_ROOT;
import static org.opentripplanner.generate.doc.framework.TemplateUtil.replaceSection;
import static org.opentripplanner.openstreetmap.tagmapping.OsmTagMapperSource.CONSTANT_SPEED_FINLAND;
import static org.opentripplanner.openstreetmap.tagmapping.OsmTagMapperSource.HAMBURG;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.framework.text.Table;
import org.opentripplanner.framework.text.TableBuilder;
import org.opentripplanner.generate.doc.framework.DocBuilder;
import org.opentripplanner.generate.doc.framework.GeneratesDocumentation;
import org.opentripplanner.openstreetmap.tagmapping.OsmTagMapper;
import org.opentripplanner.openstreetmap.tagmapping.OsmTagMapperSource;
import org.opentripplanner.openstreetmap.wayproperty.SafetyFeatures;
import org.opentripplanner.openstreetmap.wayproperty.WayPropertyPicker;
import org.opentripplanner.openstreetmap.wayproperty.WayPropertySet;

@GeneratesDocumentation
public class OsmMapperDocTest {

  private static final String FILE_NAME = "OsmMapper.md";
  private static final File TEMPLATE = new File(TEMPLATE_ROOT, FILE_NAME);

  public static List<OsmTagMapperSource> mappers() {
    return Arrays
      .stream(OsmTagMapperSource.values())
      .filter(m -> !Set.of(HAMBURG, CONSTANT_SPEED_FINLAND).contains(m))
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
    template = replaceSection(template, "prop-details", propDetails(wps));
    template = replaceSection(template, "mixins", mixinTable.toMarkdownTable());
    writeFile(outFile, template);
    assertFileEquals(original, outFile);
  }

  private static File outputFile(OsmTagMapper mapper) {
    var name = mapper.getClass().getSimpleName().replaceAll("Mapper", ".md");
    return new File("%s/osm/".formatted(DOCS_ROOT), name);
  }

  private static Table propTable(WayPropertySet wps) {
    var propTable = new TableBuilder();
    propTable.withHeaders("specifier", "permission", "safety");

    for (var prop : wps.getWayProperties()) {
      propTable.addRow(
        "`%s`".formatted(prop.specifier().toMarkdown()),
        prop.properties().getPermission(),
        emojiModifications(prop.properties().bicycleSafety(), prop.properties().walkSafety())
      );
    }
    return propTable.build();
  }

  private static String propDetails(WayPropertySet wps) {
    var docBuilder = new DocBuilder();

    var wayProperties = wps.getWayProperties();
    for (var prop : wayProperties) {
      var index = wayProperties.indexOf(prop);

      docBuilder.header(3, "Rule #%s".formatted(index), Integer.toString(index));
      docBuilder
        .text(bold("Specifier:"))
        .text("`%s`".formatted(prop.specifier().toMarkdown()))
        .lineBreak();

      docBuilder.text(bold("Permission:")).text(prop.properties().getPermission());
      docBuilder.lineBreak();
      var bike = prop.properties().bicycleSafety();
      docBuilder
        .text(bold("Bike safety factor:"))
        .text("forward: %s, back: %s".formatted(bike.forward(), bike.back()));
      docBuilder.lineBreak();
      var walk = prop.properties().walkSafety();
      docBuilder
        .text(bold("Walk safety factor:"))
        .text("forward: %s, back: %s".formatted(walk.forward(), walk.back()));
      docBuilder.endParagraph();
    }
    return docBuilder.toString();
  }

  private static String hash(WayPropertyPicker prop) {
    return prop.specifier().toMarkdown().replaceAll(" ", "").toLowerCase();
  }

  private static Table mixinTable(WayPropertySet wps) {
    var propTable = new TableBuilder();
    propTable.withHeaders("matcher", "modifications");

    for (var prop : wps.getMixins()) {
      propTable.addRow(
        "`%s`".formatted(prop.specifier().toMarkdown()),
        emojiModifications(prop.bicycleSafety(), prop.walkSafety())
      );
    }
    return propTable.build();
  }

  private static String emojiModifications(SafetyFeatures bicycle, SafetyFeatures walk) {
    return emojiIfModifies(bicycle, "\uD83D\uDEB4") + " " + emojiIfModifies(walk, "\uD83D\uDEB6");
  }

  private static String emojiIfModifies(SafetyFeatures prop, String value) {
    if (prop.modifies()) {
      return value;
    } else {
      return "";
    }
  }
}
