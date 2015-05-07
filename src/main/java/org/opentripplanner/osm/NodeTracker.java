package org.opentripplanner.osm;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;

/** 
 * A sparse bit set capable of handling 64-bit int indexes (like OSM IDs).
 * Each block is 64 64-bit longs. At 512 bytes long, it tracks 4096 IDs.
 * Node numbers in OSM tend to be contiguous so maybe the blocks should be bigger.
 * Disabled debug statements have no measurable effect on speed (verified).
 *
 * MapDB TreeSets are much faster than MapDB HashSets, but in-memory NodeTrackers are
 * much faster than MapDB TreeSets.
 */
public class NodeTracker {

    private static final Logger LOG = LoggerFactory.getLogger(NodeTracker.class);
    
    /** Lowest 6 bits, index into a single 64-bit long int. */
    private static final long LOW_MASK = 0x3F; 
    private static final int LOW_SHIFT = 6;
    
    /** Lowest 12 bits, bit position within block. */
    private static final long MID_MASK = 0xFFF;
    
    /** Highest 52 bits, the key for the block. */
    private static final long BLOCK_MASK = ~MID_MASK;
    
    /** Each block is 64 64-bit longs. */ 
    private Map<Long, long[]> blocks = Maps.newHashMap();

    private int index(long x) {
        return (int)((x & MID_MASK) >> LOW_SHIFT);
    }

    private int bit(long x) {
        return (int)(x & LOW_MASK);
    }
    
    /** 
     * The create parameter allows us to examine a block for a set bit 
     * without creating an empty block as a side effect.
     */
    private long[] block(long x, boolean create) {
        long key = x & BLOCK_MASK;
        long[] block = blocks.get(key);
        if (block == null && create) {
            block = new long[64];
            blocks.put(key, block);
            LOG.debug("Number of blocks is now {}, total {} MiB", blocks.size(), blocks.size() * 64 * 8 / 1024.0 / 1024.0);
        }
        LOG.debug("   block {}", key);
        return block;
    }
    
    public void add(long x) {
        LOG.debug("set node {}: index {} bit {}", x, index(x), bit(x));
        block(x, true)[index(x)] |= (1L << bit(x));
    }

    public boolean contains(long x) {
        LOG.debug("get node {}: index {} bit {}", x, index(x), bit(x));
        long[] block = block(x, false);
        if (block == null) return false; 
        return (block[index(x)] & (1L << bit(x))) != 0;
    }

    public static NodeTracker acceptEverything() {
        return new NodeTracker() {
            @Override 
            public boolean contains(long x) {
                return true;
            }
        };
    }
    
}

// Scan through nodes, marking those that are within the bbox.
// Then load ways, keeping only those that contain at least one marked node.
// Finally, load all nodes that are in any of those ways.

// Filters: bbox and tags.
// Use C PBF converter to pre-filter the data. Toolchains.
