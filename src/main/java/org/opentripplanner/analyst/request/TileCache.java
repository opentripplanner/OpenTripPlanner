/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.analyst.request;

import org.opentripplanner.analyst.core.TemplateTile;
import org.opentripplanner.analyst.core.Tile;
import org.opentripplanner.routing.graph.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.Weigher;

public class TileCache extends CacheLoader<TileRequest, Tile> 
    implements  Weigher<TileRequest, Tile> { 
    
    private static final Logger LOG = LoggerFactory.getLogger(TileCache.class);

    private Graph graph;

    public TileCache(Graph graph) {
        this.graph = graph;
        this.tileCache = CacheBuilder.newBuilder()
                .concurrencyLevel(concurrency)
                .maximumSize(size)
                .build(this);
    }

    private LoadingCache<TileRequest, Tile> tileCache;
    public int size = 200;
    public int concurrency = 16;

    @Override
    /** completes the abstract CacheLoader superclass */
    public Tile load(TileRequest req) throws Exception {
        LOG.debug("tile cache miss; cache size is {}", this.tileCache.size());
        return new TemplateTile(req, graph);
        //return new TemplateTile(req, hashSampler);
        //return new DynamicTile(req, hashSampler);
        //return new DynamicTile(req, sampleFactory);
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
