package org.opentripplanner.kryo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.routing.graph.kryosupport.KryoTestHelper.writeAndRead;

import org.junit.jupiter.api.Test;
import org.opentripplanner.standalone.config.RouterConfig;
import org.opentripplanner.standalone.config.framework.file.ConfigFileLoader;

class RouterConfigSerializerTest {

  @Test
  void roundTripDefaultConfig() {
    RouterConfig read = writeAndRead(RouterConfig.DEFAULT);
    assertEquals(RouterConfig.DEFAULT.toJson(), read.toJson());
  }

  @Test
  void roundTripConfigWithValues() {
    var json = "{ configVersion: \"v2\", server: { apiProcessingTimeout: \"17s\" } }";
    var original = new RouterConfig(ConfigFileLoader.nodeFromString(json, "test"), "test", false);

    RouterConfig read = writeAndRead(original);

    assertEquals("v2", read.getConfigVersion());
    assertEquals(original.server().apiProcessingTimeout(), read.server().apiProcessingTimeout());
    assertEquals(original.toJson(), read.toJson());
  }
}
