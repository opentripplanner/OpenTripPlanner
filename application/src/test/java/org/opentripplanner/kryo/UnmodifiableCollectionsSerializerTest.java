package org.opentripplanner.kryo;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

class UnmodifiableCollectionsSerializerTest {

  private final Kryo kryo = newKryo();

  @Test
  void roundTripUnmodifiableCollection() {
    var original = Collections.unmodifiableCollection(Arrays.asList("a", "b", "c"));
    var result = roundTrip(original);
    assertThat(result).containsExactly("a", "b", "c");
    assertThrows(UnsupportedOperationException.class, () -> result.add("d"));
  }

  @Test
  void roundTripUnmodifiableRandomAccessList() {
    var original = Collections.unmodifiableList(new ArrayList<>(List.of("a", "b", "c")));
    var result = roundTrip(original);
    assertThat(result).containsExactly("a", "b", "c").inOrder();
    assertThrows(UnsupportedOperationException.class, () -> result.add("d"));
  }

  @Test
  void roundTripUnmodifiableList() {
    var original = Collections.unmodifiableList(new LinkedList<>(List.of("a", "b", "c")));
    var result = roundTrip(original);
    assertThat(result).containsExactly("a", "b", "c").inOrder();
    assertThrows(UnsupportedOperationException.class, () -> result.add("d"));
  }

  @Test
  void roundTripUnmodifiableSet() {
    var original = Collections.unmodifiableSet(new HashSet<>(Set.of("a", "b", "c")));
    var result = roundTrip(original);
    assertThat(result).containsExactly("a", "b", "c");
    assertThrows(UnsupportedOperationException.class, () -> result.add("d"));
  }

  @Test
  void roundTripUnmodifiableSortedSet() {
    var original = Collections.unmodifiableSortedSet(new TreeSet<>(Set.of("c", "a", "b")));
    var result = roundTrip(original);
    assertThat(new ArrayList<>(result)).containsExactly("a", "b", "c").inOrder();
    assertThrows(UnsupportedOperationException.class, () -> result.add("d"));
  }

  @Test
  void writeRejectsSortedSetWithCustomComparator() {
    var original = Collections.unmodifiableSortedSet(
      new TreeSet<>(Comparator.<String>reverseOrder())
    );
    assertThrows(UnsupportedOperationException.class, () -> roundTrip(original));
  }

  @Test
  void roundTripUnmodifiableMap() {
    var original = Collections.unmodifiableMap(new HashMap<>(Map.of("a", 1, "b", 2)));
    var result = roundTrip(original);
    assertThat(result).containsExactly("a", 1, "b", 2);
    assertThrows(UnsupportedOperationException.class, () -> result.put("c", 3));
  }

  @Test
  void roundTripUnmodifiableSortedMap() {
    var source = new TreeMap<String, Integer>();
    source.put("c", 3);
    source.put("a", 1);
    source.put("b", 2);
    var original = Collections.unmodifiableSortedMap(source);
    var result = roundTrip(original);
    assertThat(new ArrayList<>(result.keySet())).containsExactly("a", "b", "c").inOrder();
    assertThat(result).containsExactly("a", 1, "b", 2, "c", 3);
    assertThrows(UnsupportedOperationException.class, () -> result.put("d", 4));
  }

  @Test
  void writeRejectsSortedMapWithCustomComparator() {
    var original = Collections.unmodifiableSortedMap(
      new TreeMap<String, Integer>(Comparator.<String>reverseOrder())
    );
    assertThrows(UnsupportedOperationException.class, () -> roundTrip(original));
  }

  @Test
  void copyPreservesUnmodifiabilityAndContents() {
    Map<String, Integer> original = Collections.unmodifiableMap(new HashMap<>(Map.of("a", 1)));
    Map<String, Integer> copy = kryo.copy(original);
    assertThat(copy).containsExactly("a", 1);
    assertThrows(UnsupportedOperationException.class, () -> copy.put("b", 2));
  }

  @Test
  void writeRejectsUnsupportedType() {
    var serializer = new UnmodifiableCollectionsSerializer();
    try (Output output = new Output(64, -1)) {
      assertThrows(IllegalArgumentException.class, () ->
        serializer.write(kryo, output, "not a wrapped collection")
      );
    }
  }

  private static Kryo newKryo() {
    Kryo kryo = new Kryo();
    kryo.setRegistrationRequired(false);
    UnmodifiableCollectionsSerializer.registerSerializers(kryo);
    return kryo;
  }

  @SuppressWarnings("unchecked")
  private <T> T roundTrip(T original) {
    try (Output output = new Output(256, -1)) {
      kryo.writeClassAndObject(output, original);
      output.flush();
      try (Input input = new Input(output.toBytes())) {
        return (T) kryo.readClassAndObject(input);
      }
    }
  }
}
