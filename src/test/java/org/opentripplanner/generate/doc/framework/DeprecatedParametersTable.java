package org.opentripplanner.generate.doc.framework;

import java.util.List;
import org.opentripplanner.framework.text.MarkdownFormatter;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.standalone.config.framework.json.NodeInfo;

public class DeprecatedParametersTable extends AbstractTable {

  public DeprecatedParametersTable(SkipFunction skipFunction) {
    super(skipFunction);
  }

  public static void createDeprecatedParametersTable(
    NodeAdapter root,
    MarkDownDocWriter writer,
    SkipFunction skipFunction
  ) {
    var table = new DeprecatedParametersTable(skipFunction).createTable(root);
    writer.printTable(table);
  }

  @Override
  List<String> headers() {
    return List.of("Parameter", "Since", "Deprecation Summary", "Type", "Original Summary");
  }

  @Override
  @SuppressWarnings("ConstantConditions")
  void addRow(NodeAdapter node, List<List<?>> table, NodeInfo it) {
    if (!it.isDeprecated()) {
      return;
    }
    table.add(
      List.of(
        parameterNameIndented(node, it),
        it.deprecated().since() + "(" + it.since() + ")",
        it.deprecated().description(),
        MarkdownFormatter.code(it.type().docName()),
        it.summary()
      )
    );
  }
}
