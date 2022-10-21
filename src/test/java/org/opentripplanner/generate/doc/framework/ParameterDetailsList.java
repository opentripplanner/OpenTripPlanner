package org.opentripplanner.generate.doc.framework;

import org.opentripplanner.framework.doc.DocFormatter;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.standalone.config.framework.json.NodeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ParameterDetailsList {

  private static final Logger LOG = LoggerFactory.getLogger(ParameterDetailsList.class);

  private final SkipFunction skipNodeOp;
  private final MarkDownDocWriter writer;

  public ParameterDetailsList(MarkDownDocWriter writer, SkipFunction skipNodeOp) {
    this.writer = writer;
    this.skipNodeOp = skipNodeOp;
  }

  public static void listParametersWithDetails(
    NodeAdapter root,
    MarkDownDocWriter out,
    SkipFunction skipNodeOp
  ) {
    new ParameterDetailsList(out, skipNodeOp).addParametersList(root);
  }

  private void addParametersList(NodeAdapter node) {
    for (NodeInfo it : node.parametersSorted()) {
      printNode(node, it);

      if (it.type().isComplex() && !skipNodeOp.skip(it)) {
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
    writer.printHeader2(it.name(), node.fullPath(it.name()));
    writer.printSection(DocFormatter.em(parameterSummaryLine(it, node.contextPath())));
    writer.printSection(it.summary());
    //noinspection ConstantConditions
    writer.printSection(it.description());
  }

  String parameterSummaryLine(NodeInfo info, String path) {
    var buf = new StringBuilder();
    var delimiter = " ∙ ";
    buf
      .append("Since version: ")
      .append(DocFormatter.code(info.since()))
      .append(delimiter)
      .append("Type: ")
      .append(DocFormatter.code(info.type().docName()))
      .append(delimiter)
      .append(DocFormatter.code(info.required() ? "Required" : "Optional"))
      .append(delimiter);

    if (info.type().isSimple() && info.defaultValue() != null) {
      buf.append("Default value: ").append(defaultValue(info)).append(delimiter);
    }
    buf.append("Path: ").append(DocFormatter.code(path == null ? "Root" : path));

    return buf.toString();
  }

  String defaultValue(NodeInfo info) {
    var defautlValue = info.defaultValue();
    return defautlValue == null ? "" : DocFormatter.code(info.type().quote(defautlValue));
  }
}
