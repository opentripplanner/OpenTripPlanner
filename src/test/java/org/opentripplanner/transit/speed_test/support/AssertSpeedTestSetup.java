package org.opentripplanner.transit.speed_test.support;

import org.opentripplanner.framework.application.OtpAppException;
import org.opentripplanner.framework.application.OtpFileNames;
import org.opentripplanner.standalone.config.BuildConfig;
import org.opentripplanner.transit.service.TransitModel;
import org.opentripplanner.transit.speed_test.options.SpeedTestConfig;

public class AssertSpeedTestSetup {

  public static void assertTestDateHasData(
    TransitModel transitModel,
    SpeedTestConfig config,
    BuildConfig buildConfig
  ) {
    int numberOfPatternForTestDate = transitModel
      .getTransitLayer()
      .getTripPatternsForDate(config.testDate)
      .size();

    if (numberOfPatternForTestDate < 10) {
      throw new OtpAppException(
        """
        Number of trip patterns on test date is less than 10. There is probably something wrong
        with your config or data. Check that the graph build contains transit stops and patterns
        (see build log) and check that the 'testDate' is in the period of your data.

          number of patterns on date: %d
          "testDate":                 %s (speed-test-config.json)
          "transitServiceStart":      %s (%s)
          "transitServiceEnd":        %s (%s)
        
        """.formatted(
            numberOfPatternForTestDate,
            config.testDate,
            buildConfig.transitServiceStart,
            OtpFileNames.BUILD_CONFIG_FILENAME,
            buildConfig.transitServiceEnd,
            OtpFileNames.BUILD_CONFIG_FILENAME
          )
      );
    }
  }
}
