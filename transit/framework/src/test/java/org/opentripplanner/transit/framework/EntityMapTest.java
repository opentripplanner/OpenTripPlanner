package org.opentripplanner.transit.framework;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.id.FeedScopedId;

class EntityMapTest {

  private static final TestEntity A = new TestEntity("A");
  private static final TestEntity B = new TestEntity("B");
  private static final TestEntity C = new TestEntity("C");

  @Test
  void addAndRemove() {
    var subject = new EntityMap<TestEntity>();
    subject.add(A);
    subject.add(B);

    assertEquals(A, subject.get(A.getId()));
    assertThat(subject.containsKey(B.getId())).isTrue();
    assertEquals(2, subject.size());

    subject.remove(A.getId());

    assertNull(subject.get(A.getId()));
    assertThat(subject.values()).containsExactly(B);
  }

  @Test
  void addReplacesEntityWithSameId() {
    var subject = new EntityMap<TestEntity>();
    var replacement = new TestEntity("A");
    subject.add(A);
    subject.add(replacement);

    assertEquals(1, subject.size());
    assertThat(subject.get(A.getId())).isSameInstanceAs(replacement);
  }

  @Test
  void freezeIsNotAffectedByLaterChanges() {
    var subject = new EntityMap<TestEntity>();
    subject.add(A);
    subject.add(B);

    var snapshot = subject.freeze();

    subject.remove(A.getId());
    subject.add(C);

    assertThat(snapshot.values()).containsExactly(A, B);
    assertThat(subject.values()).containsExactly(B, C);
  }

  @Test
  void copyOfIsIndependentOfTheSource() {
    var source = new EntityMap<TestEntity>();
    source.add(A);
    var snapshot = source.freeze();

    var copy = snapshot.copyOf();
    copy.add(B);
    source.add(C);

    assertThat(snapshot.values()).containsExactly(A);
    assertThat(copy.values()).containsExactly(A, B);
    assertThat(source.values()).containsExactly(A, C);
  }

  @Test
  void empty() {
    var subject = ImmutableEntityMap.<TestEntity>empty();

    assertThat(subject.isEmpty()).isTrue();
    assertThat(subject.values()).isEmpty();
  }

  @Test
  void keysValuesIteratorAndStream() {
    var subject = new EntityMap<TestEntity>();
    subject.add(A);
    subject.add(B);

    assertThat(subject.keys()).containsExactly(A.getId(), B.getId());
    assertThat(subject.values()).containsExactly(A, B);
    assertThat(subject).containsExactly(A, B);
    assertThat(subject.stream().toList()).containsExactly(A, B);
  }

  private static final class TestEntity implements TransitEntity {

    private final FeedScopedId id;

    TestEntity(String id) {
      this.id = new FeedScopedId("F", id);
    }

    @Override
    public FeedScopedId getId() {
      return id;
    }
  }
}
