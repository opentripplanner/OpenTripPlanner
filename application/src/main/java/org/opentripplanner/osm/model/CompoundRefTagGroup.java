package org.opentripplanner.osm.model;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Represents a group of OpenStreetMap tags that together form a compound reference identifier for an entity.
 * <p>
 * Tags provided at construction time are sanitized by stripping surrounding whitespace and
 * filtering out null or blank values. The group can then produce a single compound value by
 * mapping each tag to its corresponding value and joining the results with a colon delimiter.
 * <p>
 * A compound value is only produced when all tags in the group resolve to a non-null value.
 * If any tag cannot be resolved, the compound value is considered incomplete and absent.
 * <p>
 * Instances are created via the factory method {@link #of(String...)}.
 */
public class CompoundRefTagGroup {

  private static final String DELIMITER = ":";

  private final List<String> tags;

  private CompoundRefTagGroup(String... tags) {
    this.tags = Arrays.stream(tags)
      .filter(Objects::nonNull)
      .map(String::strip)
      .filter(s -> !s.isEmpty())
      .toList();
  }

  public static CompoundRefTagGroup of(String... tags) {
    return new CompoundRefTagGroup(tags);
  }

  /**
   * Produces a compound value by mapping each tag in this group to its corresponding value
   * using the provided mapper function, then joining the results with a colon delimiter.
   *
   * The compound value is only produced when all tags in the group resolve to a non-null value
   * via the mapper. If any tag maps to null, the result is considered incomplete and an empty
   * Optional is returned. Each resolved value is stripped of surrounding whitespace before joining.
   *
   * @param tagToValueMapper a function that maps an OpenStreetMap tag name to its corresponding
   *                         value, or null if the tag cannot be resolved
   * @return an Optional containing the colon-delimited compound value if all tags resolved to
   *         non-null values, or an empty Optional if any tag could not be resolved
   */
  public Optional<String> compoundValue(Function<String, String> tagToValueMapper) {
    List<String> values = tags.stream().map(tagToValueMapper).toList();
    if (values.stream().anyMatch(Objects::isNull)) {
      return Optional.empty();
    } else {
      return Optional.of(values.stream().map(String::strip).collect(Collectors.joining(DELIMITER)));
    }
  }

  @Override
  public String toString() {
    return "CompoundRefTagGroup{" + "tags=" + tags + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CompoundRefTagGroup that = (CompoundRefTagGroup) o;
    return tags.equals(that.tags);
  }

  @Override
  public int hashCode() {
    return Objects.hash(tags);
  }
}
