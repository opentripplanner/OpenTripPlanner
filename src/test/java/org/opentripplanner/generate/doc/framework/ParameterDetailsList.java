package org.opentripplanner.generate.doc.framework;

import static org.opentripplanner.framework.text.MarkdownFormatter.NEW_LINE;
import static org.opentripplanner.framework.text.MarkdownFormatter.header;

import org.opentripplanner.framework.text.MarkdownFormatter;
import org.opentripplanner.standalone.config.framework.json.ConfigType;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.standalone.config.framework.json.NodeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ParameterDetailsList {

  private static final Logger LOG = LoggerFactory.getLogger(ParameterDetailsList.class);

  private final StringBuilder buffer = new StringBuilder();
  private final SkipFunction skipNodeOp;
  private final int headerLevel;

  private ParameterDetailsList(SkipFunction skipNodeOp, int headerLevel) {
    this.skipNodeOp = skipNodeOp;
    this.headerLevel = headerLevel;
  }

  public static String listParametersWithDetails(
    NodeAdapter root,
    SkipFunction skipNodeOp,
    int headerLevel
  ) {
    var details = new ParameterDetailsList(skipNodeOp, headerLevel);
    details.addParametersList(root);
    return details.buffer.toString();
  }

  private void addParametersList(NodeAdapter node) {
    for (NodeInfo it : node.parametersSorted()) {
      printNode(node, it);

      if (it.type().isComplex() && !skipNodeOp.skip(it)) {
        var child = node.child(it.name());

        if (child == null || child.isEmpty()) {
          LOG.error("Not found: {} : {}", node.fullPath(it.name()), it.type().docName());
        } else if (it.type() == ConfigType.ARRAY) {
          for (String childName : child.listChildrenByName()) {
            addParametersList(child.child(childName));
          }
        } else {
          addParametersList(child);
        }
      }
    }
  }

  private void printNode(NodeAdapter node, NodeInfo it) {
    // Skip node if there is no more info to print
    if (!it.printDetails()) {
      return;
    }
    addSection(header(headerLevel, it.name(), node.fullPath(it.name())))
      .addSection(MarkdownFormatter.em(parameterSummaryLine(it, node.contextPath())))
      .addSection(it.summary())
      .addSection(it.description());
  }

  String parameterSummaryLine(NodeInfo info, String path) {
    var buf = new StringBuilder();
    var delimiter = " ∙ ";
    buf
      .append("Since version: ")
      .append(MarkdownFormatter.code(info.since()))
      .append(delimiter)
      .append("Type: ")
      .append(MarkdownFormatter.code(info.typeDescription()))
      .append(delimiter)
      .append(MarkdownFormatter.code(info.required() ? "Required" : "Optional"))
      .append(delimiter);

    if (info.type().isSimple() && info.defaultValue() != null) {
      buf.append("Default value: ").append(defaultValue(info)).append(delimiter);
    }
    buf.append("Path: ").append(MarkdownFormatter.code(path == null ? "Root" : path));

    return buf.toString();
  }

  String defaultValue(NodeInfo info) {
    var defaultValue = info.defaultValue();
    return defaultValue == null ? "" : MarkdownFormatter.code(info.type().quote(defaultValue));
  }

  private ParameterDetailsList addSection(String text) {
    if (text == null || text.isBlank()) {
      return this;
    }
    buffer.append(text).append(NEW_LINE).append(NEW_LINE);
    return this;
  }
}
