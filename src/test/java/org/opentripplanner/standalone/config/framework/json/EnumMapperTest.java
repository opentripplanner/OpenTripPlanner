package org.opentripplanner.standalone.config.framework.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EnumMapperTest {

  @Test
  void mapToEnum() {
    assertEquals(Foo.Bar, EnumMapper.mapToEnum("BAR", Foo.class).orElseThrow());
    assertEquals(Foo.BOO_BOO, EnumMapper.mapToEnum("boo-boo", Foo.class).orElseThrow());
    assertEquals(Foo.BOO_BOO, EnumMapper.mapToEnum("BOO_BOO", Foo.class).orElseThrow());
    assertTrue(EnumMapper.mapToEnum(null, Foo.class).isEmpty());
    assertTrue(EnumMapper.mapToEnum("not_valid_enum_value", Foo.class).isEmpty());
  }

  @Test
  void mapToEnum2() {
    assertEquals(Foo.Bar, EnumMapper.mapToEnum2("BAR", Foo.class).orElseThrow());
    assertEquals(Foo.BOO_BOO, EnumMapper.mapToEnum2("boo-boo", Foo.class).orElseThrow());
    assertEquals(Foo.BOO_BOO, EnumMapper.mapToEnum2("BOO_BOO", Foo.class).orElseThrow());
    assertTrue(EnumMapper.mapToEnum(null, Foo.class).isEmpty());
    assertTrue(EnumMapper.mapToEnum("not_valid_enum_value", Foo.class).isEmpty());
  }

  @Test
  void testToString() {
    assertEquals("bar", EnumMapper.toString(Foo.Bar));
    assertEquals("boo-boo", EnumMapper.toString(Foo.BOO_BOO));
  }

  enum Foo {
    Bar,
    BOO_BOO,
  }
}
