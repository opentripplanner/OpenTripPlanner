package org.opentripplanner.routing.api.request.request.filter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * Select a given set of transit routes base on the list of
 * modes, sub-modes, agencies and routes. A transit entity matches
 * if mode, sub-mode, agencyId or routeId matches - only one
 * "thing" needs to match.
 * <p>
 * The {@code TransitGroupSelect(modes:[BUS, TRAM], agencyIds:[A1, A3])} matches both:
 * <ul>
 *   <li>{@code Entity(mode:BUS, agency:ANY)} and</li>
 *   <li>{@code Entity(mode:SUBWAY, agency:A3)}</li>
 * </ul>
 */
public class TransitGroupSelect {

  private static final TransitGroupSelect DEFAULT = new TransitGroupSelect();

  private final List<TransitMode> modes;
  private final List<String> subModeRegexp;
  private final List<FeedScopedId> agencyIds;
  private final List<FeedScopedId> routeIds;

  public TransitGroupSelect() {
    this.modes = List.of();
    this.subModeRegexp = List.of();
    this.agencyIds = List.of();
    this.routeIds = List.of();
  }

  private TransitGroupSelect(Builder builder) {
    // Sort and keep only unique entries, this make this
    // implementation consistent for eq/hc/toString.
    this.modes = List.copyOf(
      builder.modes.stream().sorted(Comparator.comparingInt(Enum::ordinal)).distinct().toList()
    );
    this.subModeRegexp = List.copyOf(builder.subModeRegexp.stream().sorted().distinct().toList());
    this.agencyIds = List.copyOf(builder.agencyIds.stream().sorted().distinct().toList());
    this.routeIds = List.copyOf(builder.routeIds.stream().sorted().distinct().toList());
  }

  public static Builder of() {
    return new Builder(DEFAULT);
  }

  public List<TransitMode> modes() {
    return modes;
  }

  public List<String> subModeRegexp() {
    return subModeRegexp;
  }

  public List<FeedScopedId> agencyIds() {
    return agencyIds;
  }

  public List<FeedScopedId> routeIds() {
    return routeIds;
  }

  public boolean isEmpty() {
    return modes.isEmpty() && subModeRegexp.isEmpty() && agencyIds.isEmpty() && routeIds.isEmpty();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    TransitGroupSelect that = (TransitGroupSelect) o;
    return (
      Objects.equals(modes, that.modes) &&
      Objects.equals(subModeRegexp, that.subModeRegexp) &&
      Objects.equals(agencyIds, that.agencyIds) &&
      Objects.equals(routeIds, that.routeIds)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(modes, subModeRegexp, agencyIds, routeIds);
  }

  @Override
  public String toString() {
    return isEmpty()
      ? "TransitGroupSelect{ EMPTY }"
      : ToStringBuilder.of(TransitGroupSelect.class)
        .addCol("modes", modes)
        .addCol("subModeRegexp", subModeRegexp)
        .addCol("agencyIds", agencyIds)
        .addCol("routeIds", routeIds)
        .toString();
  }

  public static class Builder {

    private final TransitGroupSelect original;
    private final List<TransitMode> modes;
    private final List<String> subModeRegexp;
    private final List<FeedScopedId> agencyIds;
    private final List<FeedScopedId> routeIds;

    public Builder(TransitGroupSelect original) {
      this.original = original;
      this.modes = new ArrayList<>(original.modes);
      this.subModeRegexp = new ArrayList<>(original.subModeRegexp);
      this.agencyIds = new ArrayList<>(original.agencyIds);
      this.routeIds = new ArrayList<>(original.routeIds);
    }

    public Builder addModes(Collection<TransitMode> modes) {
      this.modes.addAll(modes);
      return this;
    }

    public Builder addSubModeRegexp(Collection<String> subModeRegexp) {
      this.subModeRegexp.addAll(subModeRegexp);
      return this;
    }

    public Builder addAgencyIds(Collection<FeedScopedId> agencyIds) {
      this.agencyIds.addAll(agencyIds);
      return this;
    }

    public Builder addRouteIds(Collection<FeedScopedId> routeIds) {
      this.routeIds.addAll(routeIds);
      return this;
    }

    public TransitGroupSelect build() {
      var obj = new TransitGroupSelect(this);
      return original.equals(obj) ? original : obj;
    }
  }
}
