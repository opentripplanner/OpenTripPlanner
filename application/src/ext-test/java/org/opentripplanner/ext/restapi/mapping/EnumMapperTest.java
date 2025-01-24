package org.opentripplanner.ext.restapi.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.restapi.model.ApiVertexType;
import org.opentripplanner.model.plan.VertexType;

public class EnumMapperTest {

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
}
