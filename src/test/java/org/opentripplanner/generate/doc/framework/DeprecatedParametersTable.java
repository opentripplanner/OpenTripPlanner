package org.opentripplanner.generate.doc.framework;

import static org.opentripplanner.framework.text.Table.Align.Center;
import static org.opentripplanner.framework.text.Table.Align.Left;

import java.util.List;
import org.opentripplanner.framework.text.MarkdownFormatter;
import org.opentripplanner.framework.text.Table;
import org.opentripplanner.framework.text.TableBuilder;
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
  List<Table.Align> alignment() {
    return List.of(Left, Center, Left, Center, Left);
  }

  @Override
  @SuppressWarnings("ConstantConditions")
  void addRow(NodeAdapter node, TableBuilder table, NodeInfo it) {
    if (!it.isDeprecated()) {
      return;
    }
    table.addRow(
      parameterNameIndented(node, it),
      it.deprecated().since() + "(" + it.since() + ")",
      it.deprecated().description(),
      MarkdownFormatter.code(it.type().docName()),
      it.summary()
    );
  }
}
