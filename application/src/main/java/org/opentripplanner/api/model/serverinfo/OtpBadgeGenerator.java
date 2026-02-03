package org.opentripplanner.api.model.serverinfo;

import com.google.common.html.HtmlEscapers;
import java.util.regex.Pattern;

/**
 * This class is used to generate a SVG badge.
 */
public class OtpBadgeGenerator {

  private static final String NUM = " ?\\d+ ?";
  private static final String DEC = " ?\\d+(\\.\\d+)? ?";

  /**
   * Accept formats:
   * - HEX: #112233
   * - RGB: rgb(0,255,0)
   * - RGBA: rgba(0,255,0,0.5)
   * - Named colors (\w{3, 20}) 'tan' to 'lightgoldenrodyellow'
   */
  private static final Pattern COLOR_PATTERN = Pattern.compile(
    "(#[\\dA-Fa-f]{6}|rgba?\\(N,N,N(,D)?\\)|[\\w]{3,30})".replace("N", NUM).replace("D", DEC)
  );

  private static final String WHITE_SPACE_EX_SPACE = "\\t\\n\\x0B\\f\\r\\x85\\u2028\\u2029";
  private static final String CONTROL_CHARS = "\\p{Cntrl}";
  private static final Pattern LABEL_PATTERN = Pattern.compile(
    "[^" + WHITE_SPACE_EX_SPACE + CONTROL_CHARS + "]*"
  );

  private static final int MARGIN = 50;
  private static final int TEXT_INSET = 5;
  private static final int FONT_WIDTH = 82;
  private static final String TEMPLATE = """
    <svg width="{{svgWidth}}" height="20" viewBox="0 0 {{width}} 200"
        xmlns="http://www.w3.org/2000/svg" role="img" aria-label="{{label}}: {{body}}">
      <title>{{label}}: {{body}}</title>
      <linearGradient id="grad1" x2="0" y2="100%">
        <stop offset="0" stop-opacity=".1" stop-color="#EEE"/>
        <stop offset="1" stop-opacity=".1"/>
      </linearGradient>
      <mask id="mask1"><rect width="{{width}}" height="200" rx="30" fill="#FFF"/></mask>
      <g mask="url(#mask1)">
        <rect width="{{labelWidth}}" height="200" fill="{{labelBgColor}}"/>
        <rect width="{{versionWidth}}" height="200" fill="#2179BF" x="{{labelWidth}}"/>
        <rect width="{{width}}" height="200" fill="url(#grad1)"/>
      </g>
      <g aria-hidden="true" fill="#fff" text-anchor="start" font-family="Verdana,DejaVu Sans,sans-serif" font-size="110">
        <text x="60" y="148" textLength="{{labelLength}}" fill="#000" opacity="0.25">{{label}}</text>
        <text x="50" y="138" textLength="{{labelLength}}">{{label}}</text>
        <text x="{{versionOffsetA}}" y="148" textLength="{{versionLength}}" fill="#000" opacity="0.25">{{body}}</text>
        <text x="{{versionOffsetB}}" y="138" textLength="{{versionLength}}">{{body}}</text>
      </g>
    </svg>
    """;

  /**
   * A color is valid if it is a:
   * <ol>
   *   <li>Named color: tan, lightgoldenrodyellow</li>
   *   <li>Hex color : #00FF80</li>
   *   <li>RGB color : rgb(255, 0, 255)</li>
   *   <li>RGBA color : rgba(255, 0, 255, 0.5)</li>
   * </ol>
   */
  public static boolean isValidColor(String color) {
    return COLOR_PATTERN.matcher(color).matches();
  }

  /**
   * Has length less than 120 characters and does not contain control characters and whitespace.
   * Space is allowed. This does not prevent Reflected XSS, but in combination with escaping the
   * label it should.
   */
  public static boolean isValidLabel(String label) {
    return (label.length() <= 120 && LABEL_PATTERN.matcher(label).matches());
  }

  /**
   *
   * @param label The label in the
   * @param labelBgColor The background color for the label using a valid SVG color format.
   * @param body The text for the body. The background color will be in OTP blue.
   * @return A string containing the svg xml document.
   */
  public static String generateOtpBadgeSvg(String label, String labelBgColor, String body) {
    int labelLength = fontWidth(label);
    int versionLength = fontWidth(body);
    int totalWidth = 4 * MARGIN + labelLength + versionLength;

    // Prevent Reflected XSS attacks by escaping
    label = HtmlEscapers.htmlEscaper().escape(label);

    return TEMPLATE.replace("{{body}}", body)
      .replace("{{labelBgColor}}", labelBgColor)
      .replace("{{label}}", label)
      .replace("{{svgWidth}}", Double.toString(totalWidth / 10.0))
      .replace("{{width}}", Integer.toString(totalWidth))
      .replace("{{labelWidth}}", Integer.toString(labelLength + 2 * MARGIN))
      .replace("{{versionWidth}}", Integer.toString(versionLength + 2 * MARGIN))
      .replace("{{labelLength}}", Integer.toString(labelLength))
      .replace("{{versionLength}}", Integer.toString(versionLength))
      .replace("{{versionOffsetA}}", Integer.toString(labelLength + 3 * MARGIN + TEXT_INSET))
      .replace("{{versionOffsetB}}", Integer.toString(labelLength + 3 * MARGIN - TEXT_INSET));
  }

  /**
   * This method estimates the width needed for the given {@code text}. It is not accurate,
   * but the text will be stretched/compressed to match the width.
   */
  private static int fontWidth(String text) {
    int length = text.length();
    int small = text.replaceAll("[^ .il1\\()\\[\\]{}!,:;]", "").length();
    int big = length - small;
    return big * FONT_WIDTH + small * ((FONT_WIDTH * 3) / 5);
  }
}
