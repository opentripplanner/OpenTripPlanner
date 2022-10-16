package org.opentripplanner.generate.doc.framework;

import static org.opentripplanner.generate.doc.framework.MarkDownDocWriter.contextIndented;

import java.util.ArrayList;
import java.util.List;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.standalone.config.framework.json.NodeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class AbstractTable {

  private static final Logger LOG = LoggerFactory.getLogger(AbstractTable.class);

  private final SkipFunction skipFunction;

  public AbstractTable(SkipFunction skipFunction) {
    this.skipFunction = skipFunction;
  }

  abstract List<String> headers();

  abstract void addRow(NodeAdapter node, List<List<?>> table, NodeInfo it);

  List<List<?>> createTable(NodeAdapter root) {
    List<List<?>> table = new ArrayList<>();
    table.add(headers());
    addParametersTable(root, table);
    return table;
  }

  private void addParametersTable(NodeAdapter node, List<List<?>> table) {
    for (NodeInfo it : node.parametersSorted()) {
      addRow(node, table, it);

      if (it.type().isComplex() && !skip(it)) {
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
    if (skipFunction.skip(info)) {
      parameter =
        skipFunction
          .linkToDoc(info)
          .map(link -> DocFormatter.linkToDoc(info.name(), link))
          .orElse(info.name());
    } else if (info.printDetails()) {
      parameter = DocFormatter.linkToAnchor(parameter, node.fullPath(parameter));
    }
    return contextIndented(node.contextPath()) + parameter;
  }

  String requiredOrOptional(NodeInfo info) {
    String text = info.required() ? "Required" : "Optional";
    return DocFormatter.em(text);
  }

  String defaultValue(NodeInfo info) {
    return info.defaultValue() == null
      ? ""
      : DocFormatter.code(info.type().quote(DocFormatter.escapeInTable(info.defaultValue())));
  }

  private boolean skip(NodeInfo info) {
    return skipFunction.skip(info);
  }
}
