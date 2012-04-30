package org.opentripplanner.analyst.core;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.IndexColorModel;
import java.util.Arrays;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.opentripplanner.analyst.request.RenderRequest;
import org.opentripplanner.analyst.request.TileRequest;
import org.opentripplanner.analyst.rest.parameter.Style;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Tile {

    /* STATIC */
    private static final Logger LOG = LoggerFactory.getLogger(Tile.class);
    public static final IndexColorModel DEFAULT_COLOR_MAP = buildDefaultColorMap();
    public static final IndexColorModel DIFFERENCE_COLOR_MAP = buildDifferenceColorMap();
    public static final IndexColorModel TRANSPARENT_COLOR_MAP = buildTransparentColorMap();
    public static final IndexColorModel MASK_COLOR_MAP = buildMaskColorMap(90);
    
    /* INSTANCE */
    final GridGeometry2D gg;
    final int width, height;
    
    Tile(TileRequest req) {
        GridEnvelope2D gridEnv = new GridEnvelope2D(0, 0, req.width, req.height);
        this.gg = new GridGeometry2D(gridEnv, (org.opengis.geometry.Envelope)(req.bbox));
        // TODO: check that gg intersects graph area 
        LOG.debug("preparing tile for {}", gg.getEnvelope2D());
        // Envelope2D worldEnv = gg.getEnvelope2D();
        this.width = gridEnv.width;
        this.height = gridEnv.height;
    }
    
    private static IndexColorModel buildDefaultColorMap() {
        byte[] r = new byte[256];
        byte[] g = new byte[256];
        byte[] b = new byte[256];
        byte[] a = new byte[256];
        Arrays.fill(a, (byte)0);
        for (int i=0; i<30; i++) {
            g[i + 00]  =  // <  30 green 
            a[i + 00]  =  
            b[i + 30]  =  // >= 30 blue
            a[i + 30]  =  
            g[i + 60]  =  // >= 60 yellow 
            r[i + 60]  =
            a[i + 60]  =  
            r[i + 90]  =  // >= 90 red
            a[i + 90]  =  
            b[i + 120] =  // >=120 pink fading to transparent 
            a[i + 120] =  
            r[i + 120] = (byte) (255 - (42 - i) * 6);
        }
        return new IndexColorModel(8, 256, r, g, b, a);
    }
    
    private static IndexColorModel buildDifferenceColorMap() {
        byte[] r = new byte[256];
        byte[] g = new byte[256];
        byte[] b = new byte[256];
        byte[] a = new byte[256];
        Arrays.fill(a, (byte) 64);
        for (int i=0; i<118; i++) {
            g[117 - i] = (byte) (i * 2);
            r[137 + i] = (byte) (i * 2);
            a[117 - i] = (byte) (64+(i * 2));
            a[137 + i] = (byte) (64+(i * 2));
            //b[128 + i] = g[128 - i] = (byte)120; 
        }
//        for (int i=0; i<10; i++) {
//            byte v = (byte) (255 - i * 25);
//            g[127 - i] = v;
//            g[128 + i] = v;
//        }
//        a[255] = 64;
        return new IndexColorModel(8, 256, r, g, b, a);
    }

    private static IndexColorModel buildTransparentColorMap() {
        byte[] r = new byte[256];
        byte[] g = new byte[256];
        byte[] b = new byte[256];
        byte[] a = new byte[256];
        for (int i=0; i<60; i++) {
            int alpha = 240 - i * 4;
            a[i] = (byte) alpha;
        }
        for (int i=60; i<255; i++) {
            a[i] = 0;
        }
        a[255] = (byte) 240;
        return new IndexColorModel(8, 256, r, g, b, a);
    }

    private static IndexColorModel buildMaskColorMap(int max) {
        byte[] r = new byte[256];
        byte[] g = new byte[256];
        byte[] b = new byte[256];
        byte[] a = new byte[256];
        for (int i=0; i<max; i++) {
            int alpha = (i * 210 / max);
            a[i] = (byte) alpha;
        }
        for (int i=max; i<=255; i++) {
            a[i] = (byte)210;
//            r[i] = (byte)255;
//            g[i] = (byte)128;
//            b[i] = (byte)128;
        }
        //a[255] = (byte) 240;
        return new IndexColorModel(8, 256, r, g, b, a);
    }

    protected BufferedImage getEmptyImage(Style style) {
        BufferedImage image;
        switch (style) {
        case GRAY :
            image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
            break;
        case DIFFERENCE :
            image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_INDEXED, DIFFERENCE_COLOR_MAP);
            break;
        case TRANSPARENT :
            image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_INDEXED, TRANSPARENT_COLOR_MAP);
            break;
        case MASK :
            image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_INDEXED, MASK_COLOR_MAP);
            break;
        case COLOR30 :
        default :
            image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_INDEXED, DEFAULT_COLOR_MAP);
        }
        return image;
    }
    
    public BufferedImage generateImage(ShortestPathTree spt, RenderRequest renderRequest) {
        long t0 = System.currentTimeMillis();
        BufferedImage image = getEmptyImage(renderRequest.style);
        byte[] imagePixelData = ((DataBufferByte)image.getRaster().getDataBuffer()).getData();
        int i = 0;
        final byte TRANSPARENT = (byte) 255;
        for (Sample s : getSamples()) {
            byte pixel;
            if (s != null) {
                pixel = s.evalByte(spt);
            } else {
                pixel = TRANSPARENT;
            }
            imagePixelData[i] = pixel;
            i++;
        }
        long t1 = System.currentTimeMillis();
        LOG.debug("filled in tile image from SPT in {}msec", t1 - t0);
        return image;
    }

    public BufferedImage linearCombination(
            double k1, ShortestPathTree spt1, 
            double k2, ShortestPathTree spt2, 
            double intercept, RenderRequest renderRequest) {
        long t0 = System.currentTimeMillis();
        BufferedImage image = getEmptyImage(renderRequest.style);
        byte[] imagePixelData = ((DataBufferByte)image.getRaster().getDataBuffer()).getData();
        int i = 0;
        final byte TRANSPARENT = (byte) 255;
        for (Sample s : getSamples()) {
            byte pixel;
            if (s != null) {
                double t = (k1 * s.eval(spt1) + k2 * s.eval(spt2)) / 60 + intercept; 
                if (t < 0 || t > 255)
                    t = TRANSPARENT;
                pixel = (byte) t;
            } else {
                pixel = TRANSPARENT;
            }
            imagePixelData[i] = pixel;
            i++;
        }
        long t1 = System.currentTimeMillis();
        LOG.debug("filled in tile image from SPT in {}msec", t1 - t0);
        return image;
    }

    public GridCoverage2D getGridCoverage2D(BufferedImage image) {
        GridCoverage2D gridCoverage = new GridCoverageFactory()
            .create("isochrone", image, gg.getEnvelope2D());
        return gridCoverage;
    }

    public abstract Sample[] getSamples();

}
