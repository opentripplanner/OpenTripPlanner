package org.opentripplanner.updater.trip.metrics;

import static com.google.common.truth.Truth.assertThat;

import io.micrometer.core.instrument.Tag;
import org.junit.jupiter.api.Test;
import org.opentripplanner.updater.trip.UrlUpdaterParameters;

class TripUpdateMetricsTest {

  @Test
  void missingUrlIsTaggedWithAnEmptyValue() {
    var metrics = new StreamingTripUpdateMetrics(
      new UrlUpdaterParameters() {
        @Override
        public String url() {
          return null;
        }

        @Override
        public String configRef() {
          return "updaters.[0]";
        }

        @Override
        public String feedId() {
          return "RB";
        }
      }
    );
    assertThat(metrics.baseTags).containsExactly(
      Tag.of("configRef", "updaters.[0]"),
      Tag.of("url", ""),
      Tag.of("feedId", "RB")
    );
  }
}
