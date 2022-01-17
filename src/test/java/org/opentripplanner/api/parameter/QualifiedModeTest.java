import org.junit.Assert;
import org.junit.Test;
import org.opentripplanner.api.parameter.ApiRequestMode;
import org.opentripplanner.api.parameter.QualifiedMode;
import org.opentripplanner.api.parameter.Qualifier;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class QualifiedModeTest {

  @Test
  public void test() {
    Set<Qualifier> ALL_QUALIFIERS = Arrays.stream(Qualifier.values()).collect(Collectors.toSet());
    String ALL_QUALIFIERS_STR = ALL_QUALIFIERS.stream().map(Enum::name).reduce((i,j) -> i + "_" + j).orElse("X");

    for (ApiRequestMode mode : ApiRequestMode.values()) {
      assertEquals(new QualifiedMode(mode.name()), mode, Set.of());
      assertEquals(new QualifiedMode(mode + "_RENT"), mode, Set.of(Qualifier.RENT));
      assertEquals(new QualifiedMode(mode + "_" + ALL_QUALIFIERS_STR), mode, ALL_QUALIFIERS);
    }
  }

  private void assertEquals(QualifiedMode qMode, ApiRequestMode mode, Set<Qualifier> qualifiers) {
    Assert.assertEquals(qMode.mode, mode);
    Assert.assertEquals(qMode.qualifiers, qualifiers);
  }
}