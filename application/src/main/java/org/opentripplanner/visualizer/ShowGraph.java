package org.opentripplanner.visualizer;

import java.awt.Point;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.index.strtree.STRtree;
import org.opentripplanner.astar.model.GraphPath;
import org.opentripplanner.astar.model.ShortestPathTree;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalPlace;
import org.opentripplanner.service.vehiclerental.street.StreetVehicleRentalLink;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.ElevatorAlightEdge;
import org.opentripplanner.street.model.edge.ElevatorBoardEdge;
import org.opentripplanner.street.model.edge.FreeEdge;
import org.opentripplanner.street.model.edge.PathwayEdge;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.edge.StreetTransitEntityLink;
import org.opentripplanner.street.model.edge.StreetVehicleParkingLink;
import org.opentripplanner.street.model.edge.VehicleParkingEdge;
import org.opentripplanner.street.model.vertex.ElevatorVertex;
import org.opentripplanner.street.model.vertex.ExitVertex;
import org.opentripplanner.street.model.vertex.IntersectionVertex;
import org.opentripplanner.street.model.vertex.SplitterVertex;
import org.opentripplanner.street.model.vertex.StreetLocation;
import org.opentripplanner.street.model.vertex.TemporaryVertex;
import org.opentripplanner.street.model.vertex.TransitBoardingAreaVertex;
import org.opentripplanner.street.model.vertex.TransitEntranceVertex;
import org.opentripplanner.street.model.vertex.TransitPathwayNodeVertex;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.model.vertex.VehicleParkingEntranceVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.state.State;
import processing.core.PApplet;
import processing.core.PFont;

/**
 * Processing applet to show a map of the graph. The user can: - Use mouse wheel to zoom (or right
 * drag, or ctrl-drag) - Left drag to pan around the map - Left click to send a list of nearby
 * vertices to the associated VertexSelectionListener.
 */
public class ShowGraph extends PApplet implements MouseWheelListener {

  private static final int FRAME_RATE = 30;
  private static final boolean VIDEO = false;
  private static final String VIDEO_PATH = "/home/syncopate/pathimage/";
  private static final DecimalFormat latFormatter = new DecimalFormat("00.0000°N ; 00.0000°S");
  private static final DecimalFormat lonFormatter = new DecimalFormat("000.0000°E ; 000.0000°W");
  /* Layer constants */
  static final int DRAW_MINIMAL = 0; // XY coordinates
  static final int DRAW_HIGHLIGHTED = 1;
  static final int DRAW_SPT = 2;
  static final int DRAW_VERTICES = 3;
  static final int DRAW_TRANSIT = 4;
  static final int DRAW_LINKS = 5;
  static final int DRAW_STREETS = 6;
  static final int DRAW_ALL = 7;
  static final int DRAW_PARTIAL = 8;
  private static double lastLabelY;
  // how many edges to draw before checking whether we need to move on to the next frame
  private final int BLOCK_SIZE = 1000;
  // how many edges to skip over (to ensure a sampling of edges throughout the visible area)
  private final long DECIMATE = 40;
  // 800 instead of 1000 msec, leaving 20% of the time for work other than drawing.
  private final int FRAME_TIME = 800 / FRAME_RATE;
  private final ArrayList<VertexSelectionListener> selectors;
  private final List<Edge> visibleStreetEdges = new ArrayList<>(1000);
  private final List<Edge> visibleLinkEdges = new ArrayList<>(1000);
  private final List<Edge> visibleTransitEdges = new ArrayList<>(1000);
  // these queues are filled by a search in another thread, so must be threadsafe
  private final Queue<Vertex> newHighlightedVertices = new LinkedBlockingQueue<>();
  private final Queue<Edge> newHighlightedEdges = new LinkedBlockingQueue<>();
  private static final DateTimeFormatter shortDateFormat = DateTimeFormatter.ofPattern(
    "HH:mm:ss z"
  );
  private final LinkedBlockingQueue<State> newSPTEdges = new LinkedBlockingQueue<>();
  private final boolean drawEdges = true;
  private int videoFrameNumber = 0;
  Graph graph;
  STRtree vertexIndex;
  STRtree edgeIndex;
  Envelope modelOuterBounds;
  Envelope modelBounds = new Envelope();
  VertexSelectionListener selector;
  private List<Vertex> visibleVertices;
  private List<Vertex> highlightedVertices = new ArrayList<>(1000);
  private List<Edge> highlightedEdges = new ArrayList<>(1000);
  private Coordinate highlightedCoordinate;
  private Edge highlightedEdge;
  private GraphPath highlightedGraphPath;
  protected double mouseModelX;
  protected double mouseModelY;
  private Point startDrag = null;
  private int dragX, dragY;
  private boolean ctrlPressed = false;
  boolean drawFast = false;
  boolean drawStreetEdges = true;
  boolean drawTransitEdges = true;
  boolean drawLinkEdges = true;
  boolean drawStreetVertices = true;
  boolean drawTransitStopVertices = true;
  boolean drawExtraVertices = true;
  private int drawLevel = DRAW_ALL;
  private int drawOffset = 0;
  private boolean drawHighlighted = true;
  public SimpleSPT simpleSPT = new SimpleSPT();
  private LinkedBlockingQueue<SPTNode> sptEdgeQueue;
  private boolean sptVisible = true;
  private float sptFlattening = 0.3f;
  private float sptThickness = 0.1f;
  private boolean drawMultistateVertices = true;
  private ShortestPathTree spt;

  /*
   * Constructor. Call processing constructor, and register the listener to notify when the user selects vertices.
   */
  public ShowGraph(VertexSelectionListener selector, Graph graph) {
    super();
    this.graph = graph;
    this.spt = null;
    this.selector = selector;
    this.selectors = new ArrayList<>();
  }

  /*
   * Setup Processing applet
   */
  public void setup() {
    size(getSize().width, getSize().height, JAVA2D);

    /* Build spatial index of vertices and edges */
    buildSpatialIndex();

    /* Set model bounds to encompass all vertices in the index, and then some */
    modelBounds = (Envelope) (vertexIndex.getRoot().getBounds());
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
    addMouseMotionListener(
      new MouseMotionAdapter() {
        @Override
        public void mouseMoved(MouseEvent e) {
          super.mouseMoved(e);
          Point p = e.getPoint();
          mouseModelX = toModelX(p.x);
          mouseModelY = toModelY(p.y);
        }
      }
    );
    addComponentListener(
      new ComponentAdapter() {
        public void componentResized(ComponentEvent e) {
          matchAspect();
          drawLevel = DRAW_PARTIAL;
        }
      }
    );
    frameRate(FRAME_RATE);
  }

  public synchronized void draw() {
    smooth();
    int startMillis = millis();
    if (drawLevel == DRAW_PARTIAL) {
      drawPartial(startMillis);
    } else if (drawLevel == DRAW_ALL) {
      boolean finished = drawAll(startMillis);
      if (!finished) {
        return;
      }
    } else if (drawLevel == DRAW_LINKS) {
      boolean finished = drawLinks(startMillis);
      if (!finished) {
        return;
      }
    } else if (drawLevel == DRAW_TRANSIT) {
      boolean finished = drawTransit(startMillis);
      if (!finished) {
        return;
      }
    } else if (drawLevel == DRAW_VERTICES) {
      drawVertices();
    } else if (drawLevel == DRAW_SPT) {
      boolean finished = drawSPT();
      if (!finished) {
        return;
      }
    } else if (drawLevel == DRAW_HIGHLIGHTED) {
      drawHighlighted();
    } else if (drawLevel == DRAW_MINIMAL) {
      if (!newHighlightedEdges.isEmpty()) handleNewHighlights();
      drawNewEdges();
      drawCoords();
    }
    drawOffset = 0;
    if (drawLevel > DRAW_MINIMAL) drawLevel -= 1; // move to next layer
  }

  public void redraw() {
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
      double tx = (modelBounds.getWidth() * dx) / getWidth();
      double ty = (modelBounds.getHeight() * dy) / getHeight();
      modelBounds.translate(tx, ty);
    }
    dragX = c.x;
    dragY = c.y;
    drawLevel = DRAW_PARTIAL;
  }

  /*
   * Zoom in/out proportional to the number of clicks of the mouse wheel.
   */
  public void mouseWheelMoved(MouseWheelEvent e) {
    double f = e.getWheelRotation() * 0.2;
    zoom(f, e.getPoint());
  }

  @SuppressWarnings("unchecked")
  public void mouseClicked() {
    Envelope screenEnv = new Envelope(new Coordinate(mouseX, mouseY));
    screenEnv.expandBy(4, 4);
    Envelope env = new Envelope(
      toModelX(screenEnv.getMinX()),
      toModelX(screenEnv.getMaxX()),
      toModelY(screenEnv.getMinY()),
      toModelY(screenEnv.getMaxY())
    );

    List<Vertex> nearby = (List<Vertex>) vertexIndex.query(env);
    selector.verticesSelected(nearby);
    drawLevel = DRAW_ALL;
  }

  public void keyPressed() {
    if (key == CODED && keyCode == CONTROL) ctrlPressed = true;
  }

  public void keyReleased() {
    if (key == CODED && keyCode == CONTROL) ctrlPressed = false;
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
    matchAspect();
    drawLevel = DRAW_ALL;
  }

  public void zoomToVertex(Vertex v) {
    Envelope e = new Envelope();
    e.expandToInclude(v.getCoordinate());
    e.expandBy(0.002);
    modelBounds = e;
    drawLevel = DRAW_ALL;
  }

  /**
   * Zoom to an envelope. Used for issue zoom.
   *
   * @author mattwigway
   */
  public void zoomToEnvelope(Envelope e) {
    modelBounds = e;
    matchAspect();
    drawLevel = DRAW_ALL;
  }

  /*
   * Iterate through all vertices and their (outgoing) edges. If they are of 'interesting' types,
   * add them to the corresponding spatial index.
   */
  public synchronized void buildSpatialIndex() {
    vertexIndex = new STRtree();
    edgeIndex = new STRtree();
    Envelope env;

    // int xminx, xmax, ymin, ymax;
    for (Vertex v : graph.getVertices()) {
      Coordinate c = v.getCoordinate();
      env = new Envelope(c);
      vertexIndex.insert(env, v);
      for (Edge e : v.getOutgoing()) {
        var edgeGeometry = e.getGeometry();
        if (edgeGeometry == null) {
          edgeIndex.insert(
            new Envelope(e.getFromVertex().getCoordinate(), e.getToVertex().getCoordinate()),
            e
          );
        } else {
          edgeIndex.insert(edgeGeometry.getEnvelopeInternal(), e);
        }
      }
    }
    vertexIndex.build();
    edgeIndex.build();
  }

  /**
   * Set the Vertex selector to newSelector, and store the old selector on the stack of selectors
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

  public void highlightCoordinate(Coordinate c) {
    double xd = 0, yd = 0;
    while (!modelBounds.contains(c)) {
      xd = modelBounds.getWidth() / 100;
      yd = modelBounds.getHeight() / 100;
      modelBounds.expandBy(xd, yd);
    }
    modelBounds.expandBy(xd, yd);
    highlightedCoordinate = c;
    drawLevel = DRAW_ALL;
  }

  public void highlightVertex(Vertex v) {
    highlightCoordinate(v.getCoordinate());
  }

  public void enqueueHighlightedEdge(Edge de) {
    newHighlightedEdges.add(de);
  }

  public void clearHighlights() {
    highlightedEdges.clear();
    highlightedVertices.clear();
    drawLevel = DRAW_ALL;
  }

  public void highlightEdge(Edge selected) {
    highlightedEdge = selected;
    drawLevel = DRAW_ALL;
  }

  public void highlightGraphPath(GraphPath gp) {
    highlightedGraphPath = gp;
    // drawLevel = DRAW_ALL;
    drawLevel = DRAW_TRANSIT; // leave streets in grey
  }

  public void setHighlightedVertices(Set<Vertex> vertices) {
    highlightedVertices = new ArrayList<>(vertices);
    drawLevel = DRAW_ALL;
  }

  public void setHighlightedVertices(List<Vertex> vertices) {
    highlightedVertices = vertices;
    drawLevel = DRAW_ALL;
  }

  public void setHighlightedEdges(List<Edge> edges) {
    highlightedEdges = edges;
    drawLevel = DRAW_ALL;
  }

  public void drawIssue(DataImportIssue anno) {
    Envelope env = new Envelope();

    Edge e = anno.getReferencedEdge();
    if (e != null) {
      this.enqueueHighlightedEdge(e);
      env.expandToInclude(e.getFromVertex().getCoordinate());
      env.expandToInclude(e.getToVertex().getCoordinate());
    }

    ArrayList<Vertex> vertices = new ArrayList<>();
    Vertex v = anno.getReferencedVertex();
    if (v != null) {
      env.expandToInclude(v.getCoordinate());
      vertices.add(v);
    }

    if (e == null && v == null) return;

    // make it a little bigger, especially needed for STOP_UNLINKED
    env.expandBy(0.02);

    // highlight relevant things
    this.clearHighlights();
    this.setHighlightedVertices(vertices);

    // zoom the graph display
    this.zoomToEnvelope(env);

    // and draw
    this.draw();
  }

  public void setShowTransit(boolean selected) {
    drawTransitEdges = selected;
    drawTransitStopVertices = selected;
  }

  public void setShowStreets(boolean selected) {
    drawStreetEdges = selected;
    drawStreetVertices = selected;
  }

  public void setShowHightlights(boolean selected) {
    drawHighlighted = selected;
  }

  public void addNewSPTEdge(State state) {
    this.newSPTEdges.add(state);
    this.simpleSPT.add(state);
  }

  public void resetSPT() {
    this.simpleSPT = new SimpleSPT();
  }

  public void setShowSPT(boolean selected) {
    sptVisible = selected;
  }

  public void setSPTFlattening(float sptFlattening) {
    this.sptFlattening = sptFlattening;
  }

  public void setSPTThickness(float sptThickness) {
    this.sptThickness = sptThickness;
  }

  public void setShowMultistateVertices(boolean selected) {
    this.drawMultistateVertices = selected;
  }

  public void setSPT(ShortestPathTree spt) {
    this.spt = spt;
  }

  /*
   * Zoom in/out. Translate the viewing window such that the place under the mouse pointer is a fixed point. If p is null, zoom around the center of
   * the viewport.
   */
  void zoom(double f, Point p) {
    double ex = modelBounds.getWidth() * f;
    double ey = modelBounds.getHeight() * f;
    modelBounds.expandBy(ex / 2, ey / 2);
    if (p != null) {
      // Note: Graphics Y coordinates increase down the screen, hence the opposite signs.
      double tx = ex * -((p.getX() / this.width) - 0.5);
      double ty = ey * +((p.getY() / this.height) - 0.5);
      modelBounds.translate(tx, ty);
    }
    // update the display
    drawLevel = DRAW_PARTIAL;
  }

  void matchAspect() {
    /* Basic sinusoidal projection of lat/lon data to square pixels */
    double yCenter = modelBounds.centre().y;
    float xScale = cos(radians((float) yCenter));
    double newX =
      modelBounds.getHeight() * (1 / xScale) * ((float) this.getWidth() / this.getHeight());
    modelBounds.expandBy((newX - modelBounds.getWidth()) / 2f, 0);
  }

  private static LineString getOrCreateGeometry(Edge edge) {
    var edgeGeometry = edge.getGeometry();
    if (edgeGeometry != null) {
      return edgeGeometry;
    }

    Coordinate[] coordinates = new Coordinate[] {
      edge.getFromVertex().getCoordinate(),
      edge.getToVertex().getCoordinate(),
    };
    return GeometryUtils.getGeometryFactory().createLineString(coordinates);
  }

  @SuppressWarnings("unchecked")
  private synchronized void findVisibleElements() {
    visibleVertices = (List<Vertex>) vertexIndex.query(modelBounds);
    visibleStreetEdges.clear();
    visibleLinkEdges.clear();
    visibleTransitEdges.clear();
    for (Edge de : (Iterable<Edge>) edgeIndex.query(modelBounds)) {
      if (
        de instanceof PathwayEdge ||
        de instanceof VehicleParkingEdge ||
        de instanceof StreetTransitEntityLink ||
        de instanceof FreeEdge ||
        de instanceof StreetVehicleParkingLink ||
        de instanceof StreetVehicleRentalLink
      ) {
        visibleLinkEdges.add(de);
      } else if (
        de instanceof StreetEdge ||
        de instanceof ElevatorAlightEdge ||
        de instanceof ElevatorBoardEdge
      ) {
        visibleStreetEdges.add(de);
      }
    }
  }

  private int drawEdge(Edge e) {
    var geometry = getOrCreateGeometry(e);
    Coordinate[] coords = geometry.getCoordinates();
    beginShape();
    for (Coordinate coord : coords) {
      vertex((float) toScreenX(coord.x), (float) toScreenY(coord.y));
    }
    endShape();
    return coords.length; // should be used to count segments, not edges drawn
  }

  /* use endpoints instead of geometry for quick updating */
  private void drawEdgeFast(Edge e) {
    Coordinate[] coords = getOrCreateGeometry(e).getCoordinates();
    Coordinate c0 = coords[0];
    Coordinate c1 = coords[coords.length - 1];
    line(
      (float) toScreenX(c0.x),
      (float) toScreenY(c0.y),
      (float) toScreenX(c1.x),
      (float) toScreenY(c1.y)
    );
  }

  private void drawGraphPath(GraphPath<State, Edge, Vertex> gp) {
    // draw edges in different colors according to mode
    for (State s : gp.states) {
      TraverseMode mode = s.getBackMode();

      Edge e = s.getBackEdge();
      if (e == null) continue;

      // TODO Add support for crating transit edges on the fly
      //      if (mode != null && mode.isTransit()) {
      //        stroke(200, 050, 000);
      //        strokeWeight(6);
      //        drawEdge(e);
      //      }
      if (e instanceof StreetEdge) {
        StreetTraversalPermission stp = ((StreetEdge) e).getPermission();
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
    lastLabelY = -999;
    labelState(gp.states.getFirst(), "begin");
    labelState(gp.states.getLast(), "end");

    if (VIDEO) {
      // freeze on final path for a few frames
      for (int i = 0; i < 10; i++) saveVideoFrame();
      resetVideoFrameNumber();
    }
  }

  private void labelState(State s, String str) {
    fill(240, 240, 240);
    Vertex v = s.getVertex();
    drawVertex(v, 8);
    str += " " + shortDateFormat.format(Instant.ofEpochSecond(s.getTimeSeconds()));
    str += " [" + (int) s.getWeight() + "]";
    double x = toScreenX(v.getX()) + 10;
    double y = toScreenY(v.getY());
    double dy = y - lastLabelY;
    if (dy == 0) {
      y = lastLabelY + 20;
    } else if (Math.abs(dy) < 20) {
      y = lastLabelY + Math.signum(dy) * 20;
    }
    text(str, (float) x, (float) y);
    lastLabelY = y;
  }

  private void drawCoordinate(Coordinate c, double r) {
    noStroke();
    ellipse(toScreenX(c.x), toScreenY(c.y), r, r);
  }

  private void drawVertex(Vertex v, double r) {
    drawCoordinate(v.getCoordinate(), r);
  }

  private boolean drawSPT() {
    if (!sptVisible) {
      return true;
    }

    noFill();
    //		if(sptEdgeQueue==null){
    //			sptEdgeQueue = simpleSPT.getEdgeQueue();
    //		}

    //		colorOverlappingBranches(sptEdgeQueue);
    //
    //		int i=0;
    //		while(!sptEdgeQueue.isEmpty()){
    //			SPTNode node = sptEdgeQueue.poll();
    //			i++;
    //			node.draw(sptBranchColors);
    //    		if ((i%BLOCK_SIZE==0) && (millis() - startMillis > FRAME_TIME))
    //    			return false;
    //    	}
    //    	sptEdgeQueue=null;

    simpleSPT.draw();

    return true;
  }

  private void colorOverlappingBranches(LinkedBlockingQueue<SPTNode> queue) {
    HashMap<Vertex, Integer> stateHeight = new HashMap<>();

    for (SPTNode node : queue) {
      Integer height = stateHeight.get(node.state.getVertex());
      if (height == null) {
        height = 0;
      } else {
        height += 1;
      }
      stateHeight.put(node.state.getVertex(), height);

      node.setHeight(height);
    }
  }

  private void drawNewEdges() {
    if (drawEdges) {
      strokeWeight(1);
      stroke(255, 255, 255); //white
      noFill();
      while (!newSPTEdges.isEmpty()) {
        State leaf = newSPTEdges.poll();

        if (leaf != null) {
          if (leaf.getBackEdge() != null) {
            drawEdge(leaf.getBackEdge());
          }
        }
      }
    }
  }

  private void drawCoords() {
    // Black background box
    fill(0, 0, 0);
    stroke(30, 128, 30);
    // noStroke();
    strokeWeight(1);
    rect(3, 3, 303, textAscent() + textDescent() + 6);
    // Print lat & lon coordinates
    fill(128, 128, 256);
    // noStroke();
    String output = lonFormatter.format(mouseModelX) + " " + latFormatter.format(mouseModelY);
    textAlign(LEFT, TOP);
    text(output, 6, 6);
  }

  private void drawVertices() {
    /* turn off vertex display when zoomed out */
    final double METERS_PER_DEGREE_LAT = 111111.111111;
    boolean closeEnough = ((modelBounds.getHeight() * METERS_PER_DEGREE_LAT) / this.width < 5);
    /* Draw selected visible vertices */
    for (Vertex v : visibleVertices) {
      if (
        drawTransitStopVertices &&
        closeEnough &&
        (v instanceof TransitStopVertex ||
          v instanceof TransitPathwayNodeVertex ||
          v instanceof TransitEntranceVertex ||
          v instanceof TransitBoardingAreaVertex)
      ) {
        fill(60, 60, 200); // Make transit stops blue dots
        drawVertex(v, 7);
      }
      if (
        drawExtraVertices &&
        closeEnough &&
        (v instanceof VehicleParkingEntranceVertex || v instanceof VehicleRentalPlace)
      ) {
        fill(255, 70, 255); // Make B+R/P+R pink
        drawVertex(v, 7);
      }
      if (
        drawStreetVertices &&
        ((v instanceof IntersectionVertex && ((IntersectionVertex) v).hasCyclingTrafficLight()) ||
          (v instanceof ElevatorVertex ||
            v instanceof ExitVertex ||
            v instanceof TemporaryVertex ||
            v instanceof SplitterVertex ||
            v instanceof StreetLocation))
      ) {
        if (v instanceof IntersectionVertex && ((IntersectionVertex) v).hasCyclingTrafficLight()) {
          fill(120, 60, 60); // Make traffic lights red dots
          drawVertex(v, 5);
        }
      }
      if (drawMultistateVertices && spt != null) {
        List<? extends State> states = spt.getStates(v);
        if (states != null) {
          fill(100, 60, 100);
          drawVertex(v, states.size() * 2);
        }
      }
    }
  }

  private void drawHighlighted() {
    /* Draw highlighted edges in another color */
    noFill();
    stroke(200, 200, 000, 16); // yellow transparent edge highlight
    strokeWeight(8);
    if (drawHighlighted && highlightedEdges != null) {
      try {
        for (Edge e : highlightedEdges) {
          drawEdge(e);
        }
      } catch (ConcurrentModificationException cme) {
        // The edge list was cleared or added to while it was being drawn, no harm done.
      }
    }
    /* Draw highlighted graph path in another color */
    if (highlightedGraphPath != null) {
      drawGraphPath(highlightedGraphPath);
    }
    /* Draw (single) highlighted edge in highlight color */
    if (highlightedEdge != null) {
      stroke(10, 200, 10, 128);
      strokeWeight(12);
      drawEdge(highlightedEdge);
    }
    /* Draw highlighted vertices */
    fill(255, 127, 0); // orange fill
    noStroke();
    if (highlightedVertices != null) {
      for (Vertex v : highlightedVertices) {
        drawVertex(v, 8);
      }
    }
    /* Draw (single) highlighed coordinate in a different color */
    if (highlightedCoordinate != null) {
      fill(255, 255, 30);
      drawCoordinate(highlightedCoordinate, 7);
    }
    noFill();
  }

  private boolean drawTransit(int startMillis) {
    if (drawTransitEdges) {
      stroke(40, 40, 128, 30); // transparent blue
      strokeWeight(4);
      noFill();
      // for (Edge e : visibleTransitEdges) {
      while (drawOffset < visibleTransitEdges.size()) {
        Edge e = visibleTransitEdges.get(drawOffset);
        drawEdge(e);
        drawOffset += 1;
        if (drawOffset % BLOCK_SIZE == 0) {
          if (millis() - startMillis > FRAME_TIME) {
            return false;
          }
        }
      }
    }
    return true;
  }

  private boolean drawLinks(int startMillis) {
    if (drawLinkEdges) {
      stroke(256, 165, 0, 30); // transparent blue
      strokeWeight(3);
      noFill();
      // for (Edge e : visibleTransitEdges) {
      while (drawOffset < visibleLinkEdges.size()) {
        Edge e = visibleLinkEdges.get(drawOffset);
        drawEdge(e);
        drawOffset += 1;
        if (drawOffset % BLOCK_SIZE == 0) {
          if (millis() - startMillis > FRAME_TIME) {
            return false;
          }
        }
      }
    }
    return true;
  }

  private boolean drawAll(int startMillis) {
    if (drawOffset == 0) {
      findVisibleElements();
      background(15);
    }
    if (drawStreetEdges) {
      stroke(30, 128, 30); // dark green
      strokeWeight(1);
      noFill();
      while (drawOffset < visibleStreetEdges.size()) {
        drawEdge(visibleStreetEdges.get(drawOffset));
        drawOffset += 1;
        if (drawOffset % BLOCK_SIZE == 0) {
          if (millis() - startMillis > FRAME_TIME) {
            return false;
          }
        }
      }
    }
    return true;
  }

  private void drawPartial(int startMillis) {
    background(15);
    stroke(30, 128, 30);
    strokeWeight(1);
    noFill();
    // noSmooth();
    int drawIndex = 0;
    int drawStart = 0;
    int drawCount = 0;
    while (drawStart < DECIMATE && drawStart < visibleStreetEdges.size()) {
      if (drawFast) drawEdgeFast(visibleStreetEdges.get(drawIndex));
      else drawEdge(visibleStreetEdges.get(drawIndex));
      drawIndex += DECIMATE;
      drawCount += 1;
      if (drawCount % BLOCK_SIZE == 0 && millis() - startMillis > FRAME_TIME) {
        // ran out of time to draw this frame.
        // enable fast-drawing when too few edges were drawn:
        // drawFast = drawCount < visibleStreetEdges.size() / 10;
        // leave edge drawing loop to let other work happen.
        break;
      }
      if (drawIndex >= visibleStreetEdges.size()) {
        // start over drawing every DECIMATEth edge, offset by 1
        drawStart += 1;
        drawIndex = drawStart;
      }
    }
  }

  private void handleNewHighlights() {
    // fill(0, 0, 0, 1);
    // rect(0,0,this.width, this.height);
    desaturate();
    noFill();
    stroke(256, 0, 0, 128); // , 8);
    strokeWeight(6);
    while (!newHighlightedEdges.isEmpty()) {
      Edge de = newHighlightedEdges.poll();
      if (de != null) {
        drawEdge(de);
        highlightedEdges.add(de);
      }
    }
    if (VIDEO) saveVideoFrame();
  }

  private void saveVideoFrame() {
    save(VIDEO_PATH + "/" + videoFrameNumber++ + ".bmp");
  }

  private void resetVideoFrameNumber() {
    videoFrameNumber = 0;
  }

  private void desaturate() {
    final float f = 8;
    loadPixels();
    for (int i = 0; i < width * height; i++) {
      int c = pixels[i];
      float r = red(c);
      float g = green(c);
      float b = blue(c);
      float avg = (r + g + b) / 3;
      r += (avg - r) / f;
      g += (avg - g) / f;
      b += (avg - b) / f;
      pixels[i] = color(r, g, b);
    }
    updatePixels();
  }

  private double toScreenY(double y) {
    return map(
      (float) y,
      (float) modelBounds.getMinY(),
      (float) modelBounds.getMaxY(),
      getSize().height,
      0
    );
  }

  private double toScreenX(double x) {
    return map(
      (float) x,
      (float) modelBounds.getMinX(),
      (float) modelBounds.getMaxX(),
      0,
      getSize().width
    );
  }

  private double toModelY(double y) {
    return map(
      (float) y,
      0,
      getSize().height,
      (float) modelBounds.getMaxY(),
      (float) modelBounds.getMinY()
    );
  }

  private double toModelX(double x) {
    return map(
      (float) x,
      0,
      getSize().width,
      (float) modelBounds.getMinX(),
      (float) modelBounds.getMaxX()
    );
  }

  /**
   * A version of ellipse that takes double args, because apparently Java is too stupid to downgrade
   * automatically.
   */
  private void ellipse(double d, double e, double f, double g) {
    ellipse((float) d, (float) e, (float) f, (float) g);
  }

  static class Trunk {

    public Edge edge;
    public Double trunkiness;

    Trunk(Edge edge, Double trunkiness) {
      this.edge = edge;
      this.trunkiness = trunkiness;
    }
  }

  class SimpleSPT {

    private final HashMap<State, SPTNode> nodes;
    SPTNode root;

    SimpleSPT() {
      nodes = new HashMap<>();
    }

    public void add(State state) {
      // create simpleSPT entry
      SPTNode curNode = new SPTNode(state);
      SPTNode parentNode = this.nodes.get(state.getBackState());
      if (parentNode != null) {
        parentNode.children.add(curNode);
      } else {
        root = curNode;
      }
      curNode.parent = parentNode;
      this.nodes.put(state, curNode);
    }

    public void draw() {
      if (root == null) {
        return;
      }

      HashMap<Vertex, Integer> vertexHeight = new HashMap<>();

      root.drawRecursive(0, vertexHeight);
    }

    public LinkedBlockingQueue<SPTNode> getEdgeQueue() {
      LinkedBlockingQueue<SPTNode> ret = new LinkedBlockingQueue<>();
      if (root != null) {
        root.addToEdgeQueue(ret);
      }
      return ret;
    }

    void setWeights() {
      if (root == null) {
        return;
      }
      root.setWeight();
    }
  }

  class SPTNode {

    // this is a tool for the traverse visitor to build a very simple
    // shortest path tree, which we can use to come up with the trunkiness
    // of every SPT edge.

    State state;
    SPTNode parent;
    List<SPTNode> children;
    double weight = 0.0;
    public Integer height;

    SPTNode(State state) {
      this.state = state;
      this.height = null;
      this.children = new ArrayList<>();
    }

    public void addToEdgeQueue(LinkedBlockingQueue<SPTNode> ret) {
      ret.add(this);
      for (SPTNode child : children) {
        child.addToEdgeQueue(ret);
      }
    }

    public void drawRecursive(int height, HashMap<Vertex, Integer> vertexStatesEncountered) {
      colorMode(HSB);

      // get the number of states we've already drawn from this vertex
      Integer vertexHeight = vertexStatesEncountered.get(this.state.getVertex());
      if (vertexHeight == null) {
        vertexHeight = 0;
      }

      // if it's larger than the 'height' of the state we're about to draw, bump the state's visual height
      // up to the number of states it has to climb over
      if (vertexHeight > height) {
        height = vertexHeight;
      }

      // increment the counter of the number of times we've encountered this vertex
      vertexStatesEncountered.put(this.state.getVertex(), vertexHeight + 1);

      if (state.getBackEdge() != null) {
        //stroke( colorRamp( (int)(state.getWeight()/10.0) ) );
        stroke(color((height * 10) % 255, 255, 255));

        strokeWeight((float) (sptThickness * Math.pow(weight, sptFlattening)));
        drawEdge(state.getBackEdge());
      }

      for (SPTNode child : children) {
        child.drawRecursive(height, vertexStatesEncountered);
      }

      colorMode(RGB);
    }

    public void draw(List<Integer> colors) {
      colorMode(HSB);

      if (state.getBackEdge() != null) {
        //stroke( colorRamp( (int)(state.getWeight()/10.0) ) );
        strokeWeight((float) (sptThickness * Math.pow(weight, sptFlattening)));

        stroke(colors.get(this.height));

        drawEdge(state.getBackEdge());
      }

      colorMode(RGB);
    }

    public void setWeight() {
      weight = state.getWeight();
      for (SPTNode child : children) {
        child.setWeight();
        weight += child.weight;
      }
    }

    public void setHeight(Integer height) {
      this.height = height;
    }

    void addChild(SPTNode child) {
      this.children.add(child);
    }

    private int colorRamp(int aa) {
      int NHUES = 6;
      int HUELEN = 256;
      int RAMPLEN = NHUES * HUELEN;
      int BRIGHTNESS = 220;

      aa = aa % RAMPLEN; //make sure aa fits within the color ramp
      int hueIndex = aa / HUELEN; //establish the hue
      int hue = hueIndex * (HUELEN / NHUES); //convert that to a hue value
      int saturation = HUELEN - (aa % HUELEN);

      return color(hue, saturation, BRIGHTNESS);
    }
  }
}
