package org.opentripplanner.routing.algorithm.raptoradapter.transit.request;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import org.opentripplanner.routing.api.request.request.filter.TransitPriorityGroupSelect;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.TripPattern;

/**
 * This class turn a {@link TransitPriorityGroupSelect} into a matcher.
 * <p>
 * Design: It uses the composite design pattern. A matcher is created for each
 * value in the "select", then the list of none empty matchers are merged into
 * a `CompositeMatcher`. So, a new matcher is only created if the field in the
 * select is present.
 */
public abstract class PriorityGroupMatcher {

  private static final PriorityGroupMatcher NOOP = new PriorityGroupMatcher() {
    @Override
    boolean match(TripPattern pattern) {
      return false;
    }

    @Override
    boolean isEmpty() {
      return true;
    }
  };

  public static PriorityGroupMatcher of(TransitPriorityGroupSelect select) {
    if (select.isEmpty()) {
      return NOOP;
    }
    List<PriorityGroupMatcher> list = new ArrayList<>();

    if (!select.modes().isEmpty()) {
      list.add(new ModeMatcher(select.modes()));
    }
    if (!select.subModeRegexp().isEmpty()) {
      list.add(new RegExpMatcher(select.subModeRegexp(), p -> p.getNetexSubmode().name()));
    }
    if (!select.agencyIds().isEmpty()) {
      list.add(new IdMatcher(select.agencyIds(), p -> p.getRoute().getAgency().getId()));
    }
    if (!select.routeIds().isEmpty()) {
      list.add(new IdMatcher(select.agencyIds(), p -> p.getRoute().getId()));
    }
    return compositeOf(list);
  }

  private static PriorityGroupMatcher compositeOf(List<PriorityGroupMatcher> list) {
    // Remove empty/noop matchers
    list = list.stream().filter(Predicate.not(PriorityGroupMatcher::isEmpty)).toList();

    if (list.isEmpty()) {
      return NOOP;
    }
    if (list.size() == 1) {
      return list.get(0);
    }
    return new CompositeMatcher(list);
  }

  abstract boolean match(TripPattern pattern);

  boolean isEmpty() {
    return false;
  }

  private static final class ModeMatcher extends PriorityGroupMatcher {

    private final Set<TransitMode> modes;

    public ModeMatcher(List<TransitMode> modes) {
      this.modes = EnumSet.copyOf(modes);
    }

    @Override
    boolean match(TripPattern pattern) {
      return modes.contains(pattern.getMode());
    }
  }

  private static final class RegExpMatcher extends PriorityGroupMatcher {

    private final Pattern[] subModeRegexp;
    private final Function<TripPattern, String> toValue;

    public RegExpMatcher(List<String> subModeRegexp, Function<TripPattern, String> toValue) {
      this.subModeRegexp = subModeRegexp.stream().map(Pattern::compile).toArray(Pattern[]::new);
      this.toValue = toValue;
    }

    @Override
    boolean match(TripPattern pattern) {
      var value = toValue.apply(pattern);
      for (Pattern p : subModeRegexp) {
        if (p.matcher(value).matches()) {
          return true;
        }
      }
      return false;
    }
  }

  private static final class IdMatcher extends PriorityGroupMatcher {

    private final Set<FeedScopedId> ids;
    private final Function<TripPattern, FeedScopedId> idProvider;

    public IdMatcher(List<FeedScopedId> ids, Function<TripPattern, FeedScopedId> idProvider) {
      this.ids = new HashSet<>(ids);
      this.idProvider = idProvider;
    }

    @Override
    boolean match(TripPattern pattern) {
      return ids.contains(idProvider.apply(pattern));
    }
  }

  /**
   * Take a list of matchers and provide a single interface. At least one matcher in the
   * list must match for the composite mather to return a match.
   */
  private static final class CompositeMatcher extends PriorityGroupMatcher {

    private final PriorityGroupMatcher[] matchers;

    public CompositeMatcher(List<PriorityGroupMatcher> matchers) {
      this.matchers = matchers.toArray(PriorityGroupMatcher[]::new);
    }

    @Override
    boolean match(TripPattern pattern) {
      for (var m : matchers) {
        if (m.match(pattern)) {
          return true;
        }
      }
      return false;
    }
  }
}
