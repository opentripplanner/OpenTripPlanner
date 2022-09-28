package org.opentripplanner.standalone.config.framework;

import static org.opentripplanner.standalone.config.framework.JsonSupport.jsonNodeForTest;
import static org.opentripplanner.standalone.config.framework.doc.DeprecatedParametersTable.createDeprecatedParametersTable;
import static org.opentripplanner.standalone.config.framework.doc.ParameterDetailsList.listParametersWithDetails;
import static org.opentripplanner.standalone.config.framework.doc.ParametersTable.createParametersTable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Set;
import org.opentripplanner.standalone.config.BuildConfig;
import org.opentripplanner.standalone.config.framework.doc.MarkDownDocWriter;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.standalone.config.framework.json.NodeInfo;

public class BuildConfigUserGuideDocPrinter {

  private static final Set<String> SKIP_OBJECTS = Set.of("dataOverlay");

  private final MarkDownDocWriter out;
  private final BuildConfigTemplate template;

  public BuildConfigUserGuideDocPrinter(BuildConfigTemplate template, MarkDownDocWriter out) {
    this.template = template;
    this.out = out;
  }

  public static void main(String[] args) throws FileNotFoundException {
    var out = new PrintStream(
      new FileOutputStream(new File("docs", "BuildConfiguration-generated.md"))
    );
    var printer = new BuildConfigUserGuideDocPrinter(
      new BuildConfigTemplate(),
      new MarkDownDocWriter(out)
    );

    // We need to include all optional nested types we want to in the document, and
    // at least one element need to added to nested objects as well (can be an unknown like {@code a:""})
    // All required fields need to be included
    var json =
      """
      {
        boardingLocationTags : [ { a:1 } ],
        dem : [ { source:1 } ],
        elevationBucket : { accessKey:1, secretKey:1, bucketName:1 },
        localFileNamePatterns : { a:1 },
        netexDefaults : { a:1 },
        osm : [{ source:1 }],
        osmDefaults : { a:1 },
        transferRequests : [{}],
        transitFeeds : [{ type:1}]
      }
      """;

    var conf = new BuildConfig(jsonNodeForTest(json), "build.config", false);
    printer.write(conf.asNodeAdapter());
  }

  public void write(NodeAdapter node) {
    out.printDocTitle(template.title());
    out.printSection(template.introduction());

    out.printHeader1("Overview");
    out.printSection(template.overview());

    out.printHeader2("Parameters");
    out.printSection(template.parameters());
    createParametersTable(node, out, this::skipObject);

    out.printHeader2("Deprecated Parameters");
    out.printSection(template.parametersDeprecated());
    createDeprecatedParametersTable(node, out, this::skipObject);

    out.printHeader1("Parameter Details");
    out.printSection(template.parameterDetails());
    listParametersWithDetails(node, out, this::skipObject);
  }

  private boolean skipObject(NodeInfo it) {
    return SKIP_OBJECTS.contains(it.name());
  }
}
