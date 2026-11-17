package org.opentripplanner.routing.graph.kryosupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.routing.graph.kryosupport.KryoTestHelper.writeAndRead;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AtomicIntegerSerializerTest {

  @ParameterizedTest
  @ValueSource(ints = { 0, 1, -1, 42, Integer.MIN_VALUE, Integer.MAX_VALUE })
  void roundTrip(int value) {
    assertEquals(value, writeAndRead(new AtomicInteger(value)).get());
  }

  @Test
  void counterCanBeIncrementedAfterRead() {
    AtomicInteger read = writeAndRead(new AtomicInteger(10));
    assertEquals(11, read.incrementAndGet());
  }

  @Test
  void sharedInstanceIsStillSharedAfterRead() {
    var counter = new AtomicInteger(5);
    var read = writeAndRead(java.util.List.of(counter, counter));
    assertEquals(read.get(0), read.get(1));
    read.get(0).incrementAndGet();
    assertEquals(6, read.get(1).get());
  }
}
