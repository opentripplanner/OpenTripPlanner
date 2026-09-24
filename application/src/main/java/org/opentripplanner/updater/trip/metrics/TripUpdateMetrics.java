package org.opentripplanner.updater.trip.metrics;

import io.micrometer.core.instrument.Tag;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.updater.spi.UpdateResult;
import org.opentripplanner.updater.trip.UrlUpdaterParameters;

public class TripUpdateMetrics {

  public static final Consumer<UpdateResult> NOOP = ignored -> {};
  protected List<Tag> baseTags;

  TripUpdateMetrics(UrlUpdaterParameters parameters) {
    this.baseTags = List.of(
      tag("configRef", parameters.configRef()),
      tag("url", parameters.url()),
      tag("feedId", parameters.feedId())
    );
  }

  /**
   * Optional parameters, like the url of a streaming updater, are tagged with an empty value.
   * The tag is kept so that all meters with the same name have the same set of tag keys.
   */
  private static Tag tag(String key, @Nullable String value) {
    return Tag.of(key, Objects.requireNonNullElse(value, ""));
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
