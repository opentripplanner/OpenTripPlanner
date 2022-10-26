package org.opentripplanner.generate.doc.framework;

import static org.opentripplanner.framework.text.MarkdownFormatter.code;
import static org.opentripplanner.framework.text.MarkdownFormatter.escapeInTable;
import static org.opentripplanner.generate.doc.framework.MarkDownDocWriter.contextIndented;

import java.util.List;
import java.util.stream.Stream;
import org.opentripplanner.framework.text.MarkdownFormatter;
import org.opentripplanner.framework.text.Table;
import org.opentripplanner.framework.text.TableBuilder;
import org.opentripplanner.standalone.config.framework.json.ConfigType;
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

  abstract List<Table.Align> alignment();

  abstract void addRow(NodeAdapter node, TableBuilder table, NodeInfo it);

  public Table createTable(NodeAdapter root) {
    var table = Table.of().withHeaders(headers()).withAlights(alignment());
    addParametersToTable(root, table);
    return table.build();
  }

  private void addParametersToTable(NodeAdapter node, TableBuilder table) {
    for (NodeInfo it : node.parametersSorted()) {
      addRow(node, table, it);

      if (it.type().isComplex() && !skip(it)) {
        var child = node.child(it.name());

        if (child == null || child.isEmpty()) {
          LOG.error("Not found: {} : {}", node.fullPath(it.name()), it.type().docName());
        } else if (it.type() == ConfigType.ARRAY) {
          addArrayChildrenToTable(it, child, table);
        } else {
          addParametersToTable(child, table);
        }
      }
    }
  }

  private void addArrayChildrenToTable(NodeInfo info, NodeAdapter node, TableBuilder table) {
    for (String childName : node.listChildrenByName()) {
      NodeAdapter child = node.child(childName);
      addRow(node, table, info.arraysChild());
      addParametersToTable(child, table);
    }
  }

  String parameterNameIndented(NodeAdapter node, NodeInfo info) {
    String parameter = info.name();

    // This is a hack, the "type" should be build in as a separate type qualifier,
    // not matched by the magic name "type"
    if ("type".equalsIgnoreCase(parameter) && info.type() == ConfigType.ENUM) {
      var upperCaseValue = node.peek(parameter).asText().toUpperCase().replace('-', '_');
      @SuppressWarnings("ConstantConditions")
      Enum<?> qualifier = Stream
        .of(info.enumType().getEnumConstants())
        .filter(it -> it.name().toUpperCase().equals(upperCaseValue))
        .findFirst()
        .orElseThrow();
      parameter += " = " + info.type().quote(qualifier.name());
    }
    if (skipFunction.skip(info)) {
      parameter =
        skipFunction
          .linkToDoc(info)
          .map(link -> MarkdownFormatter.linkToDoc(info.name(), link))
          .orElse(info.name());
    } else if (info.printDetails()) {
      parameter = MarkdownFormatter.linkToAnchor(parameter, node.fullPath(parameter));
    }
    return contextIndented(node.contextPath()) + parameter;
  }

  String requiredOrOptional(NodeInfo info) {
    String text = info.required() ? "Required" : "Optional";
    return MarkdownFormatter.em(text);
  }

  String defaultValue(NodeInfo info) {
    if (info.defaultValue() == null) {
      return "";
    }
    var defaultValue = info.type().quote(info.defaultValue());
    return escapeInTable(code(defaultValue));
  }

  private boolean skip(NodeInfo info) {
    return skipFunction.skip(info);
  }
}
