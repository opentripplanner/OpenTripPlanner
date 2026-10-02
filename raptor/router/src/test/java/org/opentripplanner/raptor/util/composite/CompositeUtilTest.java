package org.opentripplanner.raptor.util.composite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class CompositeUtilTest {

  @Test
  void testOf() {
    TNamed EMPTY = null;
    assertNull(composite(EMPTY));
    assertNull(composite(EMPTY, EMPTY));

    assertEquals("A", composite(tnamed("A")).name());
    assertEquals("A", composite(EMPTY, tnamed("A"), EMPTY).name());
    assertEquals("(A:B)", composite(tnamed("A"), tnamed("B")).name());
    // Nested composites are flattened into one composite
    assertEquals(
      "(A:B:C)",
      composite(composite(tnamed("A")), composite(tnamed("B")), composite(tnamed("C"))).name()
    );
  }

  TNamed composite(TNamed... children) {
    return TNamedComposite.of(children);
  }

  TNamed tnamed(String name) {
    return new DefaultTNamed(name);
  }

  interface TNamed {
    String name();
  }

  static final class DefaultTNamed implements TNamed {

    private final String name;

    public DefaultTNamed(String name) {
      this.name = name;
    }

    @Override
    public String name() {
      return name;
    }
  }

  static final class TNamedComposite implements TNamed {

    private final List<TNamed> children;

    private TNamedComposite(List<TNamed> children) {
      this.children = children;
    }

    static TNamed of(TNamed... children) {
      return CompositeUtil.of(
        TNamedComposite::new,
        TNamedComposite.class::isInstance,
        it -> ((TNamedComposite) it).children,
        children
      );
    }

    @Override
    public String name() {
      return "(" + children.stream().map(TNamed::name).collect(Collectors.joining(":")) + ")";
    }
  }
}
