package org.opentripplanner.generate.doc.support;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.generate.doc.support.OTPFeatureTable.otpFeaturesTable;

import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.application.OTPFeature;

public class OTPFeatureTableTest {

  @Test
  void test() {
    var table = otpFeaturesTable();
    for (OTPFeature it : OTPFeature.values()) {
      assertTrue(table.contains(it.name()), table);
    }
  }
}
