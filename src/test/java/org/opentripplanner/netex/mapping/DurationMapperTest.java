package org.opentripplanner.netex.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import org.junit.jupiter.api.Test;

public class DurationMapperTest {

  @Test
  public void mapDurationToSec() {
    assertEquals(10, DurationMapper.mapDurationToSec(Duration.ofSeconds(10), -1));
    assertEquals(-1, DurationMapper.mapDurationToSec(null, -1));
  }
}
