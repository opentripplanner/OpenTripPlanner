package org.opentripplanner.transit.model.basic;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.Nullable;

/**
 * This is used as a data container for when we want more accurate mode matching than is available
 * with just MainAndSubMode. Currently the additional filtering of replacement and geographical
 * scope is implemented, but this is designed to be capable of holding whatever NeTEx submode/GTFS
 * extended type derived features we implement.
 * <p>
 * Use {@link #of(TransitMode)} to create an instance; all narrowing features are optional and
 * default to "ignored".
 * <p>
 * Only contradictory combinations are rejected here. Combinations that are well-defined but can't
 * match any trip with the current derivation, like a geographical scope on a bus or together with
 * a required replacement, are allowed; the APIs reject them, as only they can tell the client what
 * is wrong.
 *
 * @see org.opentripplanner.model.modes.AllowNarrowedTransitModeFilter
 */
public final class NarrowedTransitMode {

  private static final List<NarrowedTransitMode> ALL = Stream.of(TransitMode.values())
    .map(mode -> of(mode).build())
    .toList();

  private final TransitMode mode;

  @Nullable
  private final SubMode subMode;

  private final ReplacementRequirement replacement;

  @Nullable
  private final GeographicalScope geographicalScope;

  private final Set<GeographicalScope> excludedGeographicalScopes;

  private NarrowedTransitMode(Builder builder) {
    if (builder.geographicalScope != null && !builder.excludedGeographicalScopes.isEmpty()) {
      throw new IllegalArgumentException(
        "A geographical scope can't be combined with excluded geographical scopes"
      );
    }
    this.mode = builder.mode;
    this.subMode = builder.subMode;
    this.replacement = builder.replacement;
    this.geographicalScope = builder.geographicalScope;
    this.excludedGeographicalScopes = builder.excludedGeographicalScopes;
  }

  /**
   * Create a builder for a narrowed mode with the given main mode. Without further narrowing, the
   * result matches all trips of the main mode.
   */
  public static Builder of(TransitMode mode) {
    return new Builder(mode);
  }

  public static NarrowedTransitMode ofMainAndSubMode(MainAndSubMode mode) {
    return of(mode.mainMode()).withSubMode(mode.subMode()).build();
  }

  public static List<NarrowedTransitMode> all() {
    return ALL;
  }

  public boolean isMainModeOnly() {
    return this.subMode == null && isConvertibleToMainAndSubMode();
  }

  /**
   * Whether this only narrows the mode by the main mode and submode, so it can be matched with the
   * faster {@link MainAndSubMode} based filters.
   */
  public boolean isConvertibleToMainAndSubMode() {
    return (
      this.replacement == ReplacementRequirement.IGNORED &&
      this.geographicalScope == null &&
      this.excludedGeographicalScopes.isEmpty()
    );
  }

  public MainAndSubMode toMainAndSubMode() {
    if (!isConvertibleToMainAndSubMode()) {
      throw new IllegalArgumentException("Not convertible to MainAndSubMode");
    }
    return new MainAndSubMode(this.mode, this.subMode);
  }

  public TransitMode getMode() {
    return mode;
  }

  /**
   * null here means that we don't care about what SubMode the trip has
   */
  @Nullable
  public SubMode getSubMode() {
    return subMode;
  }

  public ReplacementRequirement getReplacement() {
    return replacement;
  }

  /**
   * null here means that we don't care about the geographical scope of the trip
   */
  @Nullable
  public GeographicalScope getGeographicalScope() {
    return geographicalScope;
  }

  /**
   * Trips with one of these geographical scopes don't match. Trips without a scope are not
   * excluded. Empty if no scope is excluded.
   */
  public Set<GeographicalScope> getExcludedGeographicalScopes() {
    return excludedGeographicalScopes;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    NarrowedTransitMode that = (NarrowedTransitMode) o;
    return (
      mode == that.mode &&
      Objects.equals(subMode, that.subMode) &&
      replacement == that.replacement &&
      geographicalScope == that.geographicalScope &&
      excludedGeographicalScopes.equals(that.excludedGeographicalScopes)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(mode, subMode, replacement, geographicalScope, excludedGeographicalScopes);
  }

  @Override
  public String toString() {
    if (geographicalScope != null || !excludedGeographicalScopes.isEmpty()) {
      return (
        mode.name() +
        "::" +
        (subMode == null ? "" : subMode.name()) +
        "::" +
        replacement +
        "::" +
        (geographicalScope != null ? geographicalScope : "NOT" + excludedGeographicalScopes)
      );
    }
    if (replacement != ReplacementRequirement.IGNORED) {
      return mode.name() + "::" + (subMode == null ? "" : subMode.name()) + "::" + replacement;
    }
    if (subMode == null) {
      return mode.name();
    }
    return mode.name() + "::" + subMode;
  }

  /**
   * Make sure the String serialization is deterministic by sorting the elements in alphabetic
   * order.
   *
   * @see MainAndSubMode#toString(Collection)
   */
  public static String toString(Collection<NarrowedTransitMode> modes) {
    return modes != null
      ? modes.stream().map(NarrowedTransitMode::toString).sorted().toList().toString()
      : null;
  }

  public static class Builder {

    private final TransitMode mode;

    @Nullable
    private SubMode subMode;

    private ReplacementRequirement replacement = ReplacementRequirement.IGNORED;

    @Nullable
    private GeographicalScope geographicalScope;

    private Set<GeographicalScope> excludedGeographicalScopes = Set.of();

    private Builder(TransitMode mode) {
      this.mode = Objects.requireNonNull(mode);
    }

    /**
     * Only match trips with the given submode, or any submode if null.
     */
    public Builder withSubMode(@Nullable SubMode subMode) {
      this.subMode = subMode;
      return this;
    }

    public Builder withReplacement(ReplacementRequirement replacement) {
      this.replacement = Objects.requireNonNull(replacement);
      return this;
    }

    /**
     * Only match trips with the given geographical scope, or any scope if null.
     */
    public Builder withGeographicalScope(@Nullable GeographicalScope geographicalScope) {
      this.geographicalScope = geographicalScope;
      return this;
    }

    /**
     * Don't match trips with one of the given geographical scopes. Trips without a scope still
     * match. Can't be combined with {@link #withGeographicalScope(GeographicalScope)}.
     */
    public Builder withExcludedGeographicalScopes(Collection<GeographicalScope> scopes) {
      // An EnumSet keeps the scopes in a deterministic order, e.g. for toString()
      this.excludedGeographicalScopes = scopes.isEmpty()
        ? Set.of()
        : Collections.unmodifiableSet(EnumSet.copyOf(scopes));
      return this;
    }

    public NarrowedTransitMode build() {
      return new NarrowedTransitMode(this);
    }
  }
}
