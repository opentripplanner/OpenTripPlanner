package org.opentripplanner.transit.transfer.regular.internal;

import static com.google.common.truth.Truth.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.basic.Reluctance;
import org.opentripplanner.transit.transfer.regular.AlternativePath;
import org.opentripplanner.transit.transfer.regular.api.preferences.WalkPreferences;

class OpenTemplatePathsTest {

  private static final WalkPreferences PROFILE = WalkPreferences.of()
    .withReluctance(Reluctance.of(1.0))
    .build();
  private static final int A = 0;
  private static final int B = 1;
  private static final int C = 2;

  /** A -> B depends on e1 and has alternatives, A -> C depends on nothing. */
  private final OverlaidTemplatePaths<String> paths = paths();

  @Test
  void theStoredPathsAreUsedIfNothingIsClosed() {
    var subject = new OpenTemplatePaths<>(paths);

    assertThat(subject.findPath(PROFILE, A, B)).isEqualTo("best");
    assertThat(subject.pathsFrom(PROFILE, A)).containsExactly(B, "best", C, "plain");
    assertThat(forEachPath(subject)).containsExactly("0->1", "best", "0->2", "plain");
  }

  @Test
  void theFirstAlternativeWithoutAClosedIdIsUsed() {
    var subject = new OpenTemplatePaths<>(paths, Set.of("e1"));

    assertThat(subject.findPath(PROFILE, A, B)).isEqualTo("usesE2");
    assertThat(subject.pathsFrom(PROFILE, A)).containsExactly(B, "usesE2", C, "plain");
    assertThat(forEachPath(subject)).containsExactly("0->1", "usesE2", "0->2", "plain");
    assertThat(
      new OpenTemplatePaths<>(paths, Set.of("e1", "e2")).findPath(PROFILE, A, B)
    ).isEqualTo("detour");
  }

  @Test
  void thereIsNoPathIfEveryPathDependsOnAClosedId() {
    var subject = new OpenTemplatePaths<>(paths, Set.of("e1", "e2", "e3"));

    assertThat(subject.findPath(PROFILE, A, B)).isNull();
    assertThat(subject.pathsFrom(PROFILE, A)).containsExactly(C, "plain");
    assertThat(forEachPath(subject)).containsExactly("0->2", "plain");
  }

  @Test
  void theStoredPathIsUsedIfTheClosedIdIsNotOnThePair() {
    var subject = new OpenTemplatePaths<>(paths, Set.of("e2", "other"));

    assertThat(subject.findPath(PROFILE, A, B)).isEqualTo("best");
    assertThat(subject.pathsFrom(PROFILE, A)).containsExactly(B, "best", C, "plain");
  }

  @Test
  void theReplacedPathsAreUsedAsTheyAreWhateverIsClosed() {
    var overlaid = paths.copy();
    overlaid.replace(PROFILE, A, Map.of(B, "replaced"));
    var subject = new OpenTemplatePaths<>(overlaid, Set.of("e1", "e2", "e3"));

    assertThat(subject.findPath(PROFILE, A, B)).isEqualTo("replaced");
    assertThat(subject.pathsFrom(PROFILE, A)).containsExactly(B, "replaced");
    assertThat(forEachPath(subject)).containsExactly("0->1", "replaced");
  }

  @Test
  void thereIsNoPathWithoutAProfileOrAStoredPath() {
    var subject = new OpenTemplatePaths<>(paths, Set.of("e1"));

    assertThat(subject.findPath(null, A, B)).isNull();
    assertThat(subject.findPath(PROFILE, B, A)).isNull();
  }

  private static Map<String, String> forEachPath(OpenTemplatePaths<String> subject) {
    var result = new HashMap<String, String>();
    subject.forEachPath(PROFILE, (from, to, path) -> result.put(from + "->" + to, path));
    return result;
  }

  private static OverlaidTemplatePaths<String> paths() {
    var paths = new TemplatePathsByUserProfile<String>();
    paths.setPaths(PROFILE, A, Map.of(B, "best", C, "plain"));
    paths.setAlternativePaths(
      PROFILE,
      A,
      Map.of(
        B,
        List.of(
          new AlternativePath<>("best", Set.of("e1")),
          new AlternativePath<>("usesE2", Set.of("e2")),
          new AlternativePath<>("detour", Set.of("e3"))
        )
      )
    );
    return new OverlaidTemplatePaths<>(paths);
  }
}
