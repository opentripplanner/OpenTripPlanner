package org.opentripplanner.generate.doc.framework;

import static org.opentripplanner.framework.text.MarkdownFormatter.code;
import static org.opentripplanner.framework.text.MarkdownFormatter.escapeInTable;
import static org.opentripplanner.generate.doc.framework.NodeAdapterHelper.anchor;
import static org.opentripplanner.standalone.config.framework.json.ConfigType.ENUM_MAP;
import static org.opentripplanner.standalone.config.framework.json.ConfigType.ENUM_SET;

import java.util.List;
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

  private final SkipNodes skipNodes;
  private int rootLevel = 0;

  public AbstractTable(SkipNodes skipNodes) {
    this.skipNodes = skipNodes;
  }

  abstract List<String> headers();

  abstract List<Table.Align> alignment();

  abstract void addRow(NodeAdapter node, TableBuilder table, NodeInfo it);

  public Table createTable(NodeAdapter root) {
    this.rootLevel = root.level();
    var table = Table.of().withHeaders(headers()).withAlights(alignment());
    addParametersToTable(root, table);

    return table.build();
  }

  private void addParametersToTable(NodeAdapter node, TableBuilder table) {
    for (NodeInfo it : node.parametersSorted()) {
      if (it.skipChild()) {
        continue;
      }

      addRow(node, table, it);

      if (skipNodes.skipDetails(it) || skipNodes.skipDetailsForNestedElements(it)) {
        continue;
      }
      if (!it.type().isComplex()) {
        continue;
      }
      if (it.type() == ENUM_SET) {
        continue;
      }
      //noinspection ConstantConditions
      if (it.type() == ENUM_MAP && it.elementType().isSimple()) {
        continue;
      }

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

  private void addArrayChildrenToTable(NodeInfo info, NodeAdapter node, TableBuilder table) {
    boolean skipNestedObjectRow = node.listChildrenByName().size() == 1;

    for (String childName : node.listChildrenByName()) {
      NodeAdapter child = node.child(childName);
      if (!skipNestedObjectRow) {
        addRow(node, table, info.arraysChild());
      }
      addParametersToTable(child, table);
    }
  }

  String parameterNameIndented(NodeAdapter node, NodeInfo info) {
    String parameter = info.name();

    if (info.isTypeQualifier()) {
      parameter += " = " + info.toMarkdownString(node.typeQualifier());
    }
    var link = skipNodes.linkOverview(info);
    if (link.isPresent()) {
      parameter = MarkdownFormatter.linkToDoc(info.name(), link.get());
    } else if (info.printDetails()) {
      parameter = MarkdownFormatter.linkToAnchor(parameter, anchor(node, parameter));
    }
    int indentLevel = node.level() - rootLevel;
    return MarkdownFormatter.indentInTable(indentLevel) + parameter;
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
}
