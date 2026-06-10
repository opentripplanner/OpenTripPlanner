package org.opentripplanner.graph_builder.module.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.doc.DocumentedEnumTestHelper;

class CacheTaskTest {

  @Test
  void cacheFileName_followsNamingConvention() {
    assertEquals("elevation-cache-1.obj", CacheTask.ELEVATION.cacheFileName());
    assertEquals("visibility-cache-1.obj", CacheTask.VISIBILITY.cacheFileName());
  }

  @Test
  void doc() {
    DocumentedEnumTestHelper.verifyHasDocumentation(CacheTask.values());
  }
}
