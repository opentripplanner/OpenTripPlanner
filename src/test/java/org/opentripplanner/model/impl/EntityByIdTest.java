package org.opentripplanner.model.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import org.junit.Test;
import org.opentripplanner.model.TransitEntity;
import org.opentripplanner.transit.model._data.TransitModelForTest;
import org.opentripplanner.transit.model.basic.FeedScopedId;

public class EntityByIdTest {

  private static final FeedScopedId ID = TransitModelForTest.id("99");
  private static final EntityByIdTest.E E = new E(ID);
  private static final String E_TO_STRING = E.toString();
  private static final String LIST_OF_E_TO_STRING = String.format("[%s]", E_TO_STRING);
  private static final String MAP_OF_E_TO_STRING = String.format("{%s=%s}", ID, E_TO_STRING);
  private static final FeedScopedId FAKE_ID = TransitModelForTest.id("77");
  private final EntityById<E> subject = new EntityById<>();

  @Test
  public void add() {
    subject.add(E);
    assertEquals(MAP_OF_E_TO_STRING, subject.toString());
  }

  @Test
  public void addAll() {
    subject.addAll(Collections.emptyList());
    assertEquals("{}", subject.toString());
    subject.addAll(Collections.singletonList(E));
    assertEquals(MAP_OF_E_TO_STRING, subject.toString());
  }

  @Test
  public void values() {
    subject.add(E);
    assertEquals(LIST_OF_E_TO_STRING, subject.values().toString());
  }

  @Test
  public void get() {
    subject.add(E);
    assertEquals(E_TO_STRING, subject.get(ID).toString());
  }

  @Test
  public void asImmutableMap() {
    subject.add(E);
    assertEquals(E_TO_STRING, subject.get(ID).toString());
  }

  @Test
  public void containsKey() {
    assertFalse(subject.containsKey(ID));
    subject.add(E);
    assertTrue(subject.containsKey(ID));
    assertFalse(subject.containsKey(FAKE_ID));
  }

  static class E extends TransitEntity {

    E(FeedScopedId id) {
      super(id);
    }

    @Override
    public String toString() {
      return "E-" + getId();
    }
  }
}
