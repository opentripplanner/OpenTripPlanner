package org.opentripplanner.ext.restapi.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.restapi.model.ApiAbsoluteDirection;
import org.opentripplanner.ext.restapi.model.ApiRelativeDirection;
import org.opentripplanner.ext.restapi.model.ApiVertexType;
import org.opentripplanner.model.plan.AbsoluteDirection;
import org.opentripplanner.model.plan.RelativeDirection;
import org.opentripplanner.model.plan.VertexType;

public class EnumMapperTest {

  private static final String MSG =
    "Assert that the API enums have the exact same values that " +
    "the domain enums of the same type, and that the specialized mapper is mapping all " +
    "values. If this assumtion does not hold, create a new test.";

  @Test
  public void map() {
    try {
      verifyExactMatch(
        AbsoluteDirection.class,
        ApiAbsoluteDirection.class,
        AbsoluteDirectionMapper::mapAbsoluteDirection
      );
      verifyExactMatch(
        RelativeDirection.class,
        ApiRelativeDirection.class,
        RelativeDirectionMapper::mapRelativeDirection
      );
    } catch (RuntimeException ex) {
      System.out.println(MSG);
      throw ex;
    }
  }

  @Test
  public void testVertexTypeMapping() {
    verifyExplicitMatch(
      VertexType.class,
      ApiVertexType.class,
      Map.of(
        VertexType.NORMAL,
        ApiVertexType.NORMAL,
        VertexType.TRANSIT,
        ApiVertexType.TRANSIT,
        VertexType.VEHICLEPARKING,
        ApiVertexType.BIKEPARK,
        VertexType.VEHICLERENTAL,
        ApiVertexType.BIKESHARE
      ),
      VertexTypeMapper::mapVertexType
    );
  }

  private <D extends Enum<?>, A extends Enum<?>> void verifyExplicitMatch(
    Class<D> domainClass,
    Class<A> apiClass,
    Map<D, A> mappings,
    Function<D, A> mapper
  ) {
    List<A> rest = new ArrayList<>(List.of(apiClass.getEnumConstants()));
    for (D it : domainClass.getEnumConstants()) {
      A result = mapper.apply(it);
      assertEquals(mappings.get(it), result, "Map " + it);
      rest.remove(result);
    }
    assertTrue(rest.isEmpty());
  }

  private <D extends Enum<?>, A extends Enum<?>> void verifyExactMatch(
    Class<D> domainClass,
    Class<A> apiClass,
    Function<D, A> mapper
  ) {
    List<A> rest = new ArrayList<>(List.of(apiClass.getEnumConstants()));
    for (D it : domainClass.getEnumConstants()) {
      A result = mapper.apply(it);
      assertEquals(result.name(), it.name());
      rest.remove(result);
    }
    assertTrue(rest.isEmpty());
  }
}
