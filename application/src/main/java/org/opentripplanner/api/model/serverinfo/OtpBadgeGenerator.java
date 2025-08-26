package org.opentripplanner.api.model.serverinfo;

/**
 * This class is used to generate a SVG badge.
 */
public class OtpBadgeGenerator {

  private static final int MARGIN = 50;
  private static final int TEXT_INSET = 5;
  private static final int FONT_WIDTH = 82;
  private static final String TEMPLATE =
    """
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
   *
   * @param label The label in the
   * @param labelBgColor The background color for the label using a valid SVG color format.
   * @param body The text for the body. The background color will be in OTP blue.
   * @return A string containing the svg xml document.
   */
  public static String generateOtpBadgeSvg(String label, String labelBgColor, String body) {
    int labelLength = fontWith(label);
    int versionLength = fontWith(body);
    int totalWidth = 4 * MARGIN + labelLength + versionLength;

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
  private static int fontWith(String text) {
    int length = text.length();
    int small = text.replaceAll("[^ .il1\\()\\[\\]{}!,:;]", "").length();
    int big = length - small;
    return big * FONT_WIDTH + small * ((FONT_WIDTH * 3) / 5);
  }
}
