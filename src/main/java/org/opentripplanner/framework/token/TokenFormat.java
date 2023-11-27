package org.opentripplanner.framework.token;

import org.opentripplanner.framework.text.CharacterEscapeFormatter;

interface TokenFormat {
  char FIELD_SEPARATOR = '|';
  char TOKEN_ESCAPE = '\\';
  char TOKEN_SUBSTITUTION = '+';

  static CharacterEscapeFormatter tokenFormatter() {
    return new CharacterEscapeFormatter(TOKEN_ESCAPE, FIELD_SEPARATOR, TOKEN_SUBSTITUTION);
  }
}
