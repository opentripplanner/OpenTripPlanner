package org.opentripplanner.standalone.config.framework.doc;

import java.util.List;
import java.util.function.Predicate;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.standalone.config.framework.json.NodeInfo;

public class DeprecatedParametersTable extends AbstractTable {

  public DeprecatedParametersTable(MarkDownDocWriter writer, Predicate<NodeInfo> skipObjectOp) {
    super(writer, skipObjectOp);
  }

  public static void createDeprecatedParametersTable(
    NodeAdapter root,
    MarkDownDocWriter writer,
    Predicate<NodeInfo> skipObjectOp
  ) {
    var table = new DeprecatedParametersTable(writer, skipObjectOp).createTable(root);
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
        writer().code(it.type().docName()),
        it.summary()
      )
    );
  }
}
