package org.opentripplanner.apis.transmodel.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.opentripplanner.apis.transmodel.model.EnumTypes.LEG_MODE;
import static org.opentripplanner.apis.transmodel.model.EnumTypes.RELATIVE_DIRECTION;
import static org.opentripplanner.apis.transmodel.model.EnumTypes.ROUTING_ERROR_CODE;
import static org.opentripplanner.apis.transmodel.model.EnumTypes.TRANSPORT_MODE;
import static org.opentripplanner.apis.transmodel.model.EnumTypes.map;

import graphql.GraphQLContext;
import graphql.schema.GraphQLEnumType;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.opentripplanner.apis.transmodel.mapping.RelativeDirectionMapper;
import org.opentripplanner.core.model.doc.DocumentedEnum;
import org.opentripplanner.model.plan.walkstep.RelativeDirection;
import org.opentripplanner.routing.api.response.RoutingErrorCode;
import org.opentripplanner.transit.model.basic.TransitMode;

class EnumTypesTest {

  private static final String HI_DESCRIPTION = "Hi description";
  private static final String BAR_DESCRIPTION = "Bar description";
  private static final String FOO_TYPE_DESCRIPTION = "Foo description";

  @Test
  void createEnum() {
    var res = EnumTypes.createEnum("oof", Foo.values(), it -> it.name().toUpperCase());
    assertEquals("oof", res.getName());
    var bar = res.getValue("BAR");
    assertEquals("BAR", bar.getName());
  }

  @Test
  void createFromDocumentedEnum() {
    var res = EnumTypes.createFromDocumentedEnum(
      "oof",
      List.of(EnumTypes.map("Ih", Foo.Hi), EnumTypes.map("Rab", Foo.Bar))
    );

    assertEquals("oof", res.getName());
    assertEquals(FOO_TYPE_DESCRIPTION, res.getDescription());

    var bar = res.getValue("Rab");
    assertEquals("Rab", bar.getName());
    assertEquals(BAR_DESCRIPTION, bar.getDescription());

    var hi = res.getValue("Ih");
    assertEquals("Ih", hi.getName());
    assertEquals(HI_DESCRIPTION, hi.getDescription());
  }

  @Test
  void createFromDocumentedEnumMissingValueThrowsException() {
    assertThrows(IllegalStateException.class, () ->
      EnumTypes.createFromDocumentedEnum("oof", List.of(EnumTypes.map("Rab", Foo.Bar)))
    );
  }

  @Test
  void createFromDocumentedEnumDuplicateThrowsException() {
    assertThrows(IllegalStateException.class, () ->
      EnumTypes.createFromDocumentedEnum(
        "oof",
        List.of(
          EnumTypes.map("Rab", Foo.Bar),
          EnumTypes.map("Hi", Foo.Hi),
          // Duplicate: Bar -> throw exception
          EnumTypes.map("Bar", Foo.Bar)
        )
      )
    );
  }

  @Test
  void testMap() {
    Object mapping = map("iH", Foo.Hi);
    assertEquals("DocumentedEnumMapping[apiName=iH, internal=Hi]", mapping.toString());
  }

  @ParameterizedTest
  @EnumSource(RelativeDirection.class)
  void serializeRelativeDirection(RelativeDirection direction) {
    var value = RELATIVE_DIRECTION.serialize(
      RelativeDirectionMapper.map(direction),
      GraphQLContext.getDefault(),
      Locale.ENGLISH
    );
    assertInstanceOf(String.class, value);
    assertFalse(((String) value).isEmpty());
  }

  @Test
  void assertAllRoutingErrorCodesAreMapped() {
    var expected = EnumSet.allOf(RoutingErrorCode.class);
    var values = EnumSet.copyOf(
      ROUTING_ERROR_CODE.getValues()
        .stream()
        .map(it -> (RoutingErrorCode) it.getValue())
        .toList()
    );
    assertEquals(expected, values);
  }

  /**
   * Carpool lines reach the API from GTFS route types 1551-1560. Before this mapping existed the
   * mode could not be serialized, and a single carpool line made any query selecting
   * {@code transportMode} fail. The mode enums are not exhaustive over {@link TransitMode}, so this
   * test guards the one mode rather than the whole set.
   */
  @Test
  void assertCarpoolIsMappedInBothModeEnums() {
    assertEquals(TransitMode.CARPOOL, mappedValue(TRANSPORT_MODE, "carpool"));
    assertEquals(TransitMode.CARPOOL, mappedValue(LEG_MODE, "carpool"));
  }

  private static Object mappedValue(GraphQLEnumType type, String apiName) {
    var value = type.getValue(apiName);
    assertNotNull(value, () -> "'%s' is not mapped in enum %s".formatted(apiName, type.getName()));
    return value.getValue();
  }

  private enum Foo implements DocumentedEnum<Foo> {
    Hi(HI_DESCRIPTION),
    Bar(BAR_DESCRIPTION);

    private final String description;

    Foo(String description) {
      this.description = description;
    }

    @Override
    public String typeDescription() {
      return FOO_TYPE_DESCRIPTION;
    }

    @Override
    public String enumValueDescription() {
      return description;
    }
  }
}
