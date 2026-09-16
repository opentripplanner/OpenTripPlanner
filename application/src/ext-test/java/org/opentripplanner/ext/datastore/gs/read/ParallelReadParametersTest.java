package org.opentripplanner.ext.datastore.gs.read;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ParallelReadParametersTest {

  private static final int MB = 1024 * 1024;
  private static final ParallelReadParameters DEFAULTS = ParallelReadParameters.DEFAULT;

  @Test
  void disabledConfigurationNeverPlansAParallelRead() {
    var off = new ParallelReadParameters(0, 16 * MB, 32L * MB);
    assertThat(off.planFor(10L * 1024 * MB)).isNull();
  }

  @ParameterizedTest
  @ValueSource(longs = { -1, 0, 1, 31L * 1024 * 1024 })
  void smallOrUnknownObjectsAreReadSequentially(long size) {
    assertThat(DEFAULTS.planFor(size)).isNull();
  }

  @Test
  void aGraphSizedObjectGetsTheConfiguredBlockSizeAndWorkers() {
    // The dev graph object, 2.04 GiB.
    var plan = DEFAULTS.planFor(2_187_609_888L);

    assertThat(plan).isNotNull();
    assertThat(plan.blockSize()).isEqualTo(16 * MB);
    assertThat(plan.blockCount()).isEqualTo(131);
    assertThat(plan.parallelism()).isEqualTo(4);
    assertThat(plan.inFlight()).isEqualTo(12);
    assertThat(plan.poolBytes()).isEqualTo(192L * MB);
  }

  @Test
  void blockSizeShrinksSoEveryWorkerStillGetsSeveralBlocks() {
    // 40 MB with 16 MB blocks would be under 3 blocks for 4 workers; the block size is clamped
    // down so a slow connection stays amortised over a small share of the object.
    var plan = DEFAULTS.planFor(40L * MB);

    assertThat(plan).isNotNull();
    assertThat(plan.blockSize()).isEqualTo(ParallelReadParameters.MIN_BLOCK_SIZE_BYTES);
    assertThat(plan.blockCount()).isEqualTo(10);
    assertThat(plan.parallelism()).isEqualTo(4);
    assertThat(plan.inFlight()).isEqualTo(10);
  }

  @Test
  void theObjectAtTheThresholdIsStillReadInParallel() {
    assertThat(DEFAULTS.planFor(32L * MB)).isNotNull();
  }

  @Test
  void neverAllocatesMoreOffHeapThanTheCeiling() {
    var wide = new ParallelReadParameters(16, 16 * MB, 32L * MB);
    var plan = wide.planFor(100L * 1024 * MB);

    assertThat(plan).isNotNull();
    assertThat(plan.poolBytes()).isAtMost(ParallelReadParameters.MAX_POOL_BYTES);
  }

  @Test
  void rejectsValuesOutOfRange() {
    assertThrows(IllegalArgumentException.class, () ->
      new ParallelReadParameters(17, 16 * MB, 32L * MB)
    );
    assertThrows(IllegalArgumentException.class, () ->
      new ParallelReadParameters(-1, 16 * MB, 32L * MB)
    );
    assertThrows(IllegalArgumentException.class, () -> new ParallelReadParameters(4, MB, 32L * MB));
    assertThrows(IllegalArgumentException.class, () -> new ParallelReadParameters(4, 16 * MB, -1));
  }

  @Test
  void aDisabledConfigurationMayUseAnyBlockSize() {
    // Validation of the block size only matters when the feature is on.
    assertThat(new ParallelReadParameters(0, 1, 0).planFor(1_000_000)).isNull();
  }
}
