package org.opentripplanner.api.model.serverinfo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.api.model.serverinfo.OtpBadgeGenerator.generateOtpBadgeSvg;
import static org.opentripplanner.api.model.serverinfo.OtpBadgeGenerator.isValidColor;
import static org.opentripplanner.api.model.serverinfo.OtpBadgeGenerator.isValidLabel;

import org.junit.jupiter.api.Test;

public class OtpBadgeGeneratorTest {

  @Test
  void getVersionBadge() {
    String result = generateOtpBadgeSvg(
      "🦋 OTP <version>/<ser.ver.id>",
      "firebrick",
      "2.8.0-SNAPSHOT-237"
    );

    assertEquals(
      """
      <svg width="379.0" height="20" viewBox="0 0 3790 200"
          xmlns="http://www.w3.org/2000/svg" role="img" aria-label="🦋 OTP &lt;version&gt;/&lt;ser.ver.id&gt;: 2.8.0-SNAPSHOT-237">
        <title>🦋 OTP &lt;version&gt;/&lt;ser.ver.id&gt;: 2.8.0-SNAPSHOT-237</title>
        <linearGradient id="grad1" x2="0" y2="100%">
          <stop offset="0" stop-opacity=".1" stop-color="#EEE"/>
          <stop offset="1" stop-opacity=".1"/>
        </linearGradient>
        <mask id="mask1"><rect width="3790" height="200" rx="30" fill="#FFF"/></mask>
        <g mask="url(#mask1)">
          <rect width="2280" height="200" fill="firebrick"/>
          <rect width="1510" height="200" fill="#2179BF" x="2280"/>
          <rect width="3790" height="200" fill="url(#grad1)"/>
        </g>
        <g aria-hidden="true" fill="#fff" text-anchor="start" font-family="Verdana,DejaVu Sans,sans-serif" font-size="110">
          <text x="60" y="148" textLength="2180" fill="#000" opacity="0.25">🦋 OTP &lt;version&gt;/&lt;ser.ver.id&gt;</text>
          <text x="50" y="138" textLength="2180">🦋 OTP &lt;version&gt;/&lt;ser.ver.id&gt;</text>
          <text x="2335" y="148" textLength="1410" fill="#000" opacity="0.25">2.8.0-SNAPSHOT-237</text>
          <text x="2325" y="138" textLength="1410">2.8.0-SNAPSHOT-237</text>
        </g>
      </svg>
      """,
      result
    );
  }

  @Test
  void testIsValidColor() {
    assertTrue(isValidColor("red"));
    assertTrue(isValidColor("lightgoldenrodyellow"));
    assertTrue(isValidColor("#FF00A9"));
    assertTrue(isValidColor("rgb( 255, 255, 0 )"));
    assertTrue(isValidColor("rgba(0,0,0,0.5)"));
    assertFalse(isValidColor("red!"));
    assertFalse(isValidColor("#GGGGGG"));
    assertFalse(isValidColor("rgb(A,0,0)"));
  }

  @Test
  void testIsValidLabel() {
    assertTrue(isValidLabel(""));
    assertTrue(isValidLabel("A"));
    String label120 =
      "1234567 10 234567 20 234567 30 234567 40 234567 50 234567 60" +
      " 234567 70 234567 80 234567 90 23456 100 23456 110 23456 120";
    assertTrue(isValidLabel(label120));
    assertFalse(isValidLabel(label120 + "1"));
    assertTrue(isValidLabel("Label with <>§!\"#$%&/()=?+,.;:"));
    assertTrue(isValidLabel("æøåÆØÅôòóö"));
    assertTrue(isValidLabel("⚽️🦤🚌🦋⚙️☕️"));
  }
}
