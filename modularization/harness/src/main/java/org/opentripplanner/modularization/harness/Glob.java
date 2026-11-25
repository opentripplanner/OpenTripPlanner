package org.opentripplanner.modularization.harness;

import java.util.regex.Pattern;

/** A tiny glob where {@code *} matches any sequence of characters, including dots and brackets. */
final class Glob {

  private final String glob;
  private final Pattern pattern;

  Glob(String glob) {
    this.glob = glob;
    var regex = new StringBuilder();
    for (String part : glob.split("\\*", -1)) {
      if (regex.length() > 0) {
        regex.append(".*");
      }
      regex.append(Pattern.quote(part));
    }
    // Pattern.quote("") yields "\Q\E", which is fine.
    this.pattern = Pattern.compile(regex.toString(), Pattern.DOTALL);
  }

  boolean matches(String value) {
    return pattern.matcher(value).matches();
  }

  @Override
  public String toString() {
    return glob;
  }
}
