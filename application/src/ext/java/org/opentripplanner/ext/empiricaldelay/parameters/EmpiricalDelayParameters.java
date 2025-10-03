package org.opentripplanner.ext.empiricaldelay.parameters;

import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * Input parameters for the empirical delay module.
 */
public class EmpiricalDelayParameters implements Serializable {

  public static final EmpiricalDelayParameters DEFAULT = new EmpiricalDelayParameters(List.of());

  private final List<EmpiricalDelayFeedParameters> feeds;

  public EmpiricalDelayParameters(List<EmpiricalDelayFeedParameters> feeds) {
    this.feeds = List.copyOf(feeds);
  }

  public static EmpiricalDelayParameters.Builder of() {
    return DEFAULT.copyOf();
  }

  /**
   * List all eempirical delay composite datasources/feeds(file directory/cload bucket/zip).
   */
  public List<URI> listFiles() {
    return feeds.stream().map(f -> f.source()).toList();
  }

  private EmpiricalDelayParameters.Builder copyOf() {
    return new Builder(DEFAULT);
  }

  public List<EmpiricalDelayFeedParameters> feeds() {
    return feeds;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    var that = (EmpiricalDelayParameters) o;
    return Objects.equals(feeds, that.feeds);
  }

  @Override
  public int hashCode() {
    return Objects.hash(feeds);
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(EmpiricalDelayParameters.class).addCol("fedds", feeds).toString();
  }

  public static class Builder {

    private EmpiricalDelayParameters origin;
    private List<EmpiricalDelayFeedParameters> feeds = new ArrayList<>();

    public Builder(EmpiricalDelayParameters origin) {
      this.origin = origin;
    }

    public Builder addFeeds(Collection<EmpiricalDelayFeedParameters> feeds) {
      this.feeds.addAll(feeds);
      return this;
    }

    public EmpiricalDelayParameters build() {
      var candidate = new EmpiricalDelayParameters(feeds);
      return origin.equals(candidate) ? origin : candidate;
    }
  }
}
