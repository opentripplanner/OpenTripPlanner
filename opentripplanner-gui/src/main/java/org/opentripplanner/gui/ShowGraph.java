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

package org.opentripplanner.gui;

import java.awt.Point;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.MouseMotionAdapter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

import org.opentripplanner.common.IterableLibrary;
import org.opentripplanner.routing.core.DirectEdge;
import org.opentripplanner.routing.core.Edge;
import org.opentripplanner.routing.core.GenericVertex;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.GraphVertex;
import org.opentripplanner.routing.core.TransitStop;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.location.StreetLocation;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.SPTEdge;
import org.opentripplanner.routing.spt.SPTVertex;
import org.opentripplanner.routing.edgetype.PatternAlight;
import org.opentripplanner.routing.edgetype.PatternBoard;
import org.opentripplanner.routing.edgetype.StreetTransitLink;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.PatternEdge;
import org.opentripplanner.routing.edgetype.TurnEdge;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.index.strtree.STRtree;

import processing.core.PApplet;
import processing.core.PFont;

/**
 * Processing applet to show a map of the graph. 
 * The user can:
 * - Use mouse wheel to zoom (or right drag, or ctrl-drag) 
 * - Left drag to pan around the map
 * - Left click to send a list of nearby vertices to the associated
 *   VertexSelectionListener.
 */
public class ShowGraph extends PApplet implements MouseWheelListener {

	private static final int FRAME_RATE = 30;
    private static final long serialVersionUID = -8336165356756970127L;
    Graph graph;
    STRtree vertexIndex;
    STRtree edgeIndex;
    Envelope modelOuterBounds;
    Envelope modelBounds = new Envelope();
    VertexSelectionListener selector;
    private ArrayList<VertexSelectionListener> selectors;
	private List<Vertex> visibleVertices; 
    private List<DirectEdge> visibleStreetEdges = new ArrayList<DirectEdge>(1000); 
    private List<DirectEdge> visibleTransitEdges = new ArrayList<DirectEdge>(1000); 
    private List<Vertex> highlightedVertices;
    private List<DirectEdge> highlightedEdges;
    private Vertex highlightedVertex;
    private DirectEdge highlightedEdge;
    private GraphPath highlightedGraphPath;
    protected double mouseModelX;
    protected double mouseModelY;
    private Point startDrag = null;
    private int dragX, dragY;
    private boolean ctrlPressed = false;
    boolean drawFast = false;
    boolean drawStreetEdges = true;
    boolean drawTransitEdges= true;
    boolean drawLinkEdges = true;
    boolean drawStreetVertices = false;
    boolean drawTransitStopVertices = true;
    private static double lastLabelY;
    private static final DecimalFormat latFormatter = new DecimalFormat("00.0000°N ; 00.0000°S");
    private static final DecimalFormat lonFormatter = new DecimalFormat("000.0000°E ; 000.0000°W");
    private static final SimpleDateFormat shortDateFormat = new SimpleDateFormat("HH:mm:ss z");

    /* Layer constants */
    static final int DRAW_MINIMAL  = 0; // XY coordinates    
    static final int DRAW_VERTICES = 1;     
    static final int DRAW_TRANSIT  = 2; 
    static final int DRAW_STREETS  = 3; 
    static final int DRAW_ALL      = 4; 
    static final int DRAW_PARTIAL  = 6; 
    private int drawLevel = DRAW_ALL;
    private int drawOffset = 0;
    
    /*
     * Constructor.
     * Call processing constructor, and register the listener to notify when the user selects vertices.
     */
    public ShowGraph(VertexSelectionListener selector, Graph graph) {
        super();
        this.graph = graph;
        this.selector = selector;
        this.selectors = new ArrayList<VertexSelectionListener>();
    }

    /*
     * Setup Processing applet
     */
    public void setup() {
        size(getSize().width, getSize().height, P2D);
        
        /* Build spatial index of vertices and edges */
        buildSpatialIndex();

        /* Set model bounds to encompass all vertices in the index, and then some */
        modelBounds = (Envelope)(vertexIndex.getRoot().getBounds());
        modelBounds.expandBy(0.02);
        matchAspect();
        /* save this zoom level to allow returning to default later */
        modelOuterBounds = new Envelope(modelBounds);

        /* find and set up the appropriate font */
        String[] fonts = PFont.list();
        String[] preferredFonts = { "Mono", "Courier" };
        PFont font = null;
        for (String preferredFontName : preferredFonts) {
            for (String fontName : fonts) {
                if (fontName.contains(preferredFontName)) {
                    font = createFont(fontName, 16);
                    break;
                }
            }
            if (font != null) {
                break;
            }
        }
        textFont(font);
        textMode(SCREEN);
        addMouseWheelListener(this);
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                super.mouseMoved(e);
                Point p = e.getPoint();
                mouseModelX = toModelX(p.x);
                mouseModelY = toModelY(p.y);
            }
        });
        addComponentListener(new ComponentAdapter() {
        	  public void componentResized(ComponentEvent e) {
        		  matchAspect();
        		  drawLevel = DRAW_PARTIAL; 
        	  }
        });
        frameRate(FRAME_RATE);
    }

    /*
     * Zoom in/out proportional to the number of clicks of the mouse wheel.
     */
    public void mouseWheelMoved(MouseWheelEvent e) {
  		double f = e.getWheelRotation() * 0.2;
  		zoom(f, e.getPoint());
  	}
    
    /*
     * Zoom in/out.
     * Translate the viewing window such that the place under the mouse pointer is a fixed point.
     * If p is null, zoom around the center of the viewport.
     */
    void zoom (double f, Point p) {
    	double ex = modelBounds.getWidth()  * f;
  		double ey = modelBounds.getHeight() * f;
  		modelBounds.expandBy(ex/2, ey/2);
  		if (p != null) {
  	  		// Note: Graphics Y coordinates increase down the screen, hence the opposite signs.
  	  		double tx = ex * -((p.getX()/this.width)  - 0.5);
  	  		double ty = ey * +((p.getY()/this.height) - 0.5);
  	  	    modelBounds.translate(tx, ty);  			
  		}
  	    // update the display
  	    drawLevel = DRAW_PARTIAL;	
    }

    public void zoomToDefault() {
        modelBounds = new Envelope(modelOuterBounds);
        drawLevel = DRAW_ALL;
    }
    
    public void zoomOut() {
        modelBounds.expandBy(modelBounds.getWidth(), modelBounds.getHeight());
        drawLevel = DRAW_ALL;
    }

    public void zoomToLocation(Coordinate c) {
        Envelope e = new Envelope();
        e.expandToInclude(c);
        e.expandBy(0.002);
        modelBounds = e;
        drawLevel = DRAW_ALL;
    }

    public void zoomToVertex(Vertex v) {
        Envelope e = new Envelope();
        e.expandToInclude(v.getCoordinate());
        e.expandBy(0.002);
        modelBounds = e;
        drawLevel = DRAW_ALL;
    }

    void matchAspect() {
        /* Basic sinusoidal projection of lat/lon data to square pixels */
        double yCenter = modelBounds.centre().y;
        float xScale = cos(radians((float)yCenter));
        double newX = modelBounds.getHeight() * (1 / xScale) * ((float)this.getWidth() / this.getHeight());
        modelBounds.expandBy((newX - modelBounds.getWidth()) / 2f, 0);
    }

    /*
     * Iterate through all vertices and their (outgoing) edges.
     * If they are of 'interesting' types, add them to the
     * corresponding spatial index.
     */
    public synchronized void buildSpatialIndex() {
        vertexIndex = new STRtree();
        edgeIndex = new STRtree();
        Envelope env;
        // int xminx, xmax, ymin, ymax;
        for (GraphVertex gv : graph.getVertices()) {
        	Vertex v = gv.vertex;
            if ((v instanceof TransitStop) ||
            	(v instanceof StreetLocation) || 
            	(v instanceof GenericVertex)) {
            		Coordinate c = gv.vertex.getCoordinate();
            		env = new Envelope(c);
            		vertexIndex.insert(env, v);
            }
            for (DirectEdge e : IterableLibrary.filter(gv.getOutgoing(),DirectEdge.class)) {
                if (e.getGeometry() == null) continue;
                if (e instanceof PatternEdge ||
                	e instanceof StreetTransitLink ||
                	e instanceof StreetEdge) {
                	env = e.getGeometry().getEnvelopeInternal();
                	edgeIndex.insert(env, e);
                }
            }
        }
        vertexIndex.build();
        edgeIndex.build();
    }

    @SuppressWarnings("unchecked")
    private synchronized void findVisibleElements() {
    	visibleVertices = (List<Vertex>) vertexIndex.query(modelBounds);
        visibleStreetEdges.clear();
        visibleTransitEdges.clear();
        for (DirectEdge de : (Iterable<DirectEdge>) edgeIndex.query(modelBounds)) {
        	if (de instanceof PatternEdge ||
        		de instanceof StreetTransitLink) {
        		visibleTransitEdges.add(de);
        	} else if (de instanceof TurnEdge) {
        		visibleStreetEdges.add(de);
        	} else if (de instanceof StreetEdge) {
        		visibleStreetEdges.add(de);
        	}
        }
    }
    
    private int drawEdge(DirectEdge e) {
    	if (e.getGeometry() == null) return 0; // do not attempt to draw geometry-less edges
    	Coordinate[] coords = e.getGeometry().getCoordinates();
	    beginShape();
	    for (int i = 0; i < coords.length; i++)
	        vertex((float) toScreenX(coords[i].x), (float) toScreenY(coords[i].y));
	    endShape();
	    return coords.length; // should be used to count segments, not edges drawn
    }

    /* use endpoints instead of geometry for quick updating */
    private void drawEdgeFast(DirectEdge e) { 
	    Coordinate[] coords = e.getGeometry().getCoordinates();	    
	    Coordinate c0 = coords[0];
	    Coordinate c1 = coords[coords.length - 1]; 
        line((float) toScreenX(c0.x), (float) toScreenY(c0.y),
        		(float) toScreenX(c1.x), (float) toScreenY(c1.y));        		
    }

    private void drawGraphPath(GraphPath gp) {
        // draw edges in different colors according to mode
    	for (SPTEdge se : gp.edges) {
        	Edge edge = se.payload;
        	if (!(edge instanceof DirectEdge)) continue;
        	DirectEdge e = (DirectEdge) edge;
        	TraverseMode mode = se.getMode();
        	if (mode.isTransit()) {
            	stroke(200, 050, 000); 
            	strokeWeight(6);   
            	drawEdge(e);
        	}
        	if (e instanceof StreetEdge) {
        		StreetTraversalPermission stp = ((StreetEdge)e).getPermission();
            	if (stp == StreetTraversalPermission.PEDESTRIAN) {
        			stroke(000, 200, 000);
                	strokeWeight(6);   
                	drawEdge(e);
        		} else if (stp == StreetTraversalPermission.BICYCLE) {
        			stroke(000, 000, 200);
                	strokeWeight(6);   
                	drawEdge(e);
        		} else if (stp == StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE) {
        			stroke(000, 200, 200);
                	strokeWeight(6);   
                	drawEdge(e);
        		} else if (stp == StreetTraversalPermission.ALL) {
        			stroke(200, 200, 200);
                	strokeWeight(6);   
                	drawEdge(e);
        		} else {
        			stroke(64, 64, 64);
                	strokeWeight(6);   
                	drawEdge(e);
        		}
        	}
        }
        // mark key vertices
        SPTVertex fv = gp.vertices.firstElement();
        lastLabelY = -999;
    	labelVertex(fv, "begin");
        for (SPTEdge se : gp.edges) {
        	Edge e = se.payload;
        	if (e instanceof PatternBoard) {
        		labelVertex(se.tov, "board");
        	} else if (e instanceof PatternAlight) {
        		labelVertex(se.fromv, "alight");
        	}
        }
    	SPTVertex lv = gp.vertices.lastElement();
    	labelVertex(lv, "end");
    }

    private void labelVertex(SPTVertex v, String s) {
    	fill(240, 240, 240);
    	drawVertex(v, 8);
    	s += " " + shortDateFormat.format(new Date(v.state.getTime()));
    	s += " [" + (int)v.weightSum + "]";
    	double x = toScreenX(v.getX()) + 10;
    	double y = toScreenY(v.getY());
    	double dy = y - lastLabelY;
    	if (dy == 0) {
    		y = lastLabelY + 20;
    	} else if (Math.abs(dy) < 20) {
    		y = lastLabelY + Math.signum(dy) * 20;
    	}
    	text(s, (float)x, (float)y);
    	lastLabelY = y;
    }

    private void drawVertex(Vertex v, double r) {
    	noStroke();
        ellipse(toScreenX(v.getX()), toScreenY(v.getY()), r, r);
    }

    public synchronized void draw() {
    	final int BLOCK_SIZE = 1000;
    	final long DECIMATE = 100;
    	final int FRAME_TIME = 800 / FRAME_RATE; 
		int startMillis = millis();
        if (drawLevel == DRAW_PARTIAL) { 
          	background(15);
        	stroke(30, 128, 30); 
            strokeWeight(1);
        	noFill();
    		//noSmooth();
        	int drawIndex = 0;
        	int drawStart = 0;
        	int drawCount = 0;
        	while (drawStart < visibleStreetEdges.size()) {
            	if (drawFast) 
            		drawEdgeFast(visibleStreetEdges.get(drawIndex));
            	else 
            		drawEdge(visibleStreetEdges.get(drawIndex));
            	drawIndex += DECIMATE;
            	drawCount += 1;
            	if (drawCount % BLOCK_SIZE == 0 && 
            		millis() - startMillis > FRAME_TIME){
            		drawFast = drawCount < visibleStreetEdges.size() / 4; 
            		break; 
            	}
            	if (drawIndex >= visibleStreetEdges.size()) {
                	drawStart += 1;
                	drawIndex = drawStart;
            	}
            }                
    	} else if (drawLevel == DRAW_ALL) {
    	//} else if (drawLevel == DRAW_STREETS) {
    		//smooth();
    		if (drawOffset == 0) {
    			findVisibleElements();
    			background(15);
    		}
    		if (drawStreetEdges) {
        	    stroke(30, 128, 30); // dark green
                strokeWeight(1);   
            	noFill();
                //for (DirectEdge e : visibleStreetEdges) drawEdge(e);
                while (drawOffset < visibleStreetEdges.size()) {
                	drawEdge(visibleStreetEdges.get(drawOffset));
                	drawOffset += 1;
                	// if (drawOffset % FRAME_SIZE == 0) return; 
                	if (drawOffset % BLOCK_SIZE == 0) {
                		if (millis() - startMillis > FRAME_TIME) return; 
                	}
                }                
	  	    }
    	} else if (drawLevel == DRAW_TRANSIT) {
    		if (drawTransitEdges) {
				stroke(40, 40, 128, 30); // transparent blue
				strokeWeight(4);
            	noFill();
    			//for (DirectEdge e : visibleTransitEdges) {
    			while (drawOffset < visibleTransitEdges.size()) {
    				DirectEdge e = visibleTransitEdges.get(drawOffset);
    				drawEdge(e);
    				drawOffset += 1;
                	if (drawOffset % BLOCK_SIZE == 0) {
                		if (millis() - startMillis > FRAME_TIME) return; 
                	}
    			}
    		}
    	} else if (drawLevel == DRAW_VERTICES) {    	        
      	    /* turn off vertex display when zoomed out */
        	final double METERS_PER_DEGREE_LAT = 111111.111111;
      	    drawTransitStopVertices = (modelBounds.getHeight() * METERS_PER_DEGREE_LAT / this.width < 10);
	        /* Draw selected visible vertices */
        	fill(60, 60, 200);
	        for (Vertex v : visibleVertices) {
	            if (drawTransitStopVertices && v instanceof TransitStop) {
	                drawVertex(v, 5);   
	            } 
	        }
            /* Draw highlighted edges in another color */
            noFill();
        	stroke(200, 200, 000, 128); // yellow transparent edge highlight
            strokeWeight(8);   
	  	    if (highlightedEdges != null) {
		  	    for (DirectEdge e : highlightedEdges) {
	                drawEdge(e);
	            }
	  	    }
            /* Draw highlighted graph path in another color */
	  	    if (highlightedGraphPath != null) {
                drawGraphPath(highlightedGraphPath);
	  	    }
            /* Draw (single) highlighted edge in highlight color */
	        if (highlightedEdge != null && highlightedEdge.getGeometry() != null) {
	            stroke(200, 10, 10, 128);
	            strokeWeight(8);   
	            drawEdge(highlightedEdge);
	        }
	        /* Draw highlighted vertices */
    		fill(255, 127, 0); //orange fill
            noStroke();   
	        if (highlightedVertices != null) {
	        	for (Vertex v : highlightedVertices) {
	                drawVertex(v, 8);
	        	}
            }
            /* Draw (single) highlighed vertex in a different color */
	        if (highlightedVertex != null) {
	            fill(255, 255, 30);
	            drawVertex(highlightedVertex, 7);
	        }
            noFill();
  	    } else if (drawLevel==DRAW_MINIMAL) {
  	        // Black background box
  	    	fill(0, 0, 0);
  	    	stroke(30, 128, 30);
  	    	// noStroke();
  	    	strokeWeight(1);
  	        rect(3, 3, 303, textAscent() + textDescent() + 6);            
  	        // Print lat & lon coordinates
  	    	fill(128, 128, 256);
  	        //noStroke();
  	    	String output = lonFormatter.format(mouseModelX) + " " + latFormatter.format(mouseModelY);
  	    	textAlign(LEFT, TOP);
  	    	text(output, 6, 6);                
  	    }
  	    drawOffset = 0;
  	    if (drawLevel > DRAW_MINIMAL) drawLevel -= 1; // move to next layer
    }
    
    private double toScreenY(double y) {
        return map(y, modelBounds.getMinY(), modelBounds.getMaxY(), getSize().height, 0);
    }

    private double toScreenX(double x) {
        return map(x, modelBounds.getMinX(), modelBounds.getMaxX(), 0, getSize().width);
    }

    public void keyPressed() {
    	if (key == CODED && keyCode == CONTROL) ctrlPressed = true;
    }

    public void keyReleased() {
    	if (key == CODED && keyCode == CONTROL) ctrlPressed = false;
    } 

    @SuppressWarnings("unchecked")
    public void mouseClicked() {
        Envelope screenEnv = new Envelope(new Coordinate(mouseX, mouseY));
        screenEnv.expandBy(4, 4);
        Envelope env = new Envelope(toModelX(screenEnv.getMinX()), toModelX(screenEnv.getMaxX()),
                toModelY(screenEnv.getMinY()), toModelY(screenEnv.getMaxY()));

        List<Vertex> nearby = (List<Vertex>) vertexIndex.query(env);
        selector.verticesSelected(nearby);
        drawLevel = DRAW_ALL;
    }

    public void mouseReleased(MouseEvent e) {
        startDrag = null;
    }

    public void mouseDragged(MouseEvent e) {
    	Point c = e.getPoint();
    	if (startDrag == null) {
	        startDrag = c;
	        dragX = c.x;
	        dragY = c.y;
    	}
      	double dx = dragX - c.x;
       	double dy = c.y - dragY;
       	if (ctrlPressed || mouseButton == RIGHT) {
       		zoom(dy * 0.01, startDrag);
       	} else {
	   		double tx = modelBounds.getWidth()  * dx / getWidth();
	   		double ty = modelBounds.getHeight() * dy / getHeight();
	   		modelBounds.translate(tx, ty);
       	}
        dragX = c.x;
        dragY = c.y;
        drawLevel = DRAW_PARTIAL;
    }
    
    private double toModelY(double y) {
        return map(y, 0, getSize().height, modelBounds.getMaxY(), modelBounds.getMinY());
    }

    private double toModelX(double x) {
        return map(x, 0, getSize().width, modelBounds.getMinX(), modelBounds.getMaxX());
    }

    /**
     * A version of ellipse that takes double args, because apparently Java is too stupid to
     * downgrade automatically.
     * 
     * @param d
     * @param e
     * @param f
     * @param g
     */
    private void ellipse(double d, double e, double f, double g) {
        ellipse((float) d, (float) e, (float) f, (float) g);
    }

    /**
     * Set the Vertex selector to newSelector, and store the old selector on the stack of selectors
     * 
     * @param newSelector
     */
    public void pushSelector(VertexSelectionListener newSelector) {
        selectors.add(selector);
        selector = newSelector;
    }

    /**
     * Restore the previous vertexSelector
     */
    public void popSelector() {
        selector = selectors.get(selectors.size() - 1);
        selectors.remove(selectors.size() - 1);
    }

    public void highlightVertex(Vertex v) {
        Coordinate c = v.getCoordinate();
        double xd = 0, yd = 0;
        while (!modelBounds.contains(c)) {
            xd = modelBounds.getWidth() / 100;
            yd = modelBounds.getHeight() / 100;
            modelBounds.expandBy(xd, yd);
        }
        modelBounds.expandBy(xd, yd);
        highlightedVertex = v;
        drawLevel = DRAW_ALL;
    }

    public void highlightEdge(DirectEdge selected) {
        highlightedEdge = selected;
        drawLevel = DRAW_ALL;
    }

    public void highlightGraphPath(GraphPath gp) {
        highlightedGraphPath = gp;
        drawLevel = DRAW_ALL;
    }

    public void setHighlightedVertices(Set<Vertex> vertices) {
        highlightedVertices = new ArrayList<Vertex>(vertices);
        drawLevel = DRAW_ALL;
    }

    public void setHighlightedVertices(List<Vertex> vertices) {
        highlightedVertices = vertices;
        drawLevel = DRAW_ALL;
    }

    public void setHighlightedEdges(List<DirectEdge> edges) {
        highlightedEdges = edges;
        drawLevel = DRAW_ALL;
    }
}
