package org.opentripplanner.generate.doc.framework;

import java.util.List;
import java.util.function.Predicate;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.standalone.config.framework.json.NodeInfo;

public class ParametersTable extends AbstractTable {

  public ParametersTable(MarkDownDocWriter writer, Predicate<NodeInfo> skipObjectOp) {
    super(writer, skipObjectOp);
  }

  public static void createParametersTable(
    NodeAdapter root,
    MarkDownDocWriter out,
    Predicate<NodeInfo> skipObjectOp
  ) {
    var table = new ParametersTable(out, skipObjectOp).createTable(root);
    out.printTable(table);
  }

  @Override
  List<String> headers() {
    return List.of("Config Parameter", "Type", "Req./Opt.", "Default Value", "Since", "Summary");
  }

  @Override
  void addRow(NodeAdapter node, List<List<?>> table, NodeInfo info) {
    if (info.isDeprecated()) {
      return;
    }
    table.add(
      List.of(
        parameterNameIndented(node, info),
        writer().code(info.type().docName()),
        requiredOrOptional(info),
        defaultValue(info),
        info.since(),
        info.summary()
      )
    );
  }
}
