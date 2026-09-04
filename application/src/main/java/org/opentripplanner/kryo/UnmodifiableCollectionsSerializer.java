/*
 * Copyright 2010 Martin Grotzke
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an &quot;AS IS&quot; BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.opentripplanner.kryo;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * A kryo {@link Serializer} for unmodifiable {@link Collection}s and {@link Map}s created via
 * {@link Collections}.
 *
 * @author <a href="mailto:martin.grotzke@javakaffee.de">Martin Grotzke</a>
 */
public class UnmodifiableCollectionsSerializer extends Serializer<Object> {

  /**
   * Creates a new {@link UnmodifiableCollectionsSerializer} and registers its serializer for the
   * several unmodifiable Collections that can be created via {@link Collections}, including {@link
   * Map}s.
   *
   * @param kryo the {@link Kryo} instance to set the serializer on.
   * @see Collections#unmodifiableCollection(Collection)
   * @see Collections#unmodifiableList(List)
   * @see Collections#unmodifiableSet(Set)
   * @see Collections#unmodifiableSortedSet(SortedSet)
   * @see Collections#unmodifiableMap(Map)
   * @see Collections#unmodifiableSortedMap(SortedMap)
   */
  public static void registerSerializers(Kryo kryo) {
    UnmodifiableCollectionsSerializer serializer = new UnmodifiableCollectionsSerializer();
    UnmodifiableCollection.values();
    for (UnmodifiableCollection item : UnmodifiableCollection.values()) {
      kryo.register(item.type, serializer);
    }
  }

  @Override
  public void write(Kryo kryo, Output output, Object object) {
    try {
      UnmodifiableCollection unmodifiableCollection = UnmodifiableCollection.valueOfType(
        object.getClass()
      );
      // the ordinal could be replaced by s.th. else (e.g. a explicitely managed "id")
      output.writeInt(unmodifiableCollection.ordinal(), true);
      kryo.writeClassAndObject(output, unmodifiableCollection.toKryoSerializedObject(object));
    } catch (RuntimeException e) {
      // Don't eat and wrap RuntimeExceptions because the ObjectBuffer.write...
      // handles SerializationException specifically (resizing the buffer)...
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Object read(Kryo kryo, Input input, Class<?> clazz) {
    int ordinal = input.readInt(true);
    UnmodifiableCollection unmodifiableCollection = UnmodifiableCollection.values()[ordinal];
    Object intermediate = kryo.readClassAndObject(input);
    return unmodifiableCollection.fromKryoSerializedObject(intermediate);
  }

  @Override
  public Object copy(Kryo kryo, Object original) {
    try {
      UnmodifiableCollection unmodifiableCollection = UnmodifiableCollection.valueOfType(
        original.getClass()
      );
      Object intermediateCopy = kryo.copy(unmodifiableCollection.toKryoSerializedObject(original));
      return unmodifiableCollection.fromKryoSerializedObject(intermediateCopy);
    } catch (RuntimeException e) {
      // Don't eat and wrap RuntimeExceptions
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Each variant converts to/from a plain, Kryo-friendly intermediate representation on write/copy
   * ({@link #toKryoSerializedObject}), and reconstructs the concrete collection/map type it needs before
   * wrapping it back up as unmodifiable on read/copy ({@link #fromKryoSerializedObject}). This is deliberately not
   * symmetric (e.g. {@code Map} isn't a {@code Collection}, and the intermediate {@code ArrayList}
   * read back for a {@code Set} isn't itself a {@code Set}), so each side needs its own conversion
   * rather than a single shared cast.
   */
  private enum UnmodifiableCollection {
    COLLECTION(Collections.unmodifiableCollection(Arrays.asList("")).getClass()) {
      @Override
      Object toKryoSerializedObject(Object source) {
        return new ArrayList<>((Collection<?>) source);
      }

      @Override
      public Object fromKryoSerializedObject(Object serializedObject) {
        return Collections.unmodifiableCollection((Collection<?>) serializedObject);
      }
    },
    RANDOM_ACCESS_LIST(Collections.unmodifiableList(new ArrayList<Void>()).getClass()) {
      @Override
      Object toKryoSerializedObject(Object source) {
        return new ArrayList<>((Collection<?>) source);
      }

      @Override
      public Object fromKryoSerializedObject(Object serializedObject) {
        return Collections.unmodifiableList(new ArrayList<>((Collection<?>) serializedObject));
      }
    },
    LIST(Collections.unmodifiableList(new LinkedList<Void>()).getClass()) {
      @Override
      Object toKryoSerializedObject(Object source) {
        return new ArrayList<>((Collection<?>) source);
      }

      @Override
      public Object fromKryoSerializedObject(Object serializedObject) {
        return Collections.unmodifiableList(new LinkedList<>((Collection<?>) serializedObject));
      }
    },
    SET(Collections.unmodifiableSet(new HashSet<Void>()).getClass()) {
      @Override
      Object toKryoSerializedObject(Object source) {
        return new ArrayList<>((Collection<?>) source);
      }

      @Override
      public Object fromKryoSerializedObject(Object serializedObject) {
        return Collections.unmodifiableSet(new HashSet<>((Collection<?>) serializedObject));
      }
    },
    SORTED_SET(Collections.unmodifiableSortedSet(new TreeSet<Void>()).getClass()) {
      @Override
      Object toKryoSerializedObject(Object source) {
        if (((SortedSet<?>) source).comparator() != null) {
          throw new UnsupportedOperationException(
            "Serializing a SortedSet with a custom comparator is not supported, " +
              "the comparator would be lost on deserialization."
          );
        }
        return new ArrayList<>((Collection<?>) source);
      }

      @Override
      public Object fromKryoSerializedObject(Object serializedObject) {
        return Collections.unmodifiableSortedSet(new TreeSet<>((Collection<?>) serializedObject));
      }
    },
    MAP(Collections.unmodifiableMap(new HashMap<Void, Void>()).getClass()) {
      @Override
      Object toKryoSerializedObject(Object source) {
        return new HashMap<>((Map<?, ?>) source);
      }

      @Override
      public Object fromKryoSerializedObject(Object serializedObject) {
        return Collections.unmodifiableMap(new HashMap<>((Map<?, ?>) serializedObject));
      }
    },
    SORTED_MAP(Collections.unmodifiableSortedMap(new TreeMap<Void, Void>()).getClass()) {
      @Override
      Object toKryoSerializedObject(Object source) {
        if (((SortedMap<?, ?>) source).comparator() != null) {
          throw new UnsupportedOperationException(
            "Serializing a SortedMap with a custom comparator is not supported, " +
              "the comparator would be lost on deserialization."
          );
        }
        return new HashMap<>((Map<?, ?>) source);
      }

      @Override
      public Object fromKryoSerializedObject(Object serializedObject) {
        return Collections.unmodifiableSortedMap(new TreeMap<>((Map<?, ?>) serializedObject));
      }
    };

    private final Class<?> type;

    UnmodifiableCollection(Class<?> type) {
      this.type = type;
    }

    /**
     * Converts an unmodifiable collection/map wrapper into a mutable, Kryo-friendly intermediate
     * representation before serialization or copy.
     */
    abstract Object toKryoSerializedObject(Object source);

    /**
     * Reconstructs the expected collection/map implementation from the intermediate representation,
     * then wraps it back into the matching unmodifiable type.
     */
    public abstract Object fromKryoSerializedObject(Object serializedObject);

    static UnmodifiableCollection valueOfType(Class<?> type) {
      for (UnmodifiableCollection item : values()) {
        if (item.type.equals(type)) {
          return item;
        }
      }
      throw new IllegalArgumentException("The type " + type + " is not supported.");
    }
  }
}
