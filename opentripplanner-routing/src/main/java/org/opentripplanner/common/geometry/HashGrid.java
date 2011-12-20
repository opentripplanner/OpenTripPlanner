package org.opentripplanner.common.geometry;

import java.util.ArrayList;
import java.util.List;

import org.opentripplanner.routing.impl.DistanceLibrary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * A spatial index using a 2D hashtable that wraps around at the edges.
 * Mapping geographic space to a region near the origin using the modulo operator
 * preserves proximity allowing 100 percent recall (no false dismissals) in linear time, 
 * at the cost of false positives (hash collisions) which are filtered out of query results.
 * 
 * Currently only handles pointlike objects, but 2d objects could be handled by either
 * rasterizing them into grid cells, or using a hierarchical grid.
 * 
 * For more background see:
 * Schornbaum, Florian. Hierarchical Hash Grids for Coarse Collision Detection (2009)
 * 
 * @author andrewbyrd
 *
 * @param <T> The type of objects to be stored in the HashGrid. Must implement the Pointlike
 * interface.
 */
public class HashGrid<T extends Pointlike> {

private static final Logger LOG = LoggerFactory.getLogger(HashGrid.class);
    private static final double PROJECTION_MERIDIAN = -100;
    private final double binSizeMeters;
    private final int nBinsX, nBinsY;
    private final List<T>[][] bins;
    private int nBins = 0;
    private int nEntries = 0;
    
    @SuppressWarnings("unchecked")
    public HashGrid(double binSizeMeters, int xBins, int yBins) {
        if (binSizeMeters < 0)
            throw new IllegalStateException("bin size must be positive.");
        this.binSizeMeters = binSizeMeters;
        this.nBinsX = xBins;
        this.nBinsY = yBins;
        bins = (List<T>[][]) new List[xBins][yBins];
    }
    
    public void put(T t) {
        bin(t).add(t);
        nEntries +=1;
    }

    private List<T> bin(Pointlike p) {
        int xBin = xBin(p); 
        int yBin = yBin(p);
        List<T> bin = bins[xBin][yBin];
        if (bin == null) {
            bin = new ArrayList<T>();
            bins[xBin][yBin] = bin; 
            nBins += 1;
        }
        return bin;
    }
    
    private int xBin(Pointlike p) {
        double x = projLon(p.getLon(), p.getLat());
        return xWrap( (int) Math.floor(x / binSizeMeters) );
    }
    
    private int yBin(Pointlike p) {
        double y = projLat(p.getLat());
        return yWrap( (int) Math.floor(y / binSizeMeters) );
    }

    private int xWrap(int xBin) {
        int x = xBin % nBinsX;
        if (x < 0)
            x += nBinsX;
        return x;
    }
    
    private int yWrap(int yBin) {
        int y = yBin % nBinsY;
        if (y < 0)
            y += nBinsY;
        return y;
    }
        
    public T closest(Pointlike p, double radiusMeters) {
        T closestT = null;
        double closestDistance = Double.POSITIVE_INFINITY;
        int radiusBins = (int) Math.ceil(radiusMeters / binSizeMeters);
        int xBin = xBin(p);
        int yBin = yBin(p);
        int minBinX = xBin - radiusBins;
        int maxBinX = xBin + radiusBins;
        int minBinY = yBin - radiusBins;
        int maxBinY = yBin + radiusBins;
        for (int x = minBinX; x <= maxBinX; x += 1) {
            int wrappedX = xWrap(x);
            for (int y = minBinY; y <=maxBinY; y += 1) {
                int wrappedY = yWrap(y);
                List<T> bin = bins[wrappedX][wrappedY];
                if (bin != null) {
                    for (T t : bin) {
                        if (t == p)
                            continue;
                        double distance = p.fastDistance(t);
                        if (distance < closestDistance) {
                            closestT = t;
                            closestDistance = distance;
                        }
                    }
                }
            }
        }
        return closestT;
    }

    static final double M_PER_DEGREE_LAT = 111111.111111;
    
    static double mPerDegreeLon(double lat) {
        return M_PER_DEGREE_LAT * Math.cos(lat * Math.PI / 180);        
    }

    private static double projLat(double lat) {
        return M_PER_DEGREE_LAT * lat; 
    }
    
    private static double projLon(double lon, double lat) {
        return mPerDegreeLon(lat) * (lon - PROJECTION_MERIDIAN);
    }

    public String toString() {
        return String.format("HashGrid %dx%d at %d meter resolution, %d/%d bins allocated", 
               nBinsX, nBinsY, (int)binSizeMeters, nBins, nBinsX*nBinsY);
    }
    
    public String toStringVerbose() {
        return String.format("HashGrid %dx%d at %d meter resolution, %d/%d bins allocated (%4.1f%%), load factor %4f, average allocated bin size %2f", 
               nBinsX, nBinsY, (int)binSizeMeters, nBins, nBinsX*nBinsY, (double)nBins/(nBinsX*nBinsY) * 100.0, 
               (double)nEntries/(nBinsX*nBinsY), (double)nEntries/nBins);
    }

    public String densityMap() {
        StringBuilder sb = new StringBuilder();
        sb.append("HashGrid:\n");
        // flip map for northern hemisphere
        for (int y=nBinsY-1; y>=0; y--) {
            for (int x=0; x<nBinsX; x++) {
                if (bins[x][y] == null) {
                    sb.append("__"); 
                } else {
                    int z = bins[x][y].size();
                    if (z < 10) {
                        sb.append('.');
                        sb.append(z);
                    } else if (z < 100) {
                        sb.append(z);
                    } else { 
                        sb.append("XX");
                    }
                }
            }    
            sb.append('\n');
        }
        return sb.toString();
    }

    public class Point implements Pointlike {
        
        double lon, lat;
        
        public Point(Pointlike p) {
            this.lon = p.getLon();
            this.lat = p.getLat();
        }
        
        public Point(double lon, double lat) {
            this.lon = lon;
            this.lat = lat;
        }
        
        public double getLon() {
            return lon;
        }
        
        public double getLat() {
            return lat;
        }
        
        @Override
        public double distance(Coordinate c) {
            return DistanceLibrary.distance(lat, lon, c.y, c.x);
        }

        @Override
        public double distance(Pointlike p) {
            return DistanceLibrary.distance(lat, lon, p.getLat(), p.getLon());
        }
        
        @Override
        public double fastDistance(Pointlike p) {
            return DistanceLibrary.fastDistance(lat, lon, p.getLat(), p.getLon());
        }
    }
}
