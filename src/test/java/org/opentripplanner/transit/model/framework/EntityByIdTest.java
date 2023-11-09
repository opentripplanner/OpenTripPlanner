package org.opentripplanner.transit.model.framework;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import javax.annotation.Nonnull;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model._data.TransitModelForTest;

public class EntityByIdTest {

  private static final FeedScopedId ID = TransitModelForTest.id("99");
  private static final TestEntity E = TestEntity.of(ID).build();
  private static final String E_TO_STRING = E.toString();
  private static final String LIST_OF_E_TO_STRING = String.format("[%s]", E_TO_STRING);
  private static final String MAP_OF_E_TO_STRING = String.format("{%s=%s}", ID, E_TO_STRING);
  private static final FeedScopedId FAKE_ID = TransitModelForTest.id("77");
  private final EntityById<TestEntity> subject = new DefaultEntityById<>();

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
  public void size() {
    assertEquals(0, subject.size());
    subject.add(E);
    assertEquals(1, subject.size());
  }

  @Test
  public void isEmpty() {
    assertTrue(subject.isEmpty());
    subject.add(E);
    assertFalse(subject.isEmpty());
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

  private static class TestEntity extends AbstractTransitEntity<TestEntity, TestEntityBuilder> {

    TestEntity(FeedScopedId id) {
      super(id);
    }

    public static TestEntityBuilder of(FeedScopedId id) {
      return new TestEntityBuilder(id);
    }

    @Override
    public boolean sameAs(@Nonnull TestEntity other) {
      return getId().equals(other.getId());
    }

    @Nonnull
    @Override
    public TransitBuilder<TestEntity, TestEntityBuilder> copy() {
      return new TestEntityBuilder(this);
    }
  }

  private static class TestEntityBuilder
    extends AbstractEntityBuilder<TestEntity, TestEntityBuilder> {

    TestEntityBuilder(TestEntity testEntity) {
      super(testEntity);
    }

    TestEntityBuilder(FeedScopedId id) {
      super(id);
    }

    @Override
    protected TestEntity buildFromValues() {
      return new TestEntity(this.getId());
    }
  }
}
