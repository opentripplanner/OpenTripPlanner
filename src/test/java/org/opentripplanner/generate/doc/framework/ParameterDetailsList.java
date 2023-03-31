package org.opentripplanner.generate.doc.framework;

import static org.opentripplanner.generate.doc.framework.NodeAdapterHelper.anchor;
import static org.opentripplanner.standalone.config.framework.json.ConfigType.ENUM_MAP;
import static org.opentripplanner.standalone.config.framework.json.ConfigType.ENUM_SET;

import java.util.EnumSet;
import org.opentripplanner.framework.text.MarkdownFormatter;
import org.opentripplanner.standalone.config.framework.json.ConfigType;
import org.opentripplanner.standalone.config.framework.json.EnumMapper;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.standalone.config.framework.json.NodeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ParameterDetailsList {

  private static final Logger LOG = LoggerFactory.getLogger(ParameterDetailsList.class);
  public static final char SPACE = ' ';

  private final DocBuilder doc = new DocBuilder();
  private final SkipNodes skipNodes;
  private final int headerLevel;

  private ParameterDetailsList(SkipNodes skipNodes, int headerLevel) {
    this.skipNodes = skipNodes;
    this.headerLevel = headerLevel;
  }

  public static String listParametersWithDetails(
    NodeAdapter root,
    SkipNodes skipNodes,
    int headerLevel
  ) {
    var details = new ParameterDetailsList(skipNodes, headerLevel);
    details.addParametersList(root);
    return details.doc.toDoc();
  }

  private void addParametersList(NodeAdapter node) {
    for (NodeInfo it : node.parametersSorted()) {
      if (skipNodes.skipDetails(it)) {
        continue;
      }
      printNode(node, it);

      if (skipNodes.skipDetailsForNestedElements(it)) {
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
        for (String childName : child.listChildrenByName()) {
          addParametersList(child.child(childName));
        }
      } else {
        addParametersList(child);
      }
    }
  }

  private void printNode(NodeAdapter node, NodeInfo info) {
    // Skip node if there is no more info to print
    if (!info.printDetails()) {
      return;
    }
    var paramName = info.name();
    doc.header(headerLevel, paramName, anchor(node, paramName));
    addMetaInfo(info, node.contextPath());
    doc.addSection(info.summary());
    doc.addSection(info.description());
  }

  private String enumValueToString(Enum<?> en) {
    return EnumMapper.toString(en);
  }

  void addMetaInfo(NodeInfo info, String path) {
    doc.label("Since version:").code(info.since()).dotSeparator();
    doc.label("Type:").code(info.typeDescription()).dotSeparator();
    doc.label("Cardinality:").code(info.required() ? "Required" : "Optional");

    if (info.type().isSimple() && info.defaultValue() != null) {
      doc.dotSeparator().label("Default value:").code(info.type().quote(info.defaultValue()));
    }

    doc.lineBreak().label("Path:").path(path == null ? "/" : "/" + path.replace('.', '/'));

    skipNodes
      .linkDetails(info)
      .ifPresent(link ->
        doc.dotSeparator().label("See:").text(MarkdownFormatter.linkToDoc(link, link))
      );

    // Document enums
    if (EnumSet.of(ConfigType.ENUM, ConfigType.ENUM_SET).contains(info.type())) {
      doc.lineBreak().label("Enum values:").addEnums(info.enumTypeValues());
    } else if (info.type() == ConfigType.ENUM_MAP) {
      doc.lineBreak().label("Enum keys:").addEnums(info.enumTypeValues());
    }
    doc.endParagraph();
  }
}
