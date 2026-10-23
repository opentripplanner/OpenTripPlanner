package org.opentripplanner.utils.collection.trie;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;

class ImmutableTrieMapTest {

  @Test
  void empty() {
    ImmutableTrieMap<String, Integer> subject = ImmutableTrieMap.empty();

    assertThat(subject).isEmpty();
    assertEquals(0, subject.size());
    assertNull(subject.get("A"));
    assertThat(subject.entrySet()).isEmpty();
  }

  @Test
  void with() {
    var subject = ImmutableTrieMap.<String, Integer>empty().with("A", 1).with("B", 2);

    assertThat(subject).containsExactly("A", 1, "B", 2);
    assertEquals(2, subject.size());
    assertEquals(1, subject.get("A"));
    assertThat(subject.containsKey("B")).isTrue();
    assertThat(subject.containsKey("C")).isFalse();
  }

  @Test
  void withReplacesExistingValue() {
    var subject = ImmutableTrieMap.<String, Integer>empty().with("A", 1).with("A", 2);

    assertThat(subject).containsExactly("A", 2);
  }

  @Test
  void withSameValueReturnsSameMap() {
    Integer value = 1000;
    var subject = ImmutableTrieMap.<String, Integer>empty().with("A", value);

    assertSame(subject, subject.with("A", value));
  }

  @Test
  void without() {
    var subject = ImmutableTrieMap.<String, Integer>empty().with("A", 1).with("B", 2);

    assertThat(subject.without("A")).containsExactly("B", 2);
    assertSame(subject, subject.without("C"));
  }

  @Test
  void previousVersionsAreUnchanged() {
    var v1 = ImmutableTrieMap.<String, Integer>empty().with("A", 1);
    var v2 = v1.with("B", 2);
    var v3 = v2.with("A", 3).without("B");

    assertThat(v1).containsExactly("A", 1);
    assertThat(v2).containsExactly("A", 1, "B", 2);
    assertThat(v3).containsExactly("A", 3);
  }

  @Test
  void hashCollisions() {
    var a = new SameHash("A");
    var b = new SameHash("B");
    var c = new SameHash("C");

    var subject = ImmutableTrieMap.<SameHash, Integer>empty().with(a, 1).with(b, 2).with(c, 3);
    assertThat(subject).containsExactly(a, 1, b, 2, c, 3);
    assertEquals(3, subject.size());

    subject = subject.with(b, 20);
    assertThat(subject).containsExactly(a, 1, b, 20, c, 3);

    subject = subject.without(b);
    assertThat(subject).containsExactly(a, 1, c, 3);
    assertEquals(2, subject.size());

    subject = subject.without(a).without(c);
    assertThat(subject).isEmpty();
  }

  @Test
  void equalsAndHashCodeFollowTheMapContract() {
    var subject = ImmutableTrieMap.<String, Integer>empty().with("A", 1).with("B", 2);
    var expected = Map.of("A", 1, "B", 2);

    assertEquals(expected, subject);
    assertEquals(subject, expected);
    assertEquals(expected.hashCode(), subject.hashCode());
  }

  @Test
  void mutationThroughTheMapInterfaceIsNotSupported() {
    var subject = ImmutableTrieMap.<String, Integer>empty().with("A", 1);

    assertThrows(UnsupportedOperationException.class, () -> subject.put("B", 2));
    assertThrows(UnsupportedOperationException.class, () -> subject.remove("A"));
    assertThrows(UnsupportedOperationException.class, subject::clear);
    assertThrows(UnsupportedOperationException.class, () ->
      subject.entrySet().iterator().next().setValue(2)
    );
  }

  @Test
  void nullKeysAndValuesAreNotAllowed() {
    ImmutableTrieMap<String, Integer> subject = ImmutableTrieMap.empty();

    assertThrows(NullPointerException.class, () -> subject.with(null, 1));
    assertThrows(NullPointerException.class, () -> subject.with("A", null));
  }

  /**
   * Apply a random sequence of operations to an {@link ImmutableTrieMap} and a {@link HashMap},
   * and check that they stay equal. The keys are drawn from a small range so that both replacing
   * and removing existing keys are exercised.
   */
  @Test
  void behavesLikeHashMap() {
    var random = new Random(42);
    var expected = new HashMap<Integer, Integer>();
    ImmutableTrieMap<Integer, Integer> subject = ImmutableTrieMap.empty();
    List<ImmutableTrieMap<Integer, Integer>> versions = new ArrayList<>();
    List<Map<Integer, Integer>> expectedVersions = new ArrayList<>();

    for (int i = 0; i < 50_000; ++i) {
      int key = random.nextInt(5_000);
      if (random.nextInt(3) == 0) {
        expected.remove(key);
        subject = subject.without(key);
      } else {
        expected.put(key, i);
        subject = subject.with(key, i);
      }
      if (i % 5_000 == 0) {
        versions.add(subject);
        expectedVersions.add(Map.copyOf(expected));
      }
    }

    assertEquals(expected, subject);
    assertEquals(expected.size(), subject.size());

    var iterated = new HashMap<Integer, Integer>();
    subject.forEach(iterated::put);
    assertEquals(expected, iterated);

    assertEquals(expectedVersions, versions);
  }

  @Test
  void of() {
    var source = Map.of("A", 1, "B", 2);
    var subject = ImmutableTrieMap.of(source);

    assertEquals(source, subject);
    assertEquals(2, subject.size());
    assertSame(subject, ImmutableTrieMap.of(subject));
    assertSame(ImmutableTrieMap.empty(), ImmutableTrieMap.of(Map.of()));
  }

  @Test
  void ofHashCollisions() {
    var a = new SameHash("A");
    var b = new SameHash("B");
    var c = new SameHash("C");
    var source = Map.of(a, 1, b, 2, c, 3, new SameHash("D"), 4);

    var subject = ImmutableTrieMap.of(source);
    assertEquals(source, subject);

    subject = subject.with(b, 20).without(c);
    assertThat(subject).containsExactly(a, 1, b, 20, new SameHash("D"), 4);
  }

  @Test
  void ofDoesNotAllowNullValues() {
    var source = new HashMap<String, Integer>();
    source.put("A", null);

    assertThrows(NullPointerException.class, () -> ImmutableTrieMap.of(source));
  }

  @Test
  void ofDoesNotAllowDuplicateKeys() {
    var source = new IdentityHashMap<String, Integer>();
    source.put(new String("A"), 1);
    source.put(new String("A"), 2);

    assertThrows(IllegalArgumentException.class, () -> ImmutableTrieMap.of(source));
  }

  /**
   * Copy a large map, then apply a random sequence of operations to the copy and to a
   * {@link HashMap}, and check that they stay equal. This verifies that the copy can be changed
   * like a map built one mapping at a time.
   */
  @Test
  void ofLargeMapBehavesLikeHashMap() {
    var random = new Random(42);
    var expected = new HashMap<Integer, Integer>();
    for (int i = 0; i < 100_000; ++i) {
      expected.put(random.nextInt(), i);
    }

    var subject = ImmutableTrieMap.of(expected);

    assertEquals(expected, subject);
    assertEquals(expected.size(), subject.size());
    for (var e : expected.entrySet()) {
      assertEquals(e.getValue(), subject.get(e.getKey()));
    }

    var keys = new ArrayList<>(expected.keySet());
    for (int i = 0; i < 50_000; ++i) {
      int key = random.nextBoolean() ? keys.get(random.nextInt(keys.size())) : random.nextInt();
      if (random.nextBoolean()) {
        expected.remove(key);
        subject = subject.without(key);
      } else {
        expected.put(key, -i);
        subject = subject.with(key, -i);
      }
    }
    assertEquals(expected, subject);
    assertEquals(expected.size(), subject.size());
  }

  @Test
  void serialization() throws Exception {
    var expected = new HashMap<String, Integer>();
    for (int i = 0; i < 1_000; ++i) {
      expected.put("K" + i, i);
    }
    var subject = roundTrip(ImmutableTrieMap.of(expected));

    assertEquals(expected, subject);
    assertEquals(expected.size(), subject.size());

    var changed = subject.with("K1", -1).without("K2");
    expected.put("K1", -1);
    expected.remove("K2");
    assertEquals(expected, changed);
  }

  @Test
  void serializationOfTheEmptyMapReturnsTheEmptyMap() throws Exception {
    assertSame(ImmutableTrieMap.empty(), roundTrip(ImmutableTrieMap.empty()));
  }

  @SuppressWarnings("unchecked")
  private static <T> T roundTrip(T object) throws IOException, ClassNotFoundException {
    var bytes = new ByteArrayOutputStream();
    try (var out = new ObjectOutputStream(bytes)) {
      out.writeObject(object);
    }
    try (var in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
      return (T) in.readObject();
    }
  }

  /** A key where every instance has the same hash code. */
  private record SameHash(String value) {
    @Override
    public int hashCode() {
      return 7;
    }
  }
}
