package org.opentripplanner.openstreetmap.wayproperty.specifier;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.opentripplanner.openstreetmap.model.OSMWithTags;

/**
 * Allows to specify a 'logical or' condition to specify a match. This intended to be used with a
 * safety mixin.
 * <p>
 * For example if you specify ("lcn=yes", "rnc=yes", "ncn=yes") then the specifier will match if one
 * of these tags matches.
 * <p>
 * Background: If you would add 3 separate matches with a {@link BestMatchSpecifier} that would mean
 * that a way that is matched with all of them would receive too high a safety value leading, as the
 * mixin is applied several times.
 * <p>
 * 'Logical or's are only implemented for mixins without wildcards.
 */
public class LogicalOrSpecifier implements OsmSpecifier {

  private final List<ExactMatchSpecifier> subSpecs;

  public LogicalOrSpecifier(ExactMatchSpecifier... specifiers) {
    this.subSpecs = Arrays.asList(specifiers);
  }

  public LogicalOrSpecifier(String... specs) {
    this.subSpecs = Arrays.stream(specs).map(ExactMatchSpecifier::new).toList();
  }

  @Override
  public Scores matchScores(OSMWithTags way) {
    return Scores.of(matchScore(way));
  }

  @Override
  public int matchScore(OSMWithTags way) {
    var oneMatchesExactly = subSpecs.stream().anyMatch(subspec -> subspec.allTagsMatch(way));
    if (oneMatchesExactly) {
      return 1;
    } else {
      return 0;
    }
  }

  @Override
  public String toDocString() {
    return subSpecs.stream().map(ExactMatchSpecifier::toDocString).collect(Collectors.joining("|"));
  }
}
