package org.opentripplanner.updater.trip.metrics;

import io.micrometer.core.instrument.Tag;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.opentripplanner.updater.trip.UpdateResult;
import org.opentripplanner.updater.trip.UrlUpdaterParameters;
import org.opentripplanner.util.OTPFeature;

public class TripUpdateMetrics {

  public static final Consumer<UpdateResult> NOOP = ignored -> {};
  protected List<Tag> baseTags;

  TripUpdateMetrics(UrlUpdaterParameters parameters) {
    this.baseTags =
      List.of(
        Tag.of("configRef", parameters.configRef()),
        Tag.of("url", parameters.getUrl()),
        Tag.of("feedId", parameters.getFeedId())
      );
  }

  public static Consumer<UpdateResult> batch(UrlUpdaterParameters parameters) {
    return getConsumer(() -> {
      var metrics = new BatchTripUpdateMetrics(parameters);
      return metrics::setGauges;
    });
  }

  public static Consumer<UpdateResult> streaming(UrlUpdaterParameters parameters) {
    return getConsumer(() -> {
      var metrics = new StreamingTripUpdateMetrics(parameters);
      return metrics::setCounters;
    });
  }

  private static Consumer<UpdateResult> getConsumer(Supplier<Consumer<UpdateResult>> maker) {
    if (OTPFeature.ActuatorAPI.isOn()) {
      return maker.get();
    } else {
      return NOOP;
    }
  }
}
