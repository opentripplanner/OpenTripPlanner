package org.opentripplanner.graph_builder.module.osm.storage;

import com.carrotsearch.hppc.LongArrayList;
import com.carrotsearch.hppc.LongLongHashMap;

/**
 * A set of OSM ids, stored as a sparse bitset: a hash map from {@code id / 64} to a 64-bit word
 * with one bit per id.
 * <p>
 * This is used for the sets of node ids referenced by ways and areas. Each node in the input is
 * checked against them, so they are on the hot path of the node parsing phase. Compared to a
 * {@link com.carrotsearch.hppc.LongHashSet} this is better for that case:
 * <ul>
 *   <li>OSM ids are allocated sequentially, and the nodes of a way usually have nearby ids. So
 *   one word typically holds many ids, and the map is much smaller than a hash set of the same
 *   ids: on the Norway extract a word holds about 24 ids, and the set of node ids used by ways
 *   takes 32 MiB instead of 512 MiB. In the worst case, where no two ids share a word, a map
 *   entry (16 bytes) costs twice as much as a hash set entry (8 bytes).</li>
 *   <li>PBF files are sorted by id, so consecutive lookups usually hit the same word, which is
 *   then already in the CPU cache. HPPC hash sets deliberately scatter consecutive keys, so each
 *   lookup is a cache miss.</li>
 *   <li>Most lookups are misses (nodes not used by any routable way), and a miss is a single hash
 *   lookup that finds no word or a word with the bit cleared.</li>
 * </ul>
 * This class is not thread-safe for writes. Concurrent reads are safe once all ids are added.
 */
final class OsmIdBitSet {

  private static final int WORD_BITS_LOG2 = 6;

  private static final long WORD_BIT_MASK = (1L << WORD_BITS_LOG2) - 1;

  private final LongLongHashMap words = new LongLongHashMap();

  void add(long id) {
    long wordIndex = id >>> WORD_BITS_LOG2;
    int index = words.indexOf(wordIndex);
    if (words.indexExists(index)) {
      words.indexReplace(index, words.indexGet(index) | bit(id));
    } else {
      words.indexInsert(index, wordIndex, bit(id));
    }
  }

  void addAll(LongArrayList ids) {
    for (int i = 0; i < ids.size(); i++) {
      add(ids.get(i));
    }
  }

  boolean contains(long id) {
    return (words.get(id >>> WORD_BITS_LOG2) & bit(id)) != 0;
  }

  /** The bit for the given id within its word, selected by the lowest 6 bits of the id. */
  private static long bit(long id) {
    return 1L << (id & WORD_BIT_MASK);
  }
}
