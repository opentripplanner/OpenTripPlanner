package org.opentripplanner.generate.doc.framework;

import java.util.List;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

public class NodeAdapterHelper {

  private static final List<AnchorAbbreviation> ANCHOR_ABBREVIATIONS = List.of(
    new AnchorAbbreviation("rd.", "routingDefaults."),
    new AnchorAbbreviation("to.", "transferOptimization."),
    new AnchorAbbreviation("if.", "itineraryFilters."),
    new AnchorAbbreviation("nd.", "netexDefaults."),
    new AnchorAbbreviation("gd.", "gtfsDefaults."),
    new AnchorAbbreviation("tf.", "transitFeeds."),
    new AnchorAbbreviation("od.", "osmDefaults."),
    new AnchorAbbreviation("lfp.", "localFileNamePatterns."),
    new AnchorAbbreviation("u.", "updaters."),
    new AnchorAbbreviation("tpfm.", "transferParametersForMode."),
    new AnchorAbbreviation("0.", "[0]."),
    new AnchorAbbreviation("1.", "[1].")
  );
  public static final char NEW_LINE = '\n';

  /** Private to prevent instantiation. */
  private NodeAdapterHelper() {
    /* empty */
  }

  /**
   * Generate a anchor for the given node and child parameter.
   */
  public static String anchor(NodeAdapter node, String parameterName) {
    String anchor = node.fullPath(parameterName);
    for (AnchorAbbreviation it : ANCHOR_ABBREVIATIONS) {
      anchor = anchor.replace(it.name, it.abbreviation);
    }
    return anchor;
  }

  private record AnchorAbbreviation(String abbreviation, String name) {}
}
