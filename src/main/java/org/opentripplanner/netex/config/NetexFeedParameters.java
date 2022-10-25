package org.opentripplanner.netex.config;

import static java.util.Objects.requireNonNull;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import org.opentripplanner.graph_builder.model.DataSourceConfig;
import org.opentripplanner.util.lang.ToStringBuilder;

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

  private static final Set<String> FERRY_IDS_NOT_ALLOWED_FOR_BICYCLE = Collections.emptySet();

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

  /** Se configuration documentation. */
  public String feedId() {
    return feedId;
  }

  /**
   * This field is used to exclude matching <em>files</em> in the module file(zip file entries). The
   * <em>ignored</em> files are <em>not</em> loaded.
   * <p>
   * Default value is <code>'$^'</code> which matches empty stings (not a valid file name).
   *
   * @see #sharedFilePattern
   */
  public Pattern ignoreFilePattern() {
    return Pattern.compile(ignoreFilePattern);
  }

  /**
   * This field is used to match <em>shared files</em>(zip file entries) in the module file. Shared
   * files are loaded first. Then the rest of the files are grouped and loaded.
   * <p>
   * The pattern <code>'shared-data\.xml'</code> matches <code>'shared-data.xml'</code>
   * <p>
   * File names are matched in the following order - and treated accordingly to the first match:
   * <ol>
   *     <li>{@link #ignoreFilePattern}</li>
   *     <li>Shared file pattern (this)</li>
   *     <li>{@link #sharedGroupFilePattern}.</li>
   *     <li>{@link #groupFilePattern}.</li>
   * </ol>
   * <p>
   * Default value is <code>'shared-data\.xml'</code>
   */
  public Pattern sharedFilePattern() {
    return Pattern.compile(sharedFilePattern);
  }

  /**
   * This field is used to match <em>shared group files</em> in the module file(zip file entries).
   * Typically this is used to group all files from one agency together.
   * <p>
   * <em>Shared group files</em> are loaded after shared files, but before the matching group
   * files. Each <em>group</em> of files are loaded as a unit, followed by next group.
   * <p>
   * Files are grouped together by the first group pattern in the regular expression.
   * <p>
   * The pattern <code>'(\w{3})-.*-shared\.xml'</code> matches <code>'RUT-shared.xml'</code> with
   * group <code>'RUT'</code>.
   * <p>
   * Default value is <code>'(\w{3})-.*-shared\.xml'</code>
   *
   * @see #sharedFilePattern
   * @see #groupFilePattern
   */
  public Pattern sharedGroupFilePattern() {
    return Pattern.compile(sharedGroupFilePattern);
  }

  /**
   * This field is used to match <em>group files</em> in the module file(zip file entries).
   * <em>group files</em> are loaded right the after <em>shared group files</em> are loaded.
   * <p>
   * Files are grouped together by the first group pattern in the regular expression.
   * <p>
   * The pattern <code>'(\w{3})-.*\.xml'</code> matches <code>'RUT-Line-208-Hagalia-Nevlunghavn.xml'</code>
   * with group <code>'RUT'</code>.
   * <p>
   * Default value is <code>'(\w{3})-.*\.xml'</code>
   *
   * @see #sharedFilePattern
   * @see #sharedGroupFilePattern
   */
  public Pattern groupFilePattern() {
    return Pattern.compile(groupFilePattern);
  }

  /**
   * Bicycles are allowed on most ferries however Nordic profile doesn't contain a place where
   * bicycle conveyance can be defined.
   * <p>
   * For this reason we allow bicycles on ferries by default and allow to override the rare case
   * where this is not the case.
   */
  public Set<String> ferryIdsNotAllowedForBicycle() {
    return ferryIdsNotAllowedForBicycle;
  }

  public boolean noTransfersOnIsolatedStops() {
    return noTransfersOnIsolatedStops;
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
      ferryIdsNotAllowedForBicycle
    );
  }

  @Override
  public String toString() {
    return ToStringBuilder
      .of(NetexFeedParameters.class)
      .addObj("source", source, null)
      .addObj("feedId", feedId, DEFAULT.feedId)
      .addStr("sharedFilePattern", sharedFilePattern, DEFAULT.sharedFilePattern)
      .addStr("sharedGroupFilePattern", sharedGroupFilePattern, DEFAULT.sharedGroupFilePattern)
      .addStr("groupFilePattern", groupFilePattern, DEFAULT.groupFilePattern)
      .addStr("ignoreFilePattern", ignoreFilePattern, DEFAULT.ignoreFilePattern)
      .addCol("ferryIdsNotAllowedForBicycle", ferryIdsNotAllowedForBicycle, Set.of())
      .toString();
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
    private boolean noTransfersOnIsolatedStops;

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

    public Builder withNoTransfersOnIsolatedStops(boolean noTransfersOnIsolatedStops) {
      this.noTransfersOnIsolatedStops = noTransfersOnIsolatedStops;
      return this;
    }

    public NetexFeedParameters build() {
      var value = new NetexFeedParameters(this);
      return original.equals(value) ? original : value;
    }
  }
}
