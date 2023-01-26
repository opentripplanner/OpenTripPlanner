package org.opentripplanner.graph_builder.issue.report;

import java.awt.Color;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** THIS CLASS IS THREAD-SAFE */
class IssueColors {

  /**
   * We use a concurrent hash map for thread safety. It is not necessary since the report writer is
   * not writing files in parallel, but it make this class thread-safe in case we want to speed up
   * the issue report generation.
   */
  private static final Map<String, Color> ASSIGNED_COLOR = new ConcurrentHashMap<>();

  /**
   * List of back-ground-colors, should work with black text on top. The order should give dissent
   * contract for two neighboring pairs.
   */
  private static final Color[] BG_COLORS = {
    new Color(0xFFFF80),
    new Color(0xFFD0FF),
    new Color(0x90C8FF),
    new Color(0xFFE060),
    new Color(0xB0FF40),
    new Color(0xFF80FF),
    new Color(0x70A0FF),
    new Color(0xFFC000),
    new Color(0x80FF40),
    new Color(0xFFB0FF),
    new Color(0x90E0FF),
    new Color(0xFFA000),
    new Color(0xE0FF70),
    new Color(0xD0A0FF),
    new Color(0xB0FFFF),
    new Color(0xFF8080),
    new Color(0x40FF40),
    new Color(0xA090FF),
    new Color(0x90FFE0),
    new Color(0xFF60B0),
    new Color(0x70FFB0),
    new Color(0xFFFF40),
  };

  /** Get and return color a in hex format: {@code "#FF00FF"} */
  static String rgb(String issueType) {
    // The '& 0xFFFFFF' is needed to remove the alpha value
    return String.format("#%06X", backgroundColor(issueType).getRGB() & 0xFFFFFF);
  }

  private static Color backgroundColor(String issueType) {
    return ASSIGNED_COLOR.computeIfAbsent(issueType, key -> nextColor());
  }

  private static Color nextColor() {
    // Use modulo to start over if the number of issues is larger then the list of colors
    return BG_COLORS[ASSIGNED_COLOR.size() % BG_COLORS.length];
  }
}
