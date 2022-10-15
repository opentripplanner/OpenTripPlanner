package org.opentripplanner.generate.doc.framework;

import static org.opentripplanner.generate.doc.framework.MarkDownDocWriter.contextIndented;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.standalone.config.framework.json.NodeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class AbstractTable {

  private static final Logger LOG = LoggerFactory.getLogger(AbstractTable.class);

  private final Predicate<NodeInfo> skipObjectOp;
  private final MarkDownDocWriter writer;

  public AbstractTable(MarkDownDocWriter writer, Predicate<NodeInfo> skipObjectOp) {
    this.writer = writer;
    this.skipObjectOp = skipObjectOp;
  }

  abstract List<String> headers();

  abstract void addRow(NodeAdapter node, List<List<?>> table, NodeInfo it);

  public MarkDownDocWriter writer() {
    return writer;
  }

  List<List<?>> createTable(NodeAdapter root) {
    List<List<?>> table = new ArrayList<>();
    table.add(headers());
    addParametersTable(root, table);
    return table;
  }

  private void addParametersTable(NodeAdapter node, List<List<?>> table) {
    for (NodeInfo it : node.parametersSorted()) {
      addRow(node, table, it);

      if (it.type().isComplex() && !skipObjectOp.test(it)) {
        var child = node.child(it.name());
        if (child != null && !child.isEmpty()) {
          addParametersTable(child, table);
        } else {
          LOG.error("Not found: '{} : {}'.", node.fullPath(it.name()), it.type().docName());
        }
      }
    }
  }

  String parameterNameIndented(NodeAdapter node, NodeInfo info) {
    String parameter = info.name();

    if (info.printDetails()) {
      parameter = writer.linkHeader(parameter, node.contextPath());
    }
    return contextIndented(node.contextPath()) + parameter;
  }

  String requiredOrOptional(NodeInfo info) {
    return writer().em(info.required() ? "Required" : "Optional");
  }

  String defaultValue(NodeInfo info) {
    return info.defaultValue() == null
      ? ""
      : writer.code(info.type().quote(writer.escapeInTable(info.defaultValue())));
  }
}
