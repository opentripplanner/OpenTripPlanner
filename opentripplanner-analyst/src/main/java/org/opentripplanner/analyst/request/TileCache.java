package org.opentripplanner.analyst.request;

import org.opentripplanner.analyst.core.DynamicTile;
import org.opentripplanner.analyst.core.Tile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.Weigher;

@Component
public class TileCache extends CacheLoader<TileRequest, Tile> 
    implements  Weigher<TileRequest, Tile> { 
    
    private static final Logger LOG = LoggerFactory.getLogger(TileCache.class);

    @Autowired
    private SampleFactory sampleFactory;
    
//    @Autowired
//    private HashGridSampler hashSampler;
//
//    @Autowired
//    private SampleCache sampleCache;

    private final LoadingCache<TileRequest, Tile> tileCache = CacheBuilder
            .newBuilder()
            .concurrencyLevel(32)
            .maximumSize(900)
            //.softValues()
            .build(this);

    @Override
    /** completes the abstract CacheLoader superclass */
    public Tile load(TileRequest req) throws Exception {
        LOG.debug("tile cache miss; cache size is {}", this.tileCache.size());
        //return new TemplateTile(req, sampleFactory);
        //return new TemplateTile(req, hashSampler);
        //return new DynamicTile(req, hashSampler);
        return new DynamicTile(req, sampleFactory);
    }

    /** delegate to the tile LoadingCache */
    public Tile get(TileRequest req) throws Exception {
        return tileCache.get(req);
    }
    
    @Override
    public int weigh(TileRequest req, Tile tile) {
        return tile.getSamples().length;
    }
    
}
