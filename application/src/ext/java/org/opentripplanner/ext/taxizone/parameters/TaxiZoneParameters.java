package org.opentripplanner.ext.taxizone.parameters;

import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * Input parameters for the taxi zone module.
 */
public class TaxiZoneParameters implements Serializable {

  public static final TaxiZoneParameters DEFAULT = new TaxiZoneParameters(List.of());

  private final List<TaxiZoneFeedParameters> feeds;

  public TaxiZoneParameters(List<TaxiZoneFeedParameters> feeds) {
    this.feeds = List.copyOf(feeds);
  }

  public static TaxiZoneParameters.Builder of() {
    return DEFAULT.copyOf();
  }

  /**
   * List all taxi zone composite datasources/feeds(file directory/cloud bucket/zip).
   */
  public List<URI> listFiles() {
    return feeds
      .stream()
      .map(f -> f.source())
      .toList();
  }

  private TaxiZoneParameters.Builder copyOf() {
    return new Builder(DEFAULT);
  }

  public List<TaxiZoneFeedParameters> feeds() {
    return feeds;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    var that = (TaxiZoneParameters) o;
    return Objects.equals(feeds, that.feeds);
  }

  @Override
  public int hashCode() {
    return Objects.hash(feeds);
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(TaxiZoneParameters.class).addCol("feeds", feeds).toString();
  }

  public static class Builder {

    private TaxiZoneParameters origin;
    private List<TaxiZoneFeedParameters> feeds = new ArrayList<>();

    public Builder(TaxiZoneParameters origin) {
      this.origin = origin;
    }

    public Builder addFeeds(Collection<TaxiZoneFeedParameters> feeds) {
      this.feeds.addAll(feeds);
      return this;
    }

    public TaxiZoneParameters build() {
      var candidate = new TaxiZoneParameters(feeds);
      return origin.equals(candidate) ? origin : candidate;
    }
  }
}
