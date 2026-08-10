package org.opentripplanner.apis.vectortiles;

/**
 * Debug layer colors for drawing shapes on top of the map.
 */
public enum Color {
  MAGENTA("#f21d52"),
  LIGHT_MAGENTA("#f783a0"),
  BRIGHT_GREEN("#22DD9E"),
  DARK_GREEN("#136b04"),
  TEAL("#277eb5"),
  TURQUOISE("#1cafad"),
  RED("#fc0f2a"),
  PURPLE("#BC55F2"),
  BLACK("#140d0e"),
  LIGHT_RED("#ff6b6b"),
  DARK_RED("#cc0000"),
  ORANGE("#ffa500"),
  DARK_ORANGE("#ff8c00"),
  LIGHT_BLUE("#4a9eff"),
  DARK_BLUE("#0066cc");

  private final String hex;

  Color(String hex) {
    this.hex = hex;
  }

  public String hex() {
    return hex;
  }
}
