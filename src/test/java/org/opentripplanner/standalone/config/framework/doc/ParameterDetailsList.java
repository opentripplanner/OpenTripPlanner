package org.opentripplanner.standalone.config.framework.doc;

import java.util.function.Predicate;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.standalone.config.framework.json.NodeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ParameterDetailsList {

  private static final Logger LOG = LoggerFactory.getLogger(ParameterDetailsList.class);

  private final Predicate<NodeInfo> skipObjectOp;
  private final MarkDownDocWriter writer;

  public ParameterDetailsList(MarkDownDocWriter writer, Predicate<NodeInfo> skipObjectOp) {
    this.writer = writer;
    this.skipObjectOp = skipObjectOp;
  }

  public static void listParametersWithDetails(
    NodeAdapter root,
    MarkDownDocWriter out,
    Predicate<NodeInfo> skipObjectOp
  ) {
    new ParameterDetailsList(out, skipObjectOp).addParametersList(root);
  }

  private void addParametersList(NodeAdapter node) {
    for (NodeInfo it : node.parametersSorted()) {
      printNode(node, it);

      if (it.type().isComplex() && !skipObjectOp.test(it)) {
        var child = node.child(it.name());
        if (child != null && !child.isEmpty()) {
          addParametersList(child);
        } else {
          LOG.error("Not found: '{} : {}'.", node.fullPath(it.name()), it.type());
        }
      }
    }
  }

  private void printNode(NodeAdapter node, NodeInfo it) {
    // Skip node if there is no more info to print
    if (!it.printDetails()) {
      return;
    }
    writer.printHeader3(node.fullPath(it.name()));
    writer.printSection(writer.em(parameterSummaryLine(it)));
    writer.printSection(it.summary());
    //noinspection ConstantConditions
    writer.printSection(it.description());
  }

  String parameterSummaryLine(NodeInfo info) {
    var buf = new StringBuilder();
    var delimiter = " ∙ ";
    buf
      .append("Since version ")
      .append(info.since())
      .append(delimiter)
      .append("Type: ")
      .append(writer.code(info.type().docName()))
      .append(delimiter)
      .append(info.required() ? "Required" : "Optional")
      .append(' ');

    if (info.defaultValue() != null) {
      buf.append(delimiter).append("Default vaule: ").append(defaultValue(info));
    }
    return buf.toString();
  }

  String defaultValue(NodeInfo info) {
    return writer.code(info.type().quote(info.defaultValue()));
  }
}
