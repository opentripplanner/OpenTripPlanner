package org.opentripplanner.generate.doc.framework;

import java.util.List;
import org.opentripplanner.framework.text.MarkdownFormatter;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.standalone.config.framework.json.NodeInfo;

public class ParameterSummaryTable extends AbstractTable {

  public ParameterSummaryTable(SkipFunction skipFunction) {
    super(skipFunction);
  }

  public static void createParametersTable(
    NodeAdapter root,
    MarkDownDocWriter out,
    SkipFunction skipFunction
  ) {
    var table = new ParameterSummaryTable(skipFunction).createTable(root);
    out.printTable(table);
  }

  @Override
  List<String> headers() {
    return List.of("Config Parameter", "Type", "Summary", "Req./Opt.", "Default Value", "Since");
  }

  @Override
  void addRow(NodeAdapter node, List<List<?>> table, NodeInfo info) {
    if (info.isDeprecated()) {
      return;
    }
    table.add(
      List.of(
        parameterNameIndented(node, info),
        MarkdownFormatter.code(info.type().docName()),
        info.summary(),
        requiredOrOptional(info),
        info.type().isSimple() ? defaultValue(info) : "",
        info.since()
      )
    );
  }
}
