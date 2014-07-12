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

package org.opentripplanner.analyst.core;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.opentripplanner.analyst.TimeSurface;
import org.opentripplanner.analyst.request.RenderRequest;
import org.opentripplanner.analyst.request.TileRequest;
import org.opentripplanner.api.parameter.Style;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**  
 * Analyst 8-bit tile format:
 * Seconds are converted to minutes.
 * Minutes are clamped to +-120
 * Unreachable pixels are set to Byte.MIN_VALUE (-128)
 * Result is stored in image pixel as a signed byte.
 * 
 * So:
 *  -119 to +119 are interpreted literally,
 *  +120 means >= +120,
 *  -120 means <= -120,
 *  -128 means "unreachable".
 */
public abstract class Tile {

    /* STATIC */
    private static final Logger LOG = LoggerFactory.getLogger(Tile.class);

    /**
     *  Creates an interpolated 8-bit color map from the supplied array of color values.
     *  Each row in the input array is a 5-element array consisting of:
     *  colorIndex, red, green, blue, alpha
     *  Color indexes must be in increasing order. Negative indexes will be stored as signed
     *  bytes, so -1 is 0xFF etc. 
     */
    private static IndexColorModel interpolatedColorMap(int[][] breaks) {
        byte[][] vals = new byte[4][256];
        int[] br0 = null;
        for (int[] br1 : breaks) {
            if (br0 != null) {
                int i0 = br0[0];
                int i1 = br1[0];
                int steps = i1 - i0;
                for (int channel = 0; channel < 4; ++channel) {
                    int v0 = br0[channel+1];
                    int v1 = br1[channel+1];
                    float delta = (v1 - v0) / (float) steps;
                    for (int i = 0; i < steps; i++) {
                        int v = v0 + (int)(delta * i);
                        // handle negative indexes
                        int byte_i = 0x000000FF & (i0 + i);
                        vals[channel][byte_i] = (byte)v;
                    }
                }
            }
            br0 = br1;
        }
        return new IndexColorModel(8, 256, vals[0], vals[1], vals[2], vals[3]);        
    }

    /*
     * Pixels are travel times in minutes, stored as signed bytes. This allows us to represent
     * times and time differences with absolute values up to 2 hours.
     */
    private static final IndexColorModel ICM_SMOOTH_COLOR_15 = interpolatedColorMap( new int[][] { 
        {0,     0,   0,   0,  0},  
        {15,  100, 100, 100, 80},  
        {30,    0, 200,   0, 80},  
        {45,    0,   0, 200, 80},
        {60,  200, 200,   0, 80},
        {75,  200,   0,   0, 80},
        {90,  200,   0, 200, 50},
        {120, 200,   0, 200,  0} 
    }); 
    
    private static final IndexColorModel ICM_STEP_COLOR_15 = interpolatedColorMap( new int[][] { 
        {-128, 100, 100, 100, 200}, // for unreachable places 
        {0,   100, 100, 100,  0},  
        {15,  100, 100, 100, 90},  
        {15,    0, 140,   0, 10},  
        {30,    0, 140,   0, 90},  
        {30,    0,   0, 140, 10},  
        {45,    0,   0, 140, 90},
        {45,  140, 140,   0, 10},
        {60,  140, 140,   0, 90},
        {60,  140,   0,   0, 10},
        {75,  140,   0,   0, 90},
        {75,  140,   0, 140, 10},
        {90,  140,   0, 140, 90},
        {90,  100, 100, 100, 50},
        {121, 100, 100, 100, 200} 
    });
    
    private static final IndexColorModel ICM_DIFFERENCE_15 = interpolatedColorMap( new int[][] { 
        {-128,   0,   0, 0,   0},
        {-127, 150,   0, 0,  80},
        {-60,  150,   0, 0,  80},  
        {-15,  150, 150, 0, 80},
        {0, 150,  150,   0,  0},
        {0,    0,   0,   0,  0},
        {15,   0,   0, 150, 80},
        {45,   0, 150,   0, 90},
        {60, 100, 150, 100, 99},
        {127, 50, 150,  50, 99}
    });

    // SAMENESS bands (northern lights color scheme)
    private static final IndexColorModel ICM_SAMENESS_5 = interpolatedColorMap( new int[][] { 
        {-20,  80,  80,  80,   0},
        {-15, 100,   0, 100,  80},
        {-10,   0,   0, 150,  80},  
        {-5,    0, 150,   0,  80},
        { 0,    0, 150,   0, 150},
        { 5,    0, 150,   0,  80},
        { 10,   0,   0, 150,  80},
        { 15, 100,   0, 100,  80},
        { 20,  80,  80,  80,   0},
        {-20,   0,   0,   0,   0} // wrap around to hide inaccessible areas
    });

    private static final IndexColorModel ICM_GRAY_60 = interpolatedColorMap( new int[][] { 
        {-128, 0, 0, 0, 255}, // black out neg/missing/unreachable
        {   0, 0, 0, 0, 255},
        {  60, 0, 0, 0,   0},
        { 120, 0, 0, 0,   0}
    });

    private static final IndexColorModel ICM_MASK_60 = interpolatedColorMap( new int[][] { 
        { 0, 0, 0, 0, 255},
        {60, 0, 0, 0,   0}
    });

//  int[][] breaks = { 
//  // break, r, g, b, a
//  {0,     0, 150,   0, 20},  
//  {15,    0, 150,   0, 80},  
//  {20,    0,   0,  50, 80},  
//  {30,    0,   0, 150, 80},  
//  {40,   50,  50,   0, 80},  
//  {60,  150, 150,   0, 80},  
//  {70,  150,  50,   0, 80},  
//  {90,  150,   0,   0, 80},  
//  {255, 150,   0, 150,  0}
//}; 
//int[][] breaks = { 
//      // break, r, g, b, a
//      {0,   100, 100, 100, 80},  
//      {15,  100, 100, 100, 80},  
//      {15,    0, 150,   0, 80},  
//      {30,    0, 150,   0, 80},  
//      {30,    0,   0, 150, 80},  
//      {45,    0,   0, 150, 80},
//      {45,  150, 150,   0, 80},
//      {60,  150, 150,   0, 80},
//      {60,  150,   0,   0, 80},
//      {75,  150,   0,   0, 80},
//      {75,    0, 100, 100, 80},
//      {255,   0, 100, 100,  0}
//  }; 

    public static final Map<Style, IndexColorModel> modelsByStyle; 
    static {
        modelsByStyle = new EnumMap<Style, IndexColorModel>(Style.class);
        modelsByStyle.put(Style.COLOR30, ICM_STEP_COLOR_15);
        modelsByStyle.put(Style.DIFFERENCE, ICM_DIFFERENCE_15);
        modelsByStyle.put(Style.TRANSPARENT, ICM_GRAY_60); 
        modelsByStyle.put(Style.MASK, ICM_MASK_60);
        modelsByStyle.put(Style.BOARDINGS, buildBoardingColorMap());
    }
    
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
    
    private static IndexColorModel buildOldDefaultColorMap() {
    	Color[] palette = new Color[256];
    	final int ALPHA = 0x60FFFFFF; // ARGB
    	for (int i = 0; i < 28; i++) {
    		// Note: HSB = Hue / Saturation / Brightness
        	palette[i + 00] =  new Color(ALPHA & Color.HSBtoRGB(0.333f, i * 0.037f, 0.8f), true); // Green
        	palette[i + 30] =  new Color(ALPHA & Color.HSBtoRGB(0.666f, i * 0.037f, 0.8f), true); // Blue
        	palette[i + 60] =  new Color(ALPHA & Color.HSBtoRGB(0.144f, i * 0.037f, 0.8f), true); // Yellow
        	palette[i + 90] =  new Color(ALPHA & Color.HSBtoRGB(0.000f, i * 0.037f, 0.8f), true); // Red
        	palette[i + 120] = new Color(ALPHA & Color.HSBtoRGB(0.000f, 0.000f, (29 - i) * 0.0172f), true); // Black
        }
    	for (int i = 28; i < 30; i++) {
        	palette[i + 00] =  new Color(ALPHA & Color.HSBtoRGB(0.333f, (30 - i) * 0.333f, 0.8f), true); // Green
        	palette[i + 30] =  new Color(ALPHA & Color.HSBtoRGB(0.666f, (30 - i) * 0.333f, 0.8f), true); // Blue
        	palette[i + 60] =  new Color(ALPHA & Color.HSBtoRGB(0.144f, (30 - i) * 0.333f, 0.8f), true); // Yellow
        	palette[i + 90] =  new Color(ALPHA & Color.HSBtoRGB(0.000f, (30 - i) * 0.333f, 0.8f), true); // Red
        	palette[i + 120] = new Color(ALPHA & Color.HSBtoRGB(0.000f, 0.000f, (29 - i) * 0.0172f), true); // Black
    	}
        for (int i = 150; i < palette.length; i++) {
        	palette[i] = new Color(0x00000000, true);
        }
        byte[] r = new byte[256];
        byte[] g = new byte[256];
        byte[] b = new byte[256];
        byte[] a = new byte[256];
        for (int i = 0; i < palette.length; i++) {
        	r[i] = (byte)palette[i].getRed();
        	g[i] = (byte)palette[i].getGreen();
        	b[i] = (byte)palette[i].getBlue();
        	a[i] = (byte)palette[i].getAlpha();
        }
        return new IndexColorModel(8, 256, r, g, b, a);
    }
    
    private static IndexColorModel buildBoardingColorMap() {
        byte[] r = new byte[256];
        byte[] g = new byte[256];
        byte[] b = new byte[256];
        byte[] a = new byte[256];
        Arrays.fill(a, (byte) 80);
        g[0] = (byte) 255;
        b[1] = (byte) 255;
        r[2] = (byte) 255;
        g[2] = (byte) 255;
        r[3] = (byte) 255;
        a[255] = 0;
        return new IndexColorModel(8, 256, r, g, b, a);
    }

    protected BufferedImage getEmptyImage(Style style) {
        IndexColorModel colorModel = modelsByStyle.get(style);
        if (colorModel == null)
            return new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        else
            return new BufferedImage(width, height, BufferedImage.TYPE_BYTE_INDEXED, colorModel);
    }
    
    final byte UNREACHABLE = Byte.MIN_VALUE;

    public BufferedImage generateImage(TimeSurface surf, RenderRequest renderRequest) {
        long t0 = System.currentTimeMillis();
        BufferedImage image = getEmptyImage(renderRequest.style);
        byte[] imagePixelData = ((DataBufferByte)image.getRaster().getDataBuffer()).getData();
        int i = 0;
        for (Sample s : getSamples()) {
            byte pixel;
            if (s != null) {
                if (renderRequest.style == Style.BOARDINGS) {
                    pixel = 0; // FIXME s.evalBoardings(surf);
                } else {
                    long t = s.eval(surf); // renderRequest.style
                    if (t == Long.MAX_VALUE)
                        pixel = UNREACHABLE;
                    else {
                        t /= 60;
                        if (t < -120)
                            t = -120;
                        else if (t > 120)
                            t = 120;
                        pixel = (byte) t;
                    }
                }
            } else {
                pixel = UNREACHABLE;
            }
            imagePixelData[i] = pixel;
            i++;
        }
        long t1 = System.currentTimeMillis();
        LOG.debug("filled in tile image from SPT in {}msec", t1 - t0);
        return image;
    }

    public BufferedImage linearCombination(
            double k1, TimeSurface surfA,
            double k2, TimeSurface surfB,
            double intercept, RenderRequest renderRequest) {
        long t0 = System.currentTimeMillis();
        BufferedImage image = getEmptyImage(renderRequest.style);
        byte[] imagePixelData = ((DataBufferByte)image.getRaster().getDataBuffer()).getData();
        int i = 0;
        for (Sample s : getSamples()) {
            byte pixel = UNREACHABLE;
            if (s != null) {
                long t1 = s.eval(surfA);
                long t2 = s.eval(surfB);
                if (t1 != Long.MAX_VALUE && t2 != Long.MAX_VALUE) {
                    double t = (k1 * t1 + k2 * t2) / 60 + intercept; 
                    if (t < -120)
                        t = -120;
                    else if (t > 120)
                        t = 120;
                    pixel = (byte) t;
                }
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

    public static BufferedImage getLegend(Style style, int width, int height) {
        IndexColorModel model = modelsByStyle.get(style);
        if (width < 140 || width > 2000)
            width = 140;
        if (height < 25 || height > 2000)
            height = 25;
        if (model == null)
            return null;

        // These params control spacing of colour bar.
        int startVal = 0;
        int finalVal = 150;
        int labelSpacing = 30; 
        if (style == Style.DIFFERENCE) {
            startVal = -120;
            finalVal = 120;
            labelSpacing = 30;
        }
	int bandsTotal = finalVal - startVal;

        WritableRaster raster = model.createCompatibleWritableRaster(width, height);
        byte[] pixels = ((DataBufferByte) raster.getDataBuffer()).getData();
        for (int row = 0; row < height; row++)
            for (int col = 0; col < width; col++)
                pixels[row * width + col] = (byte) (startVal + col * bandsTotal / width);
        BufferedImage legend = model.convertToIntDiscrete(raster, false);
        Graphics2D gr = legend.createGraphics();
        gr.setColor(new Color(0));
        gr.drawString("travel time (minutes)", 0, 10);
        float scale = width / (float) bandsTotal;
        for (int i = startVal; i < bandsTotal; i += labelSpacing)
            gr.drawString(Integer.toString(i), scale * (-startVal + i), height);
        return legend;
    }

}
