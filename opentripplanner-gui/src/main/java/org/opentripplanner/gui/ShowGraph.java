package org.opentripplanner.gui;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.opentripplanner.routing.core.Edge;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.Intersection;
import org.opentripplanner.routing.core.TransitStop;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.edgetype.Street;
import org.opentripplanner.routing.edgetype.StreetTransitLink;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.index.strtree.STRtree;

import processing.core.PApplet;
import processing.core.PFont;

/**
 * Show a map of the graph, intersections and TransitStops only.  The user can drag to zoom.
 * The user can also click, and a list of nearby vertices will be sent to the associated  
 * VertexSelectionListener
 */
public class ShowGraph extends PApplet {

    private static final long serialVersionUID = -8336165356756970127L;

    Graph graph;

    STRtree vertexIndex;
    
    STRtree edgeIndex;

    /*
     * static public void main(String args[]) { PApplet.main(new String[] {"ShowGraph"}); }
     */

    Envelope modelOuterBounds;

    Envelope modelBounds = new Envelope();

    Envelope screenBounds;

    VertexSelectionListener selector;

    private ArrayList<VertexSelectionListener> selectors;

    int startDragX, startDragY;

    private Edge highlightedEdge;

    private Vertex highlightedVertex;

    private Set<Vertex> highlightedVertices = new HashSet<Vertex>();

    protected double mouseModelX;

    protected double mouseModelY;

    public ShowGraph(VertexSelectionListener selector, Graph graph) {
        super();
        this.graph = graph;
        this.selector = selector;
        this.selectors = new ArrayList<VertexSelectionListener>();
    }
    
    public void setup() {
        screenBounds = new Envelope(0, 700, 0, 700);
        background(0);
        size(700, 700, P2D);

        vertexIndex = new STRtree();
        edgeIndex = new STRtree();
        for (Vertex v : graph.getVertices()) {
            Envelope env = new Envelope(v.getCoordinate());
            modelBounds.expandToInclude(env);
            if (v instanceof TransitStop) {
                vertexIndex.insert(env, v);
            } else if(v instanceof Intersection) {
                vertexIndex.insert(env, v);
            } else {
                //a street-transit link, or a journey vertex.  no need for them in the ui.
            }
            
            for (Edge e : v.getOutgoing()) {
            	if (e instanceof Street || e instanceof StreetTransitLink) {
            		env = new Envelope(e.getFromVertex().getCoordinate());
            		edgeIndex.insert(env, e);
            	}
            }
        }
        modelBounds.expandBy(0.02);
        /* fix aspect ratio */
        double yCenter = (modelBounds.getMaxY() + modelBounds.getMinY()) / 2;
        float xScale = cos((float) (yCenter * Math.PI / 180));
        double xSize = modelBounds.getMaxX() - modelBounds.getMinX();
        double ySize = modelBounds.getMaxY() - modelBounds.getMinY();
        double actualXSize = xSize * xScale;
        System.out.println ("xs, ys, axs: " + xSize + ", " + ySize + "," + actualXSize);
        if (ySize > actualXSize) {
            //too tall, stretch horizontally
            System.out.println ("stretching x by " + (ySize / xScale - actualXSize));
            modelBounds.expandBy((ySize / xScale - actualXSize) / 2, 0);
        } else {
            //too wide, stretch vertically
            System.out.println ("stretching y by " + (actualXSize - ySize));
            modelBounds.expandBy(0, (actualXSize - ySize) / 2);
        }

        modelOuterBounds = new Envelope(modelBounds);
        vertexIndex.build();
        edgeIndex.build();

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                super.mouseMoved(e);
                Point p = e.getLocationOnScreen();
                mouseModelX = toModelX(p.x);
                mouseModelY = toModelY(p.y);
            }
        });

        /* find and set up the appropriate font */
        String[] fonts = PFont.list();
        String[] preferredFonts = {"Courier", "Mono"};
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
    }

    public void zoomToDefault() {
        modelBounds = new Envelope(modelOuterBounds);
    }
    
    public void zoomToVertex(Vertex v) {
    	Envelope e = new Envelope();
    	e.expandToInclude(v.getCoordinate());
    	e.expandBy(0.002);
    	modelBounds = e;
    }
    
    @SuppressWarnings("unchecked")
    public void draw() {
        fill(0);
        rect(0, 0, (int)screenBounds.getMaxX(), (int) screenBounds.getMaxY());
        List<Vertex> vertices = (List<Vertex>) vertexIndex.query(modelBounds);
        
        for (Vertex v : vertices) {
            double x = v.getX();
            double y = v.getY();
            x = toScreenX(x);
            y = toScreenY(y);
            if (v instanceof TransitStop) {
                stroke(255, 30, 255);
                ellipse(x, y, 5.0, 5.0);
            } else if (highlightedVertices.contains(v)) {
                stroke(0, 255, 0);
                ellipse(x, y, 3.0, 3.0);
            } else {
                stroke(255);
                ellipse(x, y, 3.0, 3.0);
            }
            
        }

        List<Edge> edges = (List<Edge>) edgeIndex.query(modelBounds);
        for (Edge e : edges) {
        	double x1 = toScreenX(e.getFromVertex().getX());
        	double y1 = toScreenY(e.getFromVertex().getY());
        	double x2 = toScreenX(e.getToVertex().getX());
        	double y2 = toScreenY(e.getToVertex().getY());
        	
        	if (e instanceof StreetTransitLink) {
        		stroke(75, 150, 255);
        	} else {
        		stroke(30, 255, 255);
        	}

        	line((float)x1, (float)y1, (float)x2, (float)y2);
        }
        if (highlightedEdge != null) {
            double x1 = toScreenX(highlightedEdge.getFromVertex().getX());
            double y1 = toScreenY(highlightedEdge.getFromVertex().getY());
            double x2 = toScreenX(highlightedEdge.getToVertex().getX());
            double y2 = toScreenY(highlightedEdge.getToVertex().getY());
            stroke (255, 70, 70);
            line((float)x1, (float)y1, (float)x2, (float)y2);
        }

        if (highlightedVertex != null) {
            stroke(255, 255, 30);
            fill(255, 255, 30);
            ellipse(toScreenX(highlightedVertex.getX()), toScreenY(highlightedVertex.getY()), 7.0, 7.0);
            noFill();
        }
        fill(255, 0, 0);
        text(mouseModelX + ", " + mouseModelY, 0, 10);
    }

    private double toScreenY(double y) {
        return map(y, modelBounds.getMinY(), modelBounds.getMaxY(), screenBounds.getMaxY(),
                screenBounds.getMinY());
    }

    private double toScreenX(double x) {
        return map(x, modelBounds.getMinX(), modelBounds.getMaxX(), screenBounds.getMinX(),
                screenBounds.getMaxX());
    }

    @SuppressWarnings("unchecked")
    public void mouseClicked() {
        Envelope screenEnv = new Envelope(new Coordinate(mouseX, mouseY));
        screenEnv.expandBy(3, 3);
        Envelope env = new Envelope(toModelX(screenEnv.getMinX()), toModelX(screenEnv.getMaxX()), 
                toModelY(screenEnv.getMinY()), toModelY(screenEnv.getMaxY()));
        
        List<Vertex> nearby = (List<Vertex>) vertexIndex.query(env);
        selector.verticesSelected(nearby);
    }
    
    public void mousePressed() {
        startDragX = mouseX;
        startDragY = mouseY;
    }
    
    public void mouseReleased() {
        if (startDragX == -1 || startDragY == -1 || Math.abs(mouseX - startDragX) < 5 || Math.abs(mouseY - startDragY) < 5) {
            startDragX = startDragY = -1;
            return; 
        }
        //rescale
        double x1 = toModelX(startDragX);
        double x2 = toModelX(mouseX);
        if (x1 > x2) {
            double tmp = x1;
            x1 = x2;
            x2 = tmp;
        }
        double y1 = toModelY(startDragY);
        double y2 = toModelY(mouseY);
        if (y1 > y2) {
            double tmp = y1;
            y1 = y2;
            y2 = tmp;
        }
        
        double yDist = y2 - y1;
        double xDist = x2 - x1;
        
        if (yDist < 0.00001 || xDist < 0.00001) { 
            startDragX = startDragY = -1;
            return;
        }
        
        //fix ratio
        double originalAspectRatio = modelOuterBounds.getWidth() / modelOuterBounds.getHeight();
        double zoomBoxAspectRatio = xDist / yDist;
        if (zoomBoxAspectRatio > originalAspectRatio) {
            double desiredYDist = yDist * zoomBoxAspectRatio / originalAspectRatio;
            y1 -= (desiredYDist - yDist) / 2;
            y2 += (desiredYDist - yDist) / 2;
        } else {
            double desiredXDist = xDist *  originalAspectRatio / zoomBoxAspectRatio;
            x1 -= (desiredXDist - xDist) / 2;
            x2 += (desiredXDist - xDist) / 2;
        }
        
        modelBounds = new Envelope(x1, x2, y1, y2);
    }
    
    private double toModelY(double y) {
        return map(y, screenBounds.getMinY(), screenBounds.getMaxY(), modelBounds.getMaxY(),
                modelBounds.getMinY());
    }

    private double toModelX(double x) {
        return map(x, screenBounds.getMinX(), screenBounds.getMaxX(), modelBounds.getMinX(),
                modelBounds.getMaxX());
    }

    /**
     * A version of ellipse that takes double args, because apparently Java is too stupid to downgrade automatically. 
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
    }

    public void setHighlighed(Set<Vertex> vertices) {
        highlightedVertices  = vertices;
    }

    public void highlightEdge(Edge selected) {
        highlightedEdge = selected;
    }
}
