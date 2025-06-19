package org.opentripplanner.visualizer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import javax.swing.AbstractListModel;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.ListModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.api.common.LocationStringParser;
import org.opentripplanner.api.parameter.ApiRequestMode;
import org.opentripplanner.api.parameter.QualifiedModeSet;
import org.opentripplanner.astar.model.GraphPath;
import org.opentripplanner.astar.model.ShortestPathTree;
import org.opentripplanner.astar.spi.DominanceFunction;
import org.opentripplanner.astar.spi.TraverseVisitor;
import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.RouteRequestBuilder;
import org.opentripplanner.routing.core.VehicleRoutingOptimizeType;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.GraphPathFinder;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.vertex.IntersectionVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.model.vertex.VertexLabel;
import org.opentripplanner.street.search.TemporaryVerticesContainer;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.strategy.DominanceFunctions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Exit on window close.
 */
class ExitListener extends WindowAdapter {

  public void windowClosing(WindowEvent event) {
    System.exit(0);
  }
}

/**
 * DisplayVertex holds a vertex, but has a toString value that's a little more useful.
 */
class DisplayVertex {

  public Vertex vertex;

  public DisplayVertex(Vertex v) {
    vertex = v;
  }

  public String toString() {
    String label = vertex.getLabelString();
    if (label.contains("osm node")) {
      label = vertex.getDefaultName();
    }
    return label;
  }
}

/**
 * This is a ListModel that holds Edges. It gets its edges from a PatternBoard/PatternAlight, hence
 * the iterable.
 */
class EdgeListModel extends AbstractListModel<Edge> {

  private final ArrayList<Edge> edges;

  EdgeListModel(Iterable<Edge> edges) {
    this.edges = new ArrayList<>();
    for (Edge e : edges) {
      this.edges.add(e);
    }
  }

  public int getSize() {
    return edges.size();
  }

  public Edge getElementAt(int index) {
    return edges.get(index);
  }
}

/**
 * A list of vertices where the internal container is exposed.
 */
class VertexList extends AbstractListModel<DisplayVertex> {

  public List<Vertex> selected;

  VertexList(List<Vertex> selected) {
    this.selected = selected;
  }

  public int getSize() {
    return selected.size();
  }

  public DisplayVertex getElementAt(int index) {
    return new DisplayVertex(selected.get(index));
  }
}

/**
 * A simple visualizer for graphs. It shows (using ShowGraph) a map of the graph, intersections and
 * TransitStops only, and allows a user to select stops, examine incoming and outgoing edges, and
 * examine trip patterns. It's meant mainly for debugging, so it's totally OK if it develops (say) a
 * bunch of weird buttons designed to debug specific cases.
 * <p>
 * 2024-01-26: We talked about the visualizer in the developer meeting and while the code is a bit
 * dusty, we decided that we want to keep the option open to build make the visualization of routing
 * steps work again in the future and won't delete it.
 */
public class GraphVisualizer extends JFrame implements VertexSelectionListener {

  private static final Logger LOG = LoggerFactory.getLogger(GraphVisualizer.class);
  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern(
    "yyyy-MM-dd HH:mm:ss z"
  );
  public static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss z");

  /* The graph from the router we are visualizing, note that it will not be updated if the router reloads. */
  private final Graph graph;
  private JPanel leftPanel;

  /* The Processing applet that actually displays the graph. */
  private ShowGraph showGraph;

  /* The set of callbacks that display search progress on the showGraph Processing applet. */
  public TraverseVisitor<State, Edge> traverseVisitor;

  public JList<DisplayVertex> nearbyVertices;

  private JList<Edge> outgoingEdges;

  private JList<Edge> incomingEdges;

  private JTextField sourceVertex;

  private JTextField sinkVertex;

  private JCheckBox walkCheckBox;

  private JCheckBox bikeCheckBox;

  private JCheckBox trainCheckBox;

  private JCheckBox busCheckBox;

  private JCheckBox ferryCheckBox;

  private JCheckBox transitCheckBox;

  private JCheckBox carCheckBox;

  private JTextField searchDate;
  private JTextField boardingPenaltyField;
  private DefaultListModel<DataImportIssue> issueMatchesModel;
  private JList<DataImportIssue> issueMatches;
  private DefaultListModel<String> metadataModel;
  private HashSet<Vertex> closed;
  private Vertex tracingVertex;
  private HashSet<Vertex> open;
  private HashSet<Vertex> seen;
  private JList<String> metadataList;
  private JRadioButton opQuick;
  private JRadioButton opSafe;
  private JRadioButton opFlat;
  private JRadioButton opGreenways;
  private ButtonGroup optimizeTypeGrp;
  private JTextField maxWalkField;
  private JTextField walkSpeed;
  private JTextField bikeSpeed;
  private JTextField heuristicWeight;
  private JCheckBox softWalkLimiting;
  private JTextField softWalkPenalty;
  private JTextField softWalkOverageRate;
  private JCheckBox arriveByCheckBox;
  private JLabel searchTimeElapsedLabel;
  private JCheckBox dontUseGraphicalCallbackCheckBox;
  private JTextField nPaths;
  private JList<PathPrinter> pathsList;
  private JList<State> pathStates;
  private JCheckBox showTransitCheckbox;
  private JCheckBox showStreetsCheckbox;
  private JCheckBox showMultistateVerticesCheckbox;
  private JCheckBox showHighlightedCheckbox;
  private JCheckBox showSPTCheckbox;
  private ShortestPathTree<State, Edge, Vertex> spt;
  private JTextField sptFlattening;
  private JTextField sptThickness;
  private JPopupMenu popup;
  private GraphPath<State, Edge, Vertex> firstComparePath;
  private GraphPath<State, Edge, Vertex> secondComparePath;
  private JList<State> firstComparePathStates;
  private JList<State> secondComparePathStates;
  private JList<String> secondStateData;
  private JList<String> firstStateData;
  protected State lastStateClicked = null;
  private JCheckBox longDistanceModeCheckbox;

  public GraphVisualizer(Graph graph) {
    super();
    setTitle("GraphVisualizer");
    setExtendedState(JFrame.MAXIMIZED_BOTH);
    this.graph = graph;
  }

  public void run() {
    LOG.info("Starting up graph visualizer...");
    this.init();
    this.setVisible(true);
  }

  public void init() {
    final JTabbedPane tabbedPane = new JTabbedPane();

    final Container mainTab = makeMainTab();
    Container prefsPanel = makePrefsPanel();
    Container diffTab = makeDiffTab();

    tabbedPane.addTab("Main", null, mainTab, "Pretty much everything");

    tabbedPane.addTab("Prefs", null, prefsPanel, "Routing preferences");

    tabbedPane.addTab("Diff", null, diffTab, "multistate path diffs");

    //Add the tabbed pane to this panel.
    add(tabbedPane);

    //The following line enables to use scrolling tabs.
    tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

    // startup the graphical pane; ensure closing works; draw the window
    showGraph.init();
    addWindowListener(new ExitListener());
    pack();

    // make sure the showGraph quits drawing when we switch tabs
    tabbedPane.addChangeListener(
      new ChangeListener() {
        @Override
        public void stateChanged(ChangeEvent e) {
          if (tabbedPane.getSelectedComponent().equals(mainTab)) {
            showGraph.loop();
          } else {
            showGraph.noLoop();
          }
        }
      }
    );
  }

  public void verticesSelected(final List<Vertex> selected) {
    // sort vertices by name
    Collections.sort(
      selected,
      new Comparator<>() {
        @Override
        public int compare(Vertex arg0, Vertex arg1) {
          return arg0.getLabelString().compareTo(arg1.getLabelString());
        }
      }
    );
    ListModel<DisplayVertex> data = new VertexList(selected);
    nearbyVertices.setModel(data);

    // pick out an intersection vertex and find the path
    // if the spt is already available
    Vertex target = null;
    for (Vertex vv : selected) {
      if (vv instanceof IntersectionVertex) {
        target = vv;
        break;
      }
    }
    if (target != null && spt != null) {
      List<GraphPath<State, Edge, Vertex>> paths = spt.getPaths(target);
      showPathsInPanel(paths);
    }
  }

  public Graph getGraph() {
    return graph;
  }

  protected JComponent makeTextPanel(String text) {
    JPanel panel = new JPanel(false);
    JLabel filler = new JLabel(text);
    filler.setHorizontalAlignment(JLabel.CENTER);
    panel.setLayout(new GridLayout(1, 1));
    panel.add(filler);
    return panel;
  }

  protected void trace() {
    DisplayVertex selected = (DisplayVertex) nearbyVertices.getSelectedValue();
    if (selected == null) {
      return;
    }
    Vertex v = selected.vertex;

    if (tracingVertex != v) {
      tracingVertex = v;
      closed = new HashSet<>();
      open = new HashSet<>();
      open.add(v);
      seen = new HashSet<>();
    }
    HashSet<Vertex> newOpen = new HashSet<>();
    for (Vertex v2 : open) {
      closed.add(v2);
      for (Edge e : v2.getOutgoing()) {
        Vertex target = e.getToVertex();
        if (closed.contains(target)) {
          continue;
        }
        newOpen.add(target);
      }
    }
    seen.addAll(newOpen);
    open = newOpen;
    showGraph.setHighlightedVertices(seen);
  }

  protected void traceOld() {
    HashSet<Vertex> seenVertices = new HashSet<>();
    DisplayVertex selected = (DisplayVertex) nearbyVertices.getSelectedValue();
    if (selected == null) {
      System.out.println("no vertex selected");
      return;
    }
    Vertex v = selected.vertex;
    System.out.println("initial vertex: " + v);
    Queue<Vertex> toExplore = new LinkedList<>();
    toExplore.add(v);
    seenVertices.add(v);
    while (!toExplore.isEmpty()) {
      Vertex src = toExplore.poll();
      for (Edge e : src.getOutgoing()) {
        Vertex tov = e.getToVertex();
        if (!seenVertices.contains(tov)) {
          seenVertices.add(tov);
          toExplore.add(tov);
        }
      }
    }
    showGraph.setHighlightedVertices(seenVertices);
  }

  protected void checkGraph() {
    HashSet<Vertex> seenVertices = new HashSet<>();
    Collection<Vertex> allVertices = getGraph().getVertices();
    Vertex v = allVertices.iterator().next();
    System.out.println("initial vertex: " + v);
    Queue<Vertex> toExplore = new LinkedList<>();
    toExplore.add(v);
    seenVertices.add(v);
    while (!toExplore.isEmpty()) {
      Vertex src = toExplore.poll();
      for (Edge e : src.getOutgoing()) {
        Vertex tov = e.getToVertex();
        if (!seenVertices.contains(tov)) {
          seenVertices.add(tov);
          toExplore.add(tov);
        }
      }
    }

    System.out.println(
      "After investigation, visited " + seenVertices.size() + " of " + allVertices.size()
    );

    /* now, let's find an unvisited vertex */
    for (Vertex u : allVertices) {
      if (!seenVertices.contains(u)) {
        System.out.println("unvisited vertex" + u);
        break;
      }
    }
  }

  protected void route(String from, String to) {
    Instant when;
    // Year + 1900
    try {
      when = ZonedDateTime.parse(searchDate.getText(), DATE_FORMAT).toInstant();
    } catch (DateTimeParseException e) {
      searchDate.setText("Format: " + DATE_FORMAT.toString());
      return;
    }
    List<String> modes = new ArrayList<>();
    if (walkCheckBox.isSelected()) {
      modes.add(ApiRequestMode.WALK.name());
    }
    if (bikeCheckBox.isSelected()) {
      modes.add(ApiRequestMode.BICYCLE.name());
    }
    if (carCheckBox.isSelected()) {
      modes.add(ApiRequestMode.CAR.name());
    }
    if (ferryCheckBox.isSelected()) {
      modes.add(ApiRequestMode.FERRY.name());
    }
    if (trainCheckBox.isSelected()) {
      modes.add(ApiRequestMode.RAIL.name());
      modes.add(ApiRequestMode.TRAM.name());
      modes.add(ApiRequestMode.SUBWAY.name());
      modes.add(ApiRequestMode.FUNICULAR.name());
      modes.add(ApiRequestMode.GONDOLA.name());
    }
    if (busCheckBox.isSelected()) {
      modes.add(ApiRequestMode.BUS.name());
      modes.add(ApiRequestMode.CABLE_CAR.name());
    }
    if (transitCheckBox.isSelected()) {
      modes.add(ApiRequestMode.TRANSIT.name());
    }

    // TODO: This should use the configured defaults, not the code defaults
    RouteRequestBuilder builder = RouteRequest.of();
    QualifiedModeSet qualifiedModeSet = new QualifiedModeSet(modes.toArray(String[]::new));
    builder.withJourney(b -> b.setModes(qualifiedModeSet.getRequestModes()));

    builder.withArriveBy(arriveByCheckBox.isSelected());
    builder.withDateTime(when);
    builder.withFrom(LocationStringParser.fromOldStyleString(from));
    builder.withTo(LocationStringParser.fromOldStyleString(to));
    builder.withNumItineraries(Integer.parseInt(this.nPaths.getText()));

    builder.withPreferences(preferences -> {
      preferences.withWalk(walk -> {
        walk.withBoardCost(Integer.parseInt(boardingPenaltyField.getText()) * 60); // override low 2-4 minute values
        walk.withSpeed(Float.parseFloat(walkSpeed.getText()));
      });
      preferences.withBike(bike ->
        bike
          .withSpeed(Float.parseFloat(bikeSpeed.getText()))
          // TODO LG Add ui element for bike board cost (for now bike = 2 * walk)
          .withBoardCost(Integer.parseInt(boardingPenaltyField.getText()) * 60 * 2)
          // there should be a ui element for walk distance and optimize type
          .withOptimizeType(getSelectedOptimizeType())
      );
      preferences.withScooter(scooter ->
        scooter
          .withSpeed(Float.parseFloat(bikeSpeed.getText()))
          // there should be a ui element for walk distance and optimize type
          .withOptimizeType(getSelectedOptimizeType())
      );
    });

    // apply callback if the options call for it
    // if( dontUseGraphicalCallbackCheckBox.isSelected() ){
    // TODO perhaps avoid using a GraphPathFinder and go one level down the call chain directly to a GenericAStar
    // TODO perhaps instead of giving the pathservice a callback, we can just put the visitor in the routing request
    GraphPathFinder finder = new GraphPathFinder(traverseVisitor);

    var request = builder.buildRequest();
    long t0 = System.currentTimeMillis();
    // TODO: check options properly intialized (AMB)
    try (
      var temporaryVertices = new TemporaryVerticesContainer(
        graph,
        request.from(),
        request.to(),
        request.journey().direct().mode(),
        request.journey().direct().mode()
      )
    ) {
      List<GraphPath<State, Edge, Vertex>> paths = finder.graphPathFinderEntryPoint(
        request,
        temporaryVertices
      );
      long dt = System.currentTimeMillis() - t0;
      searchTimeElapsedLabel.setText("search time elapsed: " + dt + "ms");

      // grab the spt from the visitor
      // TODO somehow yank the SPT out of the depths of the call stack... but there multiple SPTs here.
      // This is why we should probably just use AStar directly.
      /*
            spt = vis.spt;
            showGraph.setSPT(spt);
            System.out.println( "got spt:"+spt );
            */

      if (paths == null) {
        System.out.println("no path");
        showGraph.highlightGraphPath(null);
        return;
      }

      // now's a convenient time to set graphical SPT weights
      showGraph.simpleSPT.setWeights();

      showPathsInPanel(paths);

      // now's a good time to set showGraph's SPT drawing weights
      showGraph.setSPTFlattening(Float.parseFloat(sptFlattening.getText()));
      showGraph.setSPTThickness(Float.parseFloat(sptThickness.getText()));
      showGraph.redraw();
    }
  }

  VehicleRoutingOptimizeType getSelectedOptimizeType() {
    if (opQuick.isSelected()) {
      return VehicleRoutingOptimizeType.SHORTEST_DURATION;
    }
    if (opSafe.isSelected()) {
      return VehicleRoutingOptimizeType.SAFE_STREETS;
    }
    if (opFlat.isSelected()) {
      return VehicleRoutingOptimizeType.FLAT_STREETS;
    }
    if (opGreenways.isSelected()) {
      return VehicleRoutingOptimizeType.SAFEST_STREETS;
    }
    return VehicleRoutingOptimizeType.SHORTEST_DURATION;
  }

  private Container makeDiffTab() {
    JPanel pane = new JPanel();
    pane.setLayout(new GridLayout(0, 2));

    firstStateData = new JList<>();
    secondStateData = new JList<>();

    // a place to list the states of the first path
    firstComparePathStates = new JList<>();
    JScrollPane stScrollPane = new JScrollPane(firstComparePathStates);
    stScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
    pane.add(stScrollPane);
    firstComparePathStates.addListSelectionListener(
      new ComparePathStatesClickListener(firstStateData)
    );

    // a place to list the states of the second path
    secondComparePathStates = new JList<>();
    stScrollPane = new JScrollPane(secondComparePathStates);
    stScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
    pane.add(stScrollPane);
    secondComparePathStates.addListSelectionListener(
      new ComparePathStatesClickListener(secondStateData)
    );

    // a place to list details of a state selected from the first path
    stScrollPane = new JScrollPane(firstStateData);
    stScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
    pane.add(stScrollPane);

    // a place to list details of a state selected from the second path
    stScrollPane = new JScrollPane(secondStateData);
    stScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
    pane.add(stScrollPane);

    // A button that executes the 'dominates' function between the two states
    // this is useful only if you have a breakpoint set up
    JButton dominateButton = new JButton();
    dominateButton.setText("dominates");
    dominateButton.addActionListener(
      new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          State s1 = firstComparePathStates.getSelectedValue();
          State s2 = secondComparePathStates.getSelectedValue();
          DominanceFunction<State> pareto = new DominanceFunctions.Pareto();
          System.out.println("s1 dominates s2:" + pareto.betterOrEqualAndComparable(s1, s2));
          System.out.println("s2 dominates s1:" + pareto.betterOrEqualAndComparable(s2, s1));
        }
      }
    );
    pane.add(dominateButton);

    // A button that executes the 'traverse' function leading to the last clicked state
    // in either window. Also only useful if you set a breakpoint.
    JButton traverseButton = new JButton();
    traverseButton.setText("traverse");
    traverseButton.addActionListener(
      new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          if (lastStateClicked == null) {
            return;
          }

          Edge backEdge = lastStateClicked.getBackEdge();
          State backState = lastStateClicked.getBackState();

          backEdge.traverse(backState);
        }
      }
    );
    pane.add(traverseButton);

    return pane;
  }

  private Container makeMainTab() {
    Container pane = new JPanel();
    pane.setLayout(new BorderLayout());

    // init center graphical panel
    showGraph = new ShowGraph(this, getGraph());
    pane.add(showGraph, BorderLayout.CENTER);
    traverseVisitor = new VisualTraverseVisitor(showGraph);

    // init left panel
    leftPanel = new JPanel();
    leftPanel.setLayout(new BorderLayout());

    pane.add(leftPanel, BorderLayout.LINE_START);

    initRoutingSubpanel();
    initVertexInfoSubpanel();
    initControlButtons();

    // init right panel
    initRightPanel(pane);
    return pane;
  }

  private JComponent makePrefsPanel() {
    JPanel pane = new JPanel();
    pane.setLayout(new GridLayout(0, 2));

    // 4 rows (7 elements): transport mode options
    walkCheckBox = new JCheckBox("walk");
    walkCheckBox.setSelected(true);
    pane.add(walkCheckBox);
    bikeCheckBox = new JCheckBox("bike");
    pane.add(bikeCheckBox);
    trainCheckBox = new JCheckBox("trainish");
    pane.add(trainCheckBox);
    busCheckBox = new JCheckBox("busish");
    pane.add(busCheckBox);
    ferryCheckBox = new JCheckBox("ferry");
    pane.add(ferryCheckBox);
    transitCheckBox = new JCheckBox("transit");
    transitCheckBox.setSelected(true);
    pane.add(transitCheckBox);
    carCheckBox = new JCheckBox("car");
    pane.add(carCheckBox);

    // GridLayout does not support empty cells, so a dummy label is used to fix the layout.
    JLabel dummyLabel = new JLabel("");
    pane.add(dummyLabel);

    // row: arrive by?
    JLabel arriveByLabel = new JLabel("Arrive by?:");
    pane.add(arriveByLabel);
    arriveByCheckBox = new JCheckBox("arrive by");
    pane.add(arriveByCheckBox);

    // row: boarding penalty
    JLabel boardPenaltyLabel = new JLabel("Boarding penalty (min):");
    pane.add(boardPenaltyLabel);
    boardingPenaltyField = new JTextField("5");
    pane.add(boardingPenaltyField);

    // row: max walk
    JLabel maxWalkLabel = new JLabel("Maximum walk (meters):");
    pane.add(maxWalkLabel);
    maxWalkField = new JTextField("5000");
    pane.add(maxWalkField);

    // row: walk speed
    JLabel walkSpeedLabel = new JLabel("Walk speed (m/s):");
    pane.add(walkSpeedLabel);
    walkSpeed = new JTextField("1.33");
    pane.add(walkSpeed);

    // row: bike speed
    JLabel bikeSpeedLabel = new JLabel("Bike speed (m/s):");
    pane.add(bikeSpeedLabel);
    bikeSpeed = new JTextField("5.0");
    pane.add(bikeSpeed);

    // row: heuristic weight
    JLabel heuristicWeightLabel = new JLabel("Heuristic weight:");
    pane.add(heuristicWeightLabel);
    heuristicWeight = new JTextField("1.0");
    pane.add(heuristicWeight);

    // row: soft walk?
    JLabel softWalkLimitLabel = new JLabel("Soft walk-limit?:");
    pane.add(softWalkLimitLabel);
    softWalkLimiting = new JCheckBox("soft walk-limiting");
    pane.add(softWalkLimiting);

    // row: soft walk-limit penalty
    JLabel softWalkLimitPenaltyLabel = new JLabel("Soft walk-limiting penalty:");
    pane.add(softWalkLimitPenaltyLabel);
    softWalkPenalty = new JTextField("60.0");
    pane.add(softWalkPenalty);

    // row: soft walk-limit overage
    JLabel softWalkLimitOverageLabel = new JLabel("Soft walk-limiting overage:");
    pane.add(softWalkLimitOverageLabel);
    softWalkOverageRate = new JTextField("5.0");
    pane.add(softWalkOverageRate);

    // row: nPaths
    JLabel nPathsLabel = new JLabel("nPaths:");
    pane.add(nPathsLabel);
    nPaths = new JTextField("1");
    pane.add(nPaths);

    // viz preferences
    ItemListener onChangeVizPrefs = new ItemListener() {
      @Override
      public void itemStateChanged(ItemEvent e) {
        showGraph.setShowTransit(showTransitCheckbox.isSelected());
        showGraph.setShowStreets(showStreetsCheckbox.isSelected());
        showGraph.setShowMultistateVertices(showMultistateVerticesCheckbox.isSelected());
        showGraph.setShowHightlights(showHighlightedCheckbox.isSelected());
        showGraph.setShowSPT(showSPTCheckbox.isSelected());
        showGraph.redraw();
      }
    };
    showTransitCheckbox = new JCheckBox("show transit");
    showTransitCheckbox.setSelected(true);
    showTransitCheckbox.addItemListener(onChangeVizPrefs);
    pane.add(showTransitCheckbox);
    showStreetsCheckbox = new JCheckBox("show streets");
    showStreetsCheckbox.setSelected(true);
    showStreetsCheckbox.addItemListener(onChangeVizPrefs);
    pane.add(showStreetsCheckbox);
    showHighlightedCheckbox = new JCheckBox("show highlighted");
    showHighlightedCheckbox.setSelected(true);
    showHighlightedCheckbox.addItemListener(onChangeVizPrefs);
    pane.add(showHighlightedCheckbox);
    showSPTCheckbox = new JCheckBox("show SPT");
    showSPTCheckbox.setSelected(true);
    showSPTCheckbox.addItemListener(onChangeVizPrefs);
    pane.add(showSPTCheckbox);
    showMultistateVerticesCheckbox = new JCheckBox("show multistate vertices");
    showMultistateVerticesCheckbox.setSelected(true);
    showMultistateVerticesCheckbox.addItemListener(onChangeVizPrefs);
    pane.add(showMultistateVerticesCheckbox);

    // GridLayout does not support empty cells, so a dummy label is used to fix the layout.
    JLabel dummyLabel2 = new JLabel("");
    pane.add(dummyLabel2);

    // row: SPT flattening
    JLabel sptFlatteningLabel = new JLabel("SPT flattening:");
    pane.add(sptFlatteningLabel);
    sptFlattening = new JTextField("0.3");
    pane.add(sptFlattening);

    // row: SPT thickness
    JLabel sptThicknessLabel = new JLabel("SPT thickness:");
    pane.add(sptThicknessLabel);
    sptThickness = new JTextField("0.1");
    pane.add(sptThickness);

    // radio buttons: optimize type
    JLabel optimizeTypeLabel = new JLabel("Optimize type:");
    pane.add(optimizeTypeLabel);

    opQuick = new JRadioButton("Quick");
    opQuick.setSelected(true);
    opSafe = new JRadioButton("Safe");
    opFlat = new JRadioButton("Flat");
    opGreenways = new JRadioButton("Greenways");

    optimizeTypeGrp = new ButtonGroup();
    optimizeTypeGrp.add(opQuick);
    optimizeTypeGrp.add(opSafe);
    optimizeTypeGrp.add(opFlat);
    optimizeTypeGrp.add(opGreenways);

    JPanel optimizeTypePane = new JPanel();
    optimizeTypePane.add(opQuick);
    optimizeTypePane.add(opSafe);
    optimizeTypePane.add(opFlat);
    optimizeTypePane.add(opGreenways);

    pane.add(optimizeTypePane);

    return pane;
  }

  private void initRightPanel(Container pane) {
    /* right panel holds trip pattern and stop metadata */
    JPanel rightPanel = new JPanel();
    rightPanel.setLayout(new BorderLayout());
    pane.add(rightPanel, BorderLayout.LINE_END);

    JTabbedPane rightPanelTabs = new JTabbedPane();

    rightPanel.add(rightPanelTabs, BorderLayout.LINE_END);

    // a place to print out the details of a path
    pathStates = new JList<>();
    JScrollPane stScrollPane = new JScrollPane(pathStates);
    stScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
    rightPanelTabs.addTab("path states", stScrollPane);

    // when you select a path component state, it prints the backedge's metadata
    pathStates.addListSelectionListener(
      new ListSelectionListener() {
        @Override
        public void valueChanged(ListSelectionEvent e) {
          outgoingEdges.clearSelection();
          incomingEdges.clearSelection();

          @SuppressWarnings("unchecked")
          JList<State> theList = (JList<State>) e.getSource();
          State st = (State) theList.getSelectedValue();
          Edge edge = st.getBackEdge();
          reactToEdgeSelection(edge, false);
        }
      }
    );

    metadataList = new JList<>();
    metadataModel = new DefaultListModel<>();
    metadataList.setModel(metadataModel);
    JScrollPane mdScrollPane = new JScrollPane(metadataList);
    mdScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
    rightPanelTabs.addTab("metadata", mdScrollPane);

    // This is where matched issues from an issue search go
    issueMatches = new JList<>();
    issueMatches.addListSelectionListener(e -> {
      @SuppressWarnings("unchecked")
      JList<DataImportIssue> theList = (JList<DataImportIssue>) e.getSource();

      DataImportIssue issue = theList.getSelectedValue();
      if (issue == null) {
        return;
      }
      showGraph.drawIssue(issue);
    });

    issueMatchesModel = new DefaultListModel<>();
    issueMatches.setModel(issueMatchesModel);
    JScrollPane imScrollPane = new JScrollPane(issueMatches);
    imScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
    rightPanelTabs.addTab("issues", imScrollPane);

    Dimension size = new Dimension(200, 1600);

    imScrollPane.setMaximumSize(size);
    imScrollPane.setPreferredSize(size);
    stScrollPane.setMaximumSize(size);
    stScrollPane.setPreferredSize(size);
    mdScrollPane.setMaximumSize(size);
    mdScrollPane.setPreferredSize(size);
    rightPanelTabs.setMaximumSize(size);
    rightPanel.setMaximumSize(size);
  }

  private void initControlButtons() {
    /* buttons at bottom */
    JPanel buttonPanel = new JPanel();
    buttonPanel.setLayout(new GridLayout(0, 3));
    leftPanel.add(buttonPanel, BorderLayout.PAGE_END);

    JButton zoomDefaultButton = new JButton("Zoom to default");
    zoomDefaultButton.addActionListener(
      new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          showGraph.zoomToDefault();
        }
      }
    );
    buttonPanel.add(zoomDefaultButton);

    final JFrame frame = this;

    JButton zoomToNodeButton = new JButton("Zoom to node");
    zoomToNodeButton.addActionListener(
      new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          String nodeName = JOptionPane.showInputDialog(
            frame,
            "Node id",
            JOptionPane.PLAIN_MESSAGE
          );
          Vertex v = getGraph().getVertex(VertexLabel.string(nodeName));
          if (v == null) {
            System.out.println("no such node " + nodeName);
          } else {
            showGraph.zoomToVertex(v);
          }
        }
      }
    );
    buttonPanel.add(zoomToNodeButton);

    JButton zoomToLocationButton = new JButton("Zoom to location");
    zoomToLocationButton.addActionListener(
      new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          String result = JOptionPane.showInputDialog("Enter the location (lat lon)");
          if (result == null || result.length() == 0) return;
          String[] tokens = result.split("[\\s,]+");
          double lat = Double.parseDouble(tokens[0]);
          double lon = Double.parseDouble(tokens[1]);
          Coordinate c = new Coordinate(lon, lat);
          showGraph.zoomToLocation(c);
        }
      }
    );
    buttonPanel.add(zoomToLocationButton);

    JButton zoomOutButton = new JButton("Zoom out");
    zoomOutButton.addActionListener(
      new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          showGraph.zoomOut();
        }
      }
    );
    buttonPanel.add(zoomOutButton);

    JButton routeButton2 = new JButton("Route");
    routeButton2.addActionListener(
      new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          // String initialFrom = "";
          // Object selected = nearbyVertices.getSelectedValue();
          // if (selected != null) {
          // initialFrom = selected.toString();
          // }
          // RouteDialog dlg = new RouteDialog(frame, initialFrom); // modal
          String from = sourceVertex.getText();
          String to = sinkVertex.getText();
          route(from, to);
        }
      }
    );
    buttonPanel.add(routeButton2);

    JButton findButton = new JButton("Find node");
    findButton.addActionListener(
      new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          String nodeName = (String) JOptionPane.showInputDialog(
            frame,
            "Node id",
            JOptionPane.PLAIN_MESSAGE
          );
          Vertex v = getGraph().getVertex(VertexLabel.string(nodeName));
          if (v == null) {
            System.out.println("no such node " + nodeName);
          } else {
            showGraph.highlightVertex(v);
            ArrayList<Vertex> l = new ArrayList<>();
            l.add(v);
            verticesSelected(l);
          }
        }
      }
    );
    buttonPanel.add(findButton);

    JButton findEdgeButton = new JButton("Find edge");
    findEdgeButton.addActionListener(
      new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          String edgeName = (String) JOptionPane.showInputDialog(
            frame,
            "Edge name like",
            JOptionPane.PLAIN_MESSAGE
          );
          for (Vertex gv : getGraph().getVertices()) {
            for (Edge edge : gv.getOutgoing()) {
              if (edge.getDefaultName() != null && edge.getDefaultName().contains(edgeName)) {
                showGraph.highlightVertex(gv);
                ArrayList<Vertex> l = new ArrayList<>();
                l.add(gv);
                verticesSelected(l);
              }
            }
          }
        }
      }
    );
    buttonPanel.add(findEdgeButton);

    JButton checkButton = new JButton("Check graph");
    checkButton.addActionListener(
      new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          checkGraph();
        }
      }
    );
    buttonPanel.add(checkButton);

    JButton traceButton = new JButton("Trace");
    traceButton.addActionListener(
      new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          trace();
        }
      }
    );
    buttonPanel.add(traceButton);

    JButton findEdgeByIdButton = new JButton("Find edge ID");
    findEdgeByIdButton.addActionListener(e -> {
      throw new UnsupportedOperationException("Edges no longer have integer IDs.");
    });
    buttonPanel.add(findEdgeByIdButton);

    JButton snapButton = new JButton("Snap location");
    snapButton.addActionListener(
      new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          LOG.error("StreetIndex.getClosestPointOnStreet no longer exists.");
        }
      }
    );
    buttonPanel.add(snapButton);
  }

  private void getMetadata(Object selected) {
    Class<?> c = selected.getClass();
    Field[] fields;
    while (c != null && c != Object.class) {
      metadataModel.addElement("Class:" + c);
      fields = c.getDeclaredFields();
      for (Field field : fields) {
        int modifiers = field.getModifiers();
        if ((modifiers & Modifier.STATIC) != 0) {
          continue;
        }
        field.setAccessible(true);
        String name = field.getName();

        String value = "(unknown -- see console for stack trace)";
        try {
          value = "" + field.get(selected);
        } catch (IllegalArgumentException e1) {
          LOG.error("IllegalArgumentException", e1);
        } catch (IllegalAccessException e1) {
          LOG.error("IllegalAccessException", e1);
        }
        metadataModel.addElement(name + ": " + value);
      }
      c = c.getSuperclass();
    }
  }

  private void reactToEdgeSelection(Edge selected, boolean outgoing) {
    if (selected == null) {
      return;
    }
    showGraph.highlightEdge(selected);

    /* for turns, highlight the outgoing street's ends */
    if (selected instanceof StreetEdge) {
      List<Vertex> vertices = new ArrayList<>();
      List<Edge> edges = new ArrayList<>();
      Vertex tov = selected.getToVertex();
      for (Edge og : tov.getOutgoing()) {
        if (og instanceof StreetEdge) {
          edges.add(og);
          vertices.add(og.getToVertex());
          break;
        }
      }
      Vertex fromv = selected.getFromVertex();
      for (Edge ic : fromv.getIncoming()) {
        if (ic instanceof StreetEdge) {
          edges.add(ic);
          vertices.add(ic.getFromVertex());
          break;
        }
      }
      // showGraph.setHighlightedVertices(vertices);
      showGraph.setHighlightedEdges(edges);
    }

    /* add the connected vertices to the list of vertices */
    VertexList nearbyModel = (VertexList) nearbyVertices.getModel();
    List<Vertex> vertices = nearbyModel.selected;

    Vertex v;
    if (outgoing) {
      v = selected.getToVertex();
    } else {
      v = selected.getFromVertex();
    }
    if (!vertices.contains(v)) {
      vertices.add(v);
      nearbyModel = new VertexList(vertices);
      nearbyVertices.setModel(nearbyModel); // this should just be an event, but for
      // some reason, JList doesn't implement
      // the right event.
    }

    /* set up metadata tab */
    metadataModel.clear();
    getMetadata(selected);
    // fromv
    Vertex fromv = selected.getFromVertex();
    getMetadata(fromv);
    if (selected instanceof StreetEdge) {
      //TODO ElevationProfileSegment do not exist anymore
      //getMetadata(((StreetEdge) selected).getElevationProfileSegment());
    }
    metadataList.revalidate();
  }

  private void initVertexInfoSubpanel() {
    JPanel vertexDataPanel = new JPanel();
    vertexDataPanel.setLayout(new BoxLayout(vertexDataPanel, BoxLayout.PAGE_AXIS));
    vertexDataPanel.setPreferredSize(new Dimension(300, 600));
    leftPanel.add(vertexDataPanel, BorderLayout.CENTER);

    // nearby vertices
    JLabel nvLabel = new JLabel("Vertices");
    vertexDataPanel.add(nvLabel);
    nearbyVertices = new JList<>();
    nearbyVertices.setVisibleRowCount(4);
    JScrollPane nvScrollPane = new JScrollPane(nearbyVertices);
    vertexDataPanel.add(nvScrollPane);
    nearbyVertices.addListSelectionListener(
      new ListSelectionListener() {
        public void valueChanged(ListSelectionEvent e) {
          outgoingEdges.removeAll();
          incomingEdges.removeAll();
          DisplayVertex selected = (DisplayVertex) nearbyVertices.getSelectedValue();
          if (selected != null) {
            Vertex nowSelected = selected.vertex;
            showGraph.highlightVertex(nowSelected);
            outgoingEdges.setModel(new EdgeListModel(nowSelected.getOutgoing()));
            incomingEdges.setModel(new EdgeListModel(nowSelected.getIncoming()));
          }
        }
      }
    );

    // listener useful for both incoming and outgoing edge list panes
    // when a different edge is selected, change up the pattern pane and list of nearby nodes
    ListSelectionListener edgeChanged = new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        @SuppressWarnings("unchecked")
        JList<Edge> edgeList = (JList<Edge>) e.getSource();

        Edge selected = (Edge) edgeList.getSelectedValue();

        boolean outgoing = (edgeList == outgoingEdges);
        reactToEdgeSelection(selected, outgoing);
      }
    };

    // outgoing edges
    JLabel ogeLabel = new JLabel("Outgoing edges");
    vertexDataPanel.add(ogeLabel);
    outgoingEdges = new JList<>();
    outgoingEdges.setVisibleRowCount(4);
    JScrollPane ogeScrollPane = new JScrollPane(outgoingEdges);
    vertexDataPanel.add(ogeScrollPane);
    outgoingEdges.addListSelectionListener(edgeChanged);

    // incoming edges
    JLabel iceLabel = new JLabel("Incoming edges");
    vertexDataPanel.add(iceLabel);
    incomingEdges = new JList<>();
    JScrollPane iceScrollPane = new JScrollPane(incomingEdges);
    vertexDataPanel.add(iceScrollPane);
    incomingEdges.addListSelectionListener(edgeChanged);

    // paths list
    JLabel pathsLabel = new JLabel("Paths");
    vertexDataPanel.add(pathsLabel);
    pathsList = new JList<>();

    popup = new JPopupMenu();
    JMenuItem compareMenuItem = new JMenuItem("compare");
    compareMenuItem.addActionListener(new OnPopupMenuClickListener());
    popup.add(compareMenuItem);

    // make paths list right-clickable
    pathsList.addMouseListener(
      new MouseListener() {
        @Override
        public void mouseClicked(MouseEvent e) {
          if (SwingUtilities.isRightMouseButton(e)) {
            @SuppressWarnings("unchecked")
            JList<PathPrinter> list = (JList<PathPrinter>) e.getSource();
            int row = list.locationToIndex(e.getPoint());
            list.setSelectedIndex(row);

            popup.show(list, e.getX(), e.getY());
          }
        }

        @Override
        public void mousePressed(MouseEvent e) {}

        @Override
        public void mouseReleased(MouseEvent e) {}

        @Override
        public void mouseEntered(MouseEvent e) {}

        @Override
        public void mouseExited(MouseEvent e) {}
      }
    );
    pathsList.addListSelectionListener(
      new ListSelectionListener() {
        @Override
        public void valueChanged(ListSelectionEvent ev) {
          PathPrinter pp = ((PathPrinter) pathsList.getSelectedValue());
          if (pp == null) {
            return;
          }
          GraphPath<State, Edge, Vertex> path = pp.gp;

          DefaultListModel<State> pathModel = new DefaultListModel<>();
          for (State st : path.states) {
            pathModel.addElement(st);
          }
          pathStates.setModel(pathModel);

          showGraph.highlightGraphPath(path);
        }
      }
    );
    JScrollPane pathsScrollPane = new JScrollPane(pathsList);
    vertexDataPanel.add(pathsScrollPane);
  }

  private void initRoutingSubpanel() {
    /* ROUTING SUBPANEL */
    JPanel routingPanel = new JPanel();
    routingPanel.setLayout(new GridLayout(0, 2));
    leftPanel.add(routingPanel, BorderLayout.NORTH);

    // row: source vertex
    JButton setSourceVertexButton = new JButton("set source");
    setSourceVertexButton.addActionListener(
      new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          Object selected = nearbyVertices.getSelectedValue();
          if (selected != null) {
            sourceVertex.setText(selected.toString());
          }
        }
      }
    );
    routingPanel.add(setSourceVertexButton);
    sourceVertex = new JTextField();
    routingPanel.add(sourceVertex);

    // row: sink vertex
    JButton setSinkVertexButton = new JButton("set sink");
    setSinkVertexButton.addActionListener(
      new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          Object selected = nearbyVertices.getSelectedValue();
          if (selected != null) {
            sinkVertex.setText(selected.toString());
          }
        }
      }
    );
    routingPanel.add(setSinkVertexButton);
    sinkVertex = new JTextField();
    routingPanel.add(sinkVertex);

    // row: set date
    JButton resetSearchDateButton = new JButton("now ->");
    resetSearchDateButton.addActionListener(
      new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          searchDate.setText(DATE_FORMAT.format(ZonedDateTime.now()));
        }
      }
    );
    routingPanel.add(resetSearchDateButton);
    searchDate = new JTextField();
    searchDate.setText(DATE_FORMAT.format(ZonedDateTime.now()));
    routingPanel.add(searchDate);

    // row: launch, continue, and clear path search
    JButton routeButton = new JButton("path search");
    routeButton.addActionListener(
      new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          String from = sourceVertex.getText();
          String to = sinkVertex.getText();
          route(from, to);
        }
      }
    );
    routingPanel.add(routeButton);
    JButton continueButton = new JButton("continue");
    continueButton.addActionListener(
      new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          //TODO continue search
        }
      }
    );
    routingPanel.add(continueButton);
    JButton clearRouteButton = new JButton("clear path");
    clearRouteButton.addActionListener(
      new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          showGraph.highlightGraphPath(null);
          showGraph.clearHighlights();
          showGraph.resetSPT();
        }
      }
    );
    routingPanel.add(clearRouteButton);

    //label: search time elapsed
    searchTimeElapsedLabel = new JLabel("search time elapsed:");
    routingPanel.add(searchTimeElapsedLabel);

    //option: don't use graphical callback. useful for doing a quick profile
    dontUseGraphicalCallbackCheckBox = new JCheckBox("no graphics");
    routingPanel.add(dontUseGraphicalCallbackCheckBox);
  }

  private void showPathsInPanel(List<GraphPath<State, Edge, Vertex>> paths) {
    // show paths in a list panel
    DefaultListModel<PathPrinter> data = new DefaultListModel<>();
    for (GraphPath<State, Edge, Vertex> gp : paths) {
      data.addElement(new PathPrinter(gp));
    }
    pathsList.setModel(data);
  }

  static class PathPrinter {

    GraphPath<State, Edge, Vertex> gp;

    PathPrinter(GraphPath<State, Edge, Vertex> gp) {
      this.gp = gp;
    }

    public String toString() {
      String startTime = TIME_FORMAT.format(Instant.ofEpochSecond(gp.getStartTime()));
      String endTime = TIME_FORMAT.format(Instant.ofEpochSecond(gp.getEndTime()));
      return (
        "Path (" +
        startTime +
        "-" +
        endTime +
        ") weight:" +
        gp.getWeight() +
        " dur:" +
        (gp.getDuration() / 60.0)
        // " walk:" +
        // gp.getWalkDistance()
      );
    }
  }

  private final class ComparePathStatesClickListener implements ListSelectionListener {

    private final JList<String> outputList;

    ComparePathStatesClickListener(JList<String> outputList) {
      this.outputList = outputList;
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
      @SuppressWarnings("unchecked")
      JList<State> theList = (JList<State>) e.getSource();
      State st = (State) theList.getSelectedValue();
      if (st == null) {
        return;
      }

      DefaultListModel<String> stateListModel = new DefaultListModel<>();
      stateListModel.addElement("weight:" + st.getWeight());
      stateListModel.addElement("weightdelta:" + st.getWeightDelta());
      stateListModel.addElement("rentingVehicle:" + st.isRentingVehicle());
      stateListModel.addElement("vehicleParked:" + st.isVehicleParked());
      stateListModel.addElement("walkDistance:" + st.getWalkDistance());
      stateListModel.addElement("elapsedTime:" + st.getElapsedTimeSeconds());
      outputList.setModel(stateListModel);

      lastStateClicked = st;
    }
  }

  private final class OnPopupMenuClickListener implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
      PathPrinter pp = ((PathPrinter) pathsList.getSelectedValue());
      if (pp == null) {
        return;
      }
      GraphPath<State, Edge, Vertex> path = pp.gp;

      firstComparePath = secondComparePath;
      secondComparePath = path;

      if (firstComparePath != null) {
        DefaultListModel<State> pathModel = new DefaultListModel<>();
        for (State st : firstComparePath.states) {
          pathModel.addElement(st);
        }
        firstComparePathStates.setModel(pathModel);
      }
      if (secondComparePath != null) {
        DefaultListModel<State> pathModel = new DefaultListModel<>();
        for (State st : secondComparePath.states) {
          pathModel.addElement(st);
        }
        secondComparePathStates.setModel(pathModel);
      }

      int[] diff = diffPaths();
      final int diverge = diff[0];
      final int converge = diff[1];
      if (diff[0] >= 0) {
        firstComparePathStates.setCellRenderer(
          new DiffListCellRenderer(diverge, firstComparePath.states.size() - converge - 1)
        );
        secondComparePathStates.setCellRenderer(
          new DiffListCellRenderer(diverge, secondComparePath.states.size() - converge - 1)
        );
      }
    }

    private int[] diffPaths() {
      if (firstComparePath == null || secondComparePath == null) {
        return new int[] { -2, -2 };
      }

      int l1 = firstComparePath.states.size();
      int l2 = secondComparePath.states.size();
      int minlen = l1 < l2 ? l1 : l2;

      int divergence = -1;
      int convergence = -1;

      // find divergence
      for (int i = 0; i < minlen; i++) {
        Vertex v1 = firstComparePath.states.get(i).getVertex();
        Vertex v2 = secondComparePath.states.get(i).getVertex();
        if (!v1.equals(v2)) {
          divergence = i - 1;
          break;
        }
      }

      // find convergence
      for (int i = 0; i < minlen; i++) {
        Vertex v1 = firstComparePath.states.get(l1 - i - 1).getVertex();
        Vertex v2 = secondComparePath.states.get(l2 - i - 1).getVertex();
        if (!v1.equals(v2)) {
          convergence = i - 1;
          break;
        }
      }

      return new int[] { divergence, convergence };
    }

    private final class DiffListCellRenderer extends DefaultListCellRenderer {

      private final int diverge;
      private final int converge;

      private DiffListCellRenderer(int diverge, int converge) {
        this.diverge = diverge;
        this.converge = converge;
      }

      @Override
      public Component getListCellRendererComponent(
        JList<?> list,
        Object value,
        int index,
        boolean isSelected,
        boolean cellHasFocus
      ) {
        Component c = super.getListCellRendererComponent(
          list,
          value,
          index,
          isSelected,
          cellHasFocus
        );
        if (isSelected) {
          return c;
        }

        if (index <= diverge) {
          c.setBackground(new Color(196, 201, 255));
        }
        if (index >= converge) {
          c.setBackground(new Color(255, 196, 196));
        }

        return c;
      }
    }
  }
}
