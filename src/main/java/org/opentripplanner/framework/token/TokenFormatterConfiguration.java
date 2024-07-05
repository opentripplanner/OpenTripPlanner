package org.opentripplanner.framework.token;

import org.opentripplanner.framework.text.CharacterEscapeFormatter;

class TokenFormatterConfiguration {

  private static final char TOKEN_ESCAPE = '\\';
  private static final char TOKEN_SUBSTITUTION = '+';
  private static final char FIELD_SEPARATOR = '|';

  /** Prevent instantiation - this is a utility class. */
  private TokenFormatterConfiguration() {}

  /**
   * We use the pipe '|' for field separations. The IDs included in the token frequently use
   * ':' so the visual difference is better than the alternatives like ',' ';' and TAB.
   */
  static char fieldSeparator() {
    return FIELD_SEPARATOR;
  }

  static CharacterEscapeFormatter tokenFormatter() {
    return new CharacterEscapeFormatter(TOKEN_ESCAPE, FIELD_SEPARATOR, TOKEN_SUBSTITUTION);
  }
}
