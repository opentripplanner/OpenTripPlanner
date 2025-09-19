package org.opentripplanner.api.model.serverinfo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.api.model.serverinfo.OtpBadgeGenerator.generateOtpBadgeSvg;

import org.junit.jupiter.api.Test;

public class OtpBadgeGeneratorTest {

  @Test
  void getVersionBadge() {
    String result = generateOtpBadgeSvg(
      "OTP version/ser.ver.id",
      "firebrick",
      "2.8.0-SNAPSHOT-237"
    );

    assertEquals(
      """
      <svg width="324.9" height="20" viewBox="0 0 3249 200"
          xmlns="http://www.w3.org/2000/svg" role="img" aria-label="OTP version/ser.ver.id: 2.8.0-SNAPSHOT-237">
        <title>OTP version/ser.ver.id: 2.8.0-SNAPSHOT-237</title>
        <linearGradient id="grad1" x2="0" y2="100%">
          <stop offset="0" stop-opacity=".1" stop-color="#EEE"/>
          <stop offset="1" stop-opacity=".1"/>
        </linearGradient>
        <mask id="mask1"><rect width="3249" height="200" rx="30" fill="#FFF"/></mask>
        <g mask="url(#mask1)">
          <rect width="1739" height="200" fill="firebrick"/>
          <rect width="1510" height="200" fill="#2179BF" x="1739"/>
          <rect width="3249" height="200" fill="url(#grad1)"/>
        </g>
        <g aria-hidden="true" fill="#fff" text-anchor="start" font-family="Verdana,DejaVu Sans,sans-serif" font-size="110">
          <text x="60" y="148" textLength="1639" fill="#000" opacity="0.25">OTP version/ser.ver.id</text>
          <text x="50" y="138" textLength="1639">OTP version/ser.ver.id</text>
          <text x="1794" y="148" textLength="1410" fill="#000" opacity="0.25">2.8.0-SNAPSHOT-237</text>
          <text x="1784" y="138" textLength="1410">2.8.0-SNAPSHOT-237</text>
        </g>
      </svg>
      """,
      result
    );
  }
}
