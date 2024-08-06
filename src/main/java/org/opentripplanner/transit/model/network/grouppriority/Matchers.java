package org.opentripplanner.transit.model.network.grouppriority;

import static org.opentripplanner.transit.model.network.grouppriority.BinarySetOperator.AND;
import static org.opentripplanner.transit.model.network.grouppriority.BinarySetOperator.OR;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.opentripplanner.routing.api.request.request.filter.TransitGroupSelect;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.FeedScopedId;

/**
 * This class turns a {@link TransitGroupSelect} into a matcher.
 * <p>
 * Design: It uses the composite design pattern. A matcher is created for each
 * value in the "select", then the list of non-empty matchers is merged into
 * a `CompositeMatcher`. So, a new matcher is only created if the field in the
 * select is present.
 */
final class Matchers {

  private static final Matcher NOOP = new EmptyMatcher();

  static Matcher of(TransitGroupSelect select) {
    if (select.isEmpty()) {
      return NOOP;
    }
    List<Matcher> list = new ArrayList<>();

    if (!select.modes().isEmpty()) {
      list.add(new ModeMatcher(select.modes()));
    }
    if (!select.subModeRegexp().isEmpty()) {
      list.add(new RegExpMatcher("SubMode", select.subModeRegexp(), EntityAdapter::subMode));
    }
    if (!select.agencyIds().isEmpty()) {
      list.add(new IdMatcher("Agency", select.agencyIds(), EntityAdapter::agencyId));
    }
    if (!select.routeIds().isEmpty()) {
      list.add(new IdMatcher("Route", select.routeIds(), EntityAdapter::routeId));
    }
    return andOf(list);
  }

  @SuppressWarnings("unchecked")
  static Matcher[] of(Collection<TransitGroupSelect> selectors) {
    return selectors
      .stream()
      .map(Matchers::of)
      .filter(Predicate.not(Matcher::isEmpty))
      .toArray(Matcher[]::new);
  }

  private static <T> String arrayToString(BinarySetOperator op, T[] values) {
    return colToString(op, Arrays.asList(values));
  }

  private static <T> String colToString(BinarySetOperator op, Collection<T> values) {
    return values.stream().map(Objects::toString).collect(Collectors.joining(" " + op + " "));
  }

  private static Matcher andOf(List<Matcher> list) {
    // Remove empty/noop matchers
    list = list.stream().filter(Predicate.not(Matcher::isEmpty)).toList();

    if (list.isEmpty()) {
      return NOOP;
    }
    if (list.size() == 1) {
      return list.get(0);
    }
    return new AndMatcher(list);
  }

  private static final class EmptyMatcher implements Matcher {

    @Override
    public boolean match(EntityAdapter entity) {
      return false;
    }

    @Override
    public boolean isEmpty() {
      return true;
    }

    @Override
    public String toString() {
      return "Empty";
    }
  }

  private static final class ModeMatcher implements Matcher {

    private final Set<TransitMode> modes;

    public ModeMatcher(List<TransitMode> modes) {
      this.modes = EnumSet.copyOf(modes);
    }

    @Override
    public boolean match(EntityAdapter entity) {
      return modes.contains(entity.mode());
    }

    @Override
    public String toString() {
      return "Mode(" + colToString(OR, modes) + ')';
    }
  }

  private static final class RegExpMatcher implements Matcher {

    private final String typeName;
    private final Pattern[] patterns;
    private final Function<EntityAdapter, String> toValue;

    public RegExpMatcher(
      String typeName,
      List<String> regexps,
      Function<EntityAdapter, String> toValue
    ) {
      this.typeName = typeName;
      this.patterns = regexps.stream().map(Pattern::compile).toArray(Pattern[]::new);
      this.toValue = toValue;
    }

    @Override
    public boolean match(EntityAdapter entity) {
      var value = toValue.apply(entity);
      for (Pattern p : patterns) {
        if (p.matcher(value).matches()) {
          return true;
        }
      }
      return false;
    }

    @Override
    public String toString() {
      return typeName + "Regexp(" + arrayToString(OR, patterns) + ')';
    }
  }

  private static final class IdMatcher implements Matcher {

    private final String typeName;
    private final Set<FeedScopedId> ids;
    private final Function<EntityAdapter, FeedScopedId> idProvider;

    public IdMatcher(
      String typeName,
      List<FeedScopedId> ids,
      Function<EntityAdapter, FeedScopedId> idProvider
    ) {
      this.typeName = typeName;
      this.ids = new HashSet<>(ids);
      this.idProvider = idProvider;
    }

    @Override
    public boolean match(EntityAdapter entity) {
      return ids.contains(idProvider.apply(entity));
    }

    @Override
    public String toString() {
      return typeName + "Id(" + colToString(OR, ids) + ')';
    }
  }

  /**
   * Takes a list of matchers and provide a single interface. All matchers in the list must match
   * for the composite matcher to return a match.
   */
  private static final class AndMatcher implements Matcher {

    private final Matcher[] matchers;

    public AndMatcher(List<Matcher> matchers) {
      this.matchers = matchers.toArray(Matcher[]::new);
    }

    @Override
    public boolean match(EntityAdapter entity) {
      for (var m : matchers) {
        if (!m.match(entity)) {
          return false;
        }
      }
      return true;
    }

    @Override
    public String toString() {
      return "(" + arrayToString(AND, matchers) + ')';
    }
  }
}
