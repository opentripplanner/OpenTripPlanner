package org.opentripplanner.ext.restapi.parameter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.opentripplanner.api.parameter.ApiRequestMode;
import org.opentripplanner.api.parameter.QualifiedMode;
import org.opentripplanner.api.parameter.Qualifier;

public class QualifiedModeTest {

  @Test
  public void test() {
    Set<Qualifier> ALL_QUALIFIERS = Arrays.stream(Qualifier.values()).collect(Collectors.toSet());
    String ALL_QUALIFIERS_STR = ALL_QUALIFIERS.stream()
      .map(Enum::name)
      .reduce((i, j) -> i + "_" + j)
      .orElse("X");

    for (ApiRequestMode mode : ApiRequestMode.values()) {
      assertModeEquals(new QualifiedMode(mode.name()), mode, Set.of());
      assertModeEquals(new QualifiedMode(mode + "_RENT"), mode, Set.of(Qualifier.RENT));
      assertModeEquals(new QualifiedMode(mode + "_" + ALL_QUALIFIERS_STR), mode, ALL_QUALIFIERS);
    }
  }

  private void assertModeEquals(
    QualifiedMode qMode,
    ApiRequestMode mode,
    Set<Qualifier> qualifiers
  ) {
    assertEquals(qMode.mode, mode);
    assertEquals(qMode.qualifiers, qualifiers);
  }
}
