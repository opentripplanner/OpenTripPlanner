package org.opentripplanner.ext.datastore.gs;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.opentripplanner.standalone.config.framework.json.JsonSupport.newNodeAdapterForTest;

import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.datastore.gs.config.GsConfig;
import org.opentripplanner.framework.application.OtpAppException;

class GsConfigTest {

  @Test
  void parsesTheParallelReadParameters() {
    var config = GsConfig.fromConfig(
      newNodeAdapterForTest(
        """
        { "gsConfig" : { "parallelReads" : 8, "parallelReadBlockSizeMb" : 8, "parallelReadMinSizeMb" : 0 } }
        """
      ),
      "gsConfig"
    );

    assertThat(config.parallelReads()).isEqualTo(8);
    assertThat(config.parallelReadBlockSizeMb()).isEqualTo(8);
    assertThat(config.parallelReadMinSizeMb()).isEqualTo(0);
  }

  @Test
  void anOutOfRangeValueIsAConfigurationErrorNamingTheBlock() {
    var root = newNodeAdapterForTest("{ \"gsConfig\" : { \"parallelReads\" : 1 } }");

    var e = assertThrows(OtpAppException.class, () -> GsConfig.fromConfig(root, "gsConfig"));

    assertThat(e).hasMessageThat().contains("gsConfig");
    assertThat(e).hasMessageThat().contains("parallelReads");
  }
}
