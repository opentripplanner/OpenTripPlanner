package org.opentripplanner.kryo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.routing.graph.kryosupport.KryoTestHelper.writeAndRead;

import org.junit.jupiter.api.Test;
import org.opentripplanner.standalone.config.BuildConfig;
import org.opentripplanner.standalone.config.framework.file.ConfigFileLoader;

class BuildConfigSerializerTest {

  @Test
  void roundTripDefaultConfig() {
    BuildConfig read = writeAndRead(BuildConfig.DEFAULT);
    assertEquals(BuildConfig.DEFAULT.toJson(), read.toJson());
  }

  @Test
  void roundTripConfigWithValues() {
    var json = "{ configVersion: \"v1\", subwayAccessTime: 3.5, maxStopToShapeSnapDistance: 77 }";
    var original = new BuildConfig(ConfigFileLoader.nodeFromString(json, "test"), "test", false);

    BuildConfig read = writeAndRead(original);

    assertEquals("v1", read.configVersion);
    assertEquals(original.subwayAccessTime, read.subwayAccessTime);
    assertEquals(original.toJson(), read.toJson());
  }
}
