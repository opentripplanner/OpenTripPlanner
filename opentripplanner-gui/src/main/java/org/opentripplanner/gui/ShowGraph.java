package org.opentripplanner.gui;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.impl.GraphSerializationLibrary;
import org.opentripplanner.routing.vertextypes.Intersection;
import org.opentripplanner.routing.vertextypes.TransitStop;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.index.strtree.STRtree;

import processing.core.PApplet;

public class ShowGraph extends PApplet {

    private static final long serialVersionUID = -8336165356756970127L;

    Graph graph;

    STRtree vertexIndex;

    /*
     * static public void main(String args[]) { PApplet.main(new String[] {"ShowGraph"}); }
     */

    Envelope modelOuterBounds;

    Envelope modelBounds = new Envelope();

    Envelope screenBounds;

    VertexSelector selector;

    private String graphObj;
    
    public ShowGraph(VertexSelector selector, String graphObj) {
        super();
        this.selector = selector;
        this.graphObj = graphObj;
    }
    
    public void setup() {
        screenBounds = new Envelope(0, 700, 0, 700);
        background(0);
        size(700, 700, P2D);

        try {
            graph = GraphSerializationLibrary.readGraph(new File(
                    graphObj));
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        vertexIndex = new STRtree();
        for (Vertex v : graph.getVertices()) {
            Envelope env = new Envelope(v.getCoordinate());
            if (v.getType() == TransitStop.class) {
                modelBounds.expandToInclude(env);
                vertexIndex.insert(env, v);
            } else if(v.getType() == Intersection.class) { 
                vertexIndex.insert(env, v);
            } else {
                //a street-transit link, or a journey vertex.  no need for them in the ui.
            }
        }
        modelBounds.expandBy(0.02);
        modelOuterBounds = new Envelope(modelBounds);
        vertexIndex.build();
    }

    public void zoomToDefault() {
        modelBounds = new Envelope(modelOuterBounds);
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
            if (v.getType() == TransitStop.class) {
                stroke(255, 30, 255);
                ellipse(x - 2, y - 2, 5.0, 5.0);
            } else {
                stroke(255);
                ellipse(x - 1, y - 1, 3.0, 3.0);
            }
            
        }
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
        double x = toModelX(mouseX);
        double y = toModelY(mouseY);
        System.out.println ("x, y: " + x + ", " + y);
        Envelope env = new Envelope(new Coordinate(x, y));
        env.expandBy(0.001);
        List<Vertex> nearby = (List<Vertex>) vertexIndex.query(env);
        selector.verticesSelected(nearby);
    }

    int startDragX, startDragY;
    
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

    private void ellipse(double d, double e, double f, double g) {
        ellipse((float) d, (float) e, (float) f, (float) g);
    }
}
