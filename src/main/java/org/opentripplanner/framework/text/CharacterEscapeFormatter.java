package org.opentripplanner.framework.text;

/**
 * This class is used to escape characters in a string, removing a special character from
 * the string. For example, if you want to make sure a string does not contain {@code ';'},
 * the {@code ';'} can be replaced with {@code '\+'}. The slash({@code '\'}) is used as an
 * escape character, so we need to escape all {@code '\'} as well. Now, the escaped string
 * does not contain the special character anymore. The original string can be computed by
 * reversing the process.
 * <p>
 * A "special-character" is removed from a text using an escape character and
 * a substitution character. For example, if:
 * <ul>
 *   <li>the escape char is '\'</li>
 *   <li>the special char is ';'</li>
 *   <li>and the substitution char is '+'</li>
 * </ul>
 *
 * then replace:
 * <ul>
 *   <li>'\' with '\\' and</li>
 *   <li>';' with '\;'</li>
 * </ul>
 * To get back the original text, the reverse process using {@link #decode(String)}.
 * <pre>
 *
 * Original: "\tThis;is;an;example\+"
 * Encoded:  "\\tThis\+is\+an\+example\\+"
 * Decoded:  "\tThis;is;an;example\+"
 * </pre>
 */
public class CharacterEscapeFormatter {

  private final char escapeChar;
  private final char specialChar;
  private final char substitutionChar;

  /**
   * @param escapeChar the character used as an escape character.
   * @param specialChar the character to be removed/replaced in the encoded text.
   * @param substitutionChar the character used together with the escape character to put in the
   *                         encoded text as a placeholder for the special character.
   */
  public CharacterEscapeFormatter(char escapeChar, char specialChar, char substitutionChar) {
    this.escapeChar = escapeChar;
    this.specialChar = specialChar;
    this.substitutionChar = substitutionChar;
  }

  /**
   * Encode the given text and replace the {@code specialChar} with a placeholder. The original
   * text can be retrieved by using {@link #decode(String)}.
   * @param text the text to encode.
   * @return the encoded text without the {@code specialChar}.
   */
  public String encode(String text) {
    final var buf = new StringBuilder();
    for (int i = 0; i < text.length(); ++i) {
      char ch = text.charAt(i);
      if (ch == escapeChar) {
        buf.append(escapeChar).append(escapeChar);
      } else if (ch == specialChar) {
        buf.append(escapeChar).append(substitutionChar);
      } else {
        buf.append(ch);
      }
    }
    return buf.toString();
  }

  /**
   * Return the original text by decoding the encoded text.
   * @see #encode(String)
   */
  public String decode(String encodedText) {
    if (encodedText.length() < 2) {
      return encodedText;
    }
    final var buf = new StringBuilder();
    boolean prevEsc = false;
    for (int i = 0; i < encodedText.length(); ++i) {
      char ch = encodedText.charAt(i);
      if (prevEsc) {
        if (ch == escapeChar) {
          buf.append(escapeChar);
        } else if (ch == substitutionChar) {
          buf.append(specialChar);
        } else {
          throw new IllegalStateException(
            "Unexpected combination of escape-char '%c' and '%c' character at position %d. Text: '%s'.".formatted(
                escapeChar,
                ch,
                i,
                encodedText
              )
          );
        }
        prevEsc = false;
      } else if (ch == escapeChar) {
        prevEsc = true;
      } else {
        buf.append(ch);
      }
    }
    return buf.toString();
  }
}
