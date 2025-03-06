package org.opentripplanner.netex.config;

import static java.util.Objects.requireNonNull;
import static org.opentripplanner.netex.config.IgnorableFeature.FARE_FRAME;
import static org.opentripplanner.netex.config.IgnorableFeature.PARKING;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import org.opentripplanner.graph_builder.model.DataSourceConfig;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * Parameters to configure the NETEX import. Se the generated build-config documentation or
 * the config mapping for documentation.
 */
public class NetexFeedParameters implements DataSourceConfig {

  /**
   * The default feed-id is used if the auto-discover feature is used; hence no feeds
   * are present in the build-config.
   */
  private static final String NETEX_FEED_ID = "NETEX";
  private static final String EMPTY_STRING_PATTERN = "$^";
  private static final String IGNORE_FILE_PATTERN = EMPTY_STRING_PATTERN;
  private static final String SHARED_FILE_PATTERN = "shared-data\\.xml";
  private static final String SHARED_GROUP_FILE_PATTERN = "(\\w{3})-.*-shared\\.xml";
  private static final String GROUP_FILE_PATTERN = "(\\w{3})-.*\\.xml";
  private static final boolean NO_TRANSFERS_ON_ISOLATED_STOPS = false;
  private static final Set<IgnorableFeature> IGNORED_FEATURES = Set.of(PARKING);

  private static final Set<String> FERRY_IDS_NOT_ALLOWED_FOR_BICYCLE = Collections.emptySet();
  private static final Set<String> ROUTE_TO_CENTROID_STATION_IDS = Collections.emptySet();

  public static final NetexFeedParameters DEFAULT = new NetexFeedParameters();

  private final URI source;
  private final String feedId;

  /*
    All Patterns are stored as String because the Pattern class to not support the JavaBean spec;
    Pattern.compile(PATTERN).equals(Pattern.compile(PATTERN)) is false.
  */
  private final String sharedFilePattern;
  private final String sharedGroupFilePattern;
  private final String groupFilePattern;
  private final String ignoreFilePattern;
  private final Set<String> ferryIdsNotAllowedForBicycle;
  private final boolean noTransfersOnIsolatedStops;
  private final Set<IgnorableFeature> ignoredFeatures;

  private NetexFeedParameters() {
    this.source = null;
    this.feedId = NETEX_FEED_ID;

    // Verify patterns by creating one
    {
      this.sharedFilePattern = Pattern.compile(SHARED_FILE_PATTERN).pattern();
      this.sharedGroupFilePattern = Pattern.compile(SHARED_GROUP_FILE_PATTERN).pattern();
      this.groupFilePattern = Pattern.compile(GROUP_FILE_PATTERN).pattern();
      this.ignoreFilePattern = Pattern.compile(IGNORE_FILE_PATTERN).pattern();
    }
    this.ferryIdsNotAllowedForBicycle = FERRY_IDS_NOT_ALLOWED_FOR_BICYCLE;
    this.noTransfersOnIsolatedStops = NO_TRANSFERS_ON_ISOLATED_STOPS;
    this.ignoredFeatures = IGNORED_FEATURES;
  }

  private NetexFeedParameters(Builder builder) {
    this.source = builder.source;
    this.feedId = requireNonNull(builder.feedId);
    this.sharedFilePattern = requireNonNull(builder.sharedFilePattern);
    this.sharedGroupFilePattern = requireNonNull(builder.sharedGroupFilePattern);
    this.groupFilePattern = requireNonNull(builder.groupFilePattern);
    this.ignoreFilePattern = requireNonNull(builder.ignoreFilePattern);
    this.ferryIdsNotAllowedForBicycle = Set.copyOf(builder.ferryIdsNotAllowedForBicycle);
    this.noTransfersOnIsolatedStops = builder.noTransfersOnIsolatedStops;
    this.ignoredFeatures = Set.copyOf(builder.ignoredFeatures);
  }

  public static Builder of() {
    return DEFAULT.copyOf();
  }

  public Builder copyOf() {
    return new Builder(this);
  }

  public URI source() {
    return source;
  }

  /** See {@link org.opentripplanner.standalone.config.buildconfig.NetexConfig}. */
  public String feedId() {
    return feedId;
  }

  /** See {@link org.opentripplanner.standalone.config.buildconfig.NetexConfig}. */
  public Pattern sharedFilePattern() {
    return Pattern.compile(sharedFilePattern);
  }

  /** See {@link org.opentripplanner.standalone.config.buildconfig.NetexConfig}. */
  public Pattern sharedGroupFilePattern() {
    return Pattern.compile(sharedGroupFilePattern);
  }

  /** See {@link org.opentripplanner.standalone.config.buildconfig.NetexConfig}. */
  public Pattern groupFilePattern() {
    return Pattern.compile(groupFilePattern);
  }

  /** See {@link org.opentripplanner.standalone.config.buildconfig.NetexConfig}. */
  public Pattern ignoreFilePattern() {
    return Pattern.compile(ignoreFilePattern);
  }

  /** See {@link org.opentripplanner.standalone.config.buildconfig.NetexConfig}. */
  public Set<String> ferryIdsNotAllowedForBicycle() {
    return ferryIdsNotAllowedForBicycle;
  }

  /** See {@link org.opentripplanner.standalone.config.buildconfig.NetexConfig}. */
  public boolean noTransfersOnIsolatedStops() {
    return noTransfersOnIsolatedStops;
  }

  /** See {@link org.opentripplanner.standalone.config.buildconfig.NetexConfig}. */
  public boolean ignoreFareFrame() {
    return ignoredFeatures.contains(FARE_FRAME);
  }

  public boolean ignoreParking() {
    return ignoredFeatures.contains(PARKING);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    NetexFeedParameters that = (NetexFeedParameters) o;
    return (
      Objects.equals(source, that.source) &&
      feedId.equals(that.feedId) &&
      ignoreFilePattern.equals(that.ignoreFilePattern) &&
      sharedFilePattern.equals(that.sharedFilePattern) &&
      sharedGroupFilePattern.equals(that.sharedGroupFilePattern) &&
      groupFilePattern.equals(that.groupFilePattern) &&
      ignoredFeatures.equals(that.ignoredFeatures) &&
      ferryIdsNotAllowedForBicycle.equals(that.ferryIdsNotAllowedForBicycle)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(
      source,
      feedId,
      ignoreFilePattern,
      sharedFilePattern,
      sharedGroupFilePattern,
      groupFilePattern,
      ignoredFeatures,
      ferryIdsNotAllowedForBicycle
    );
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(NetexFeedParameters.class)
      .addObj("source", source, null)
      .addObj("feedId", feedId, DEFAULT.feedId)
      .addStr("sharedFilePattern", sharedFilePattern, DEFAULT.sharedFilePattern)
      .addStr("sharedGroupFilePattern", sharedGroupFilePattern, DEFAULT.sharedGroupFilePattern)
      .addStr("groupFilePattern", groupFilePattern, DEFAULT.groupFilePattern)
      .addStr("ignoreFilePattern", ignoreFilePattern, DEFAULT.ignoreFilePattern)
      .addCol("ignoredFeatures", ignoredFeatures)
      .addCol("ferryIdsNotAllowedForBicycle", ferryIdsNotAllowedForBicycle, Set.of())
      .toString();
  }

  public Set<IgnorableFeature> ignoredFeatures() {
    return ignoredFeatures;
  }

  public static class Builder {

    private final NetexFeedParameters original;
    private URI source;
    private String feedId;
    private String sharedFilePattern;
    private String sharedGroupFilePattern;
    private String groupFilePattern;
    private String ignoreFilePattern;
    private final Set<String> ferryIdsNotAllowedForBicycle = new HashSet<>();
    private final Set<String> routeToCentroidStopPlaceIds = new HashSet<>();
    private boolean noTransfersOnIsolatedStops;
    private final Set<IgnorableFeature> ignoredFeatures;

    private Builder(NetexFeedParameters original) {
      this.original = original;
      this.source = original.source();
      this.feedId = original.feedId();
      this.sharedFilePattern = original.sharedFilePattern;
      this.sharedGroupFilePattern = original.sharedGroupFilePattern;
      this.groupFilePattern = original.groupFilePattern;
      this.ignoreFilePattern = original.ignoreFilePattern;
      this.ferryIdsNotAllowedForBicycle.addAll(original.ferryIdsNotAllowedForBicycle);
      this.noTransfersOnIsolatedStops = original.noTransfersOnIsolatedStops;
      this.ignoredFeatures = new HashSet<>(original.ignoredFeatures);
    }

    public URI source() {
      return source;
    }

    public Builder withSource(URI source) {
      this.source = source;
      return this;
    }

    public Builder withFeedId(String feedId) {
      this.feedId = feedId;
      return this;
    }

    public Builder withSharedFilePattern(Pattern sharedFilePattern) {
      this.sharedFilePattern = sharedFilePattern.pattern();
      return this;
    }

    public Builder withSharedGroupFilePattern(Pattern sharedGroupFilePattern) {
      this.sharedGroupFilePattern = sharedGroupFilePattern.pattern();
      return this;
    }

    public Builder withGroupFilePattern(Pattern groupFilePattern) {
      this.groupFilePattern = groupFilePattern.pattern();
      return this;
    }

    public Builder withIgnoreFilePattern(Pattern ignoreFilePattern) {
      this.ignoreFilePattern = ignoreFilePattern.pattern();
      return this;
    }

    public Builder addFerryIdsNotAllowedForBicycle(Collection<String> ferryId) {
      ferryIdsNotAllowedForBicycle.addAll(ferryId);
      return this;
    }

    public Builder addRouteToCentroidStopPlaceIds(Collection<String> stationIds) {
      routeToCentroidStopPlaceIds.addAll(stationIds);
      return this;
    }

    public Builder withNoTransfersOnIsolatedStops(boolean noTransfersOnIsolatedStops) {
      this.noTransfersOnIsolatedStops = noTransfersOnIsolatedStops;
      return this;
    }

    public Builder withIgnoreFareFrame(boolean ignoreFareFrame) {
      return applyIgnore(ignoreFareFrame, FARE_FRAME);
    }

    public Builder withIgnoreParking(boolean ignoreParking) {
      return applyIgnore(ignoreParking, PARKING);
    }

    private Builder applyIgnore(boolean ignore, IgnorableFeature feature) {
      if (ignore) {
        ignoredFeatures.add(feature);
      } else {
        ignoredFeatures.remove(feature);
      }
      return this;
    }

    public NetexFeedParameters build() {
      var value = new NetexFeedParameters(this);
      return original.equals(value) ? original : value;
    }
  }
}
