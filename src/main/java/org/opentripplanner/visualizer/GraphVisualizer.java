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

package org.opentripplanner.visualizer;

import com.google.common.collect.Sets;
import com.vividsolutions.jts.geom.Coordinate;
import javassist.Modifier;
import org.opentripplanner.common.model.GenericLocation;
import org.opentripplanner.graph_builder.annotation.GraphBuilderAnnotation;
import org.opentripplanner.graph_builder.annotation.StopUnlinked;
import org.opentripplanner.routing.algorithm.TraverseVisitor;
import org.opentripplanner.routing.core.OptimizeType;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.impl.GraphPathFinder;
import org.opentripplanner.routing.spt.DominanceFunction;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.vertextype.IntersectionVertex;
import org.opentripplanner.standalone.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.Field;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.Queue;

/**
 * Exit on window close.
 * 
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
        String label = vertex.getLabel();
        if (label.contains("osm node")) {
            label = vertex.getName();
        }
        return label;
    }
}

/**
 * This is a ListModel that holds Edges. It gets its edges from a PatternBoard/PatternAlight, hence the iterable.
 */
class EdgeListModel extends AbstractListModel<Edge> {

    private static final long serialVersionUID = 1L;

    private ArrayList<Edge> edges;

    EdgeListModel(Iterable<Edge> edges) {
        this.edges = new ArrayList<Edge>();
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

    private static final long serialVersionUID = 1L;

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
};

/**
 * A simple visualizer for graphs. It shows (using ShowGraph) a map of the graph, intersections and 
 * TransitStops only, and allows a user to select stops, examine incoming and outgoing edges, and 
 * examine trip patterns. It's meant mainly for debugging, so it's totally OK if it develops (say) 
 * a bunch of weird buttons designed to debug specific cases.
 */
public class GraphVisualizer extends JFrame implements VertexSelectionListener {
	
	private final class ComparePathStatesClickListener implements ListSelectionListener {
		private JList<String> outputList;

		ComparePathStatesClickListener(JList<String> outputList){
			this.outputList = outputList;
		}
		
		@Override
		public void valueChanged(ListSelectionEvent e) {
			@SuppressWarnings("unchecked")
			JList<State> theList = (JList<State>)e.getSource();
			State st = (State)theList.getSelectedValue();
			if(st==null){
				return;
			}
			
			DefaultListModel<String> stateListModel = new DefaultListModel<String>();
			stateListModel.addElement( "weight:"+st.getWeight() );
			stateListModel.addElement( "weightdelta:"+st.getWeightDelta() );
			stateListModel.addElement( "bikeRenting:"+st.isBikeRenting() );
			stateListModel.addElement( "carParked:"+st.isCarParked() );
			stateListModel.addElement( "walkDistance:"+st.getWalkDistance() );
			stateListModel.addElement( "elapsedTime:"+st.getElapsedTimeSeconds() );
			stateListModel.addElement( "numBoardings:"+st.getNumBoardings() );
			outputList.setModel( stateListModel );
			
			lastStateClicked = st;
		}
	}

	private final class OnPopupMenuClickListener implements ActionListener {
		private final class DiffListCellRenderer extends DefaultListCellRenderer {
			private final int diverge;
			private final int converge;

			private DiffListCellRenderer(int diverge, int converge) {
				this.diverge = diverge;
				this.converge = converge;
			}

			@Override
			public Component getListCellRendererComponent(JList<?> list, Object value, int index,
					boolean isSelected, boolean cellHasFocus) {
				Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				if(isSelected){
					return c;
				}
				
				if(index <= diverge){
					c.setBackground(new Color(196,201,255));
				}
				if(index >= converge){
					c.setBackground(new Color(255,196,196));
				}
				
				return c;
			}
		}
		
		private int[] diffPaths() {
			if(firstComparePath == null || secondComparePath == null) {
				int[] failboat = {-2,-2};
				return failboat;
			}
			
			int l1 = firstComparePath.states.size();
			int l2 = secondComparePath.states.size();
			int minlen = l1 < l2 ? l1 : l2;
			
			int divergence=-1;
			int convergence=-1;
			
			// find divergence
			for(int i=0; i<minlen; i++){
				Vertex v1 = firstComparePath.states.get(i).getVertex();
				Vertex v2 = secondComparePath.states.get(i).getVertex();
				if(!v1.equals(v2)){
					divergence = i-1;
					break;
				}
			}
			
			// find convergence
			for(int i=0; i<minlen; i++){
				Vertex v1 = firstComparePath.states.get(l1-i-1).getVertex();
				Vertex v2 = secondComparePath.states.get(l2-i-1).getVertex();
				if(!v1.equals(v2)){
					convergence = i-1;
					break;
				}
			}
			
			int[] ret = {divergence,convergence};
			return ret;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			PathPrinter pp = ((PathPrinter) pathsList.getSelectedValue());
			if(pp==null){
				return;
			}
			GraphPath path = pp.gp;
			
			firstComparePath = secondComparePath;
			secondComparePath = path;
			
			if(firstComparePath != null) {
				DefaultListModel<State> pathModel = new DefaultListModel<State>();
				for( State st : firstComparePath.states ){
					pathModel.addElement( st );
				}
				firstComparePathStates.setModel( pathModel );
			}
			if(secondComparePath != null){
				DefaultListModel<State> pathModel = new DefaultListModel<State>();
				for( State st : secondComparePath.states ){
					pathModel.addElement( st );
				}
				secondComparePathStates.setModel( pathModel );
			}
			
			int[] diff = diffPaths();
			final int diverge = diff[0];
			final int converge = diff[1];
			if(diff[0]>=0){
				firstComparePathStates.setCellRenderer(new DiffListCellRenderer(diverge,firstComparePath.states.size()-converge-1));
				secondComparePathStates.setCellRenderer(new DiffListCellRenderer(diverge,secondComparePath.states.size()-converge-1));
			}
		}
	}

	class PathPrinter{
		GraphPath gp;
		PathPrinter(GraphPath gp){
			this.gp=gp;
		}
		public String toString(){
			SimpleDateFormat shortDateFormat = new SimpleDateFormat("HH:mm:ss z");
			String startTime = shortDateFormat.format(new Date(gp.getStartTime()*1000));
			String endTime = shortDateFormat.format(new Date(gp.getEndTime()*1000));
			return "Path ("+startTime+"-"+endTime+") weight:"+gp.getWeight()+" dur:"+(gp.getDuration()/60.0)+" walk:"+gp.getWalkDistance()+" nTrips:"+gp.getTrips().size();
		}
	}

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(GraphVisualizer.class);

    private JPanel leftPanel;

    /* The Processing applet that actually displays the graph. */
    private ShowGraph showGraph;

    /* The set of callbacks that display search progress on the showGraph Processing applet. */
    public TraverseVisitor traverseVisitor;

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

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");

    private JTextField boardingPenaltyField;

    private DefaultListModel<GraphBuilderAnnotation> annotationMatchesModel;

    private JList<GraphBuilderAnnotation> annotationMatches;
    
    private DefaultListModel<String> metadataModel;

    private HashSet<Vertex> closed;

    private Vertex tracingVertex;

    private HashSet<Vertex> open;

    private HashSet<Vertex> seen;

    private JList<String> metadataList;

    /* The router we are visualizing. */
    private final Router router;

    /* The graph from the router we are visualizing, note that it will not be updated if the router reloads. */
    private final Graph graph;

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
	
	private ShortestPathTree spt;
	
	private JTextField sptFlattening;
	
	private JTextField sptThickness;
	
	private JPopupMenu popup;

	private GraphPath firstComparePath;
	private GraphPath secondComparePath;

	private JList<State> firstComparePathStates;
	private JList<State> secondComparePathStates;

	private JList<String> secondStateData;

	private JList<String> firstStateData;

	protected State lastStateClicked=null;

	private JCheckBox longDistanceModeCheckbox;

    public GraphVisualizer(Router router) {
        super();
        LOG.info("Starting up graph visualizer...");
        setTitle("GraphVisualizer");
        this.router = router;
        this.graph = router.graph;
        init();
    }

    public void run () {
        this.setVisible(true);
    }
    
    public void init() {
    	final JTabbedPane tabbedPane = new JTabbedPane();
    	
    	final Container mainTab = makeMainTab();
    	Container prefsPanel = makePrefsPanel();
    	Container diffTab = makeDiffTab();
         
    	tabbedPane.addTab("Main", null, mainTab,
                "Pretty much everything");
         
    	tabbedPane.addTab("Prefs", null, prefsPanel,
                "Routing preferences");
    	
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
        tabbedPane.addChangeListener(new ChangeListener(){
        	@Override
        	public void stateChanged(ChangeEvent e) {
        		if( tabbedPane.getSelectedComponent().equals(mainTab) ){
        			showGraph.loop();	
        		} else{
        			showGraph.noLoop();
        		}
        	}
        });
    }

	private Container makeDiffTab() {
        JPanel pane = new JPanel();
        pane.setLayout(new GridLayout(0, 2));
        
        firstStateData = new JList<String>();
        secondStateData = new JList<String>();
        
        // a place to list the states of the first path
        firstComparePathStates = new JList<State>();        
        JScrollPane stScrollPane = new JScrollPane(firstComparePathStates);
        stScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        pane.add(stScrollPane);
        firstComparePathStates.addListSelectionListener(new ComparePathStatesClickListener(firstStateData));
        
        // a place to list the states of the second path
        secondComparePathStates = new JList<State>();
        stScrollPane = new JScrollPane(secondComparePathStates);
        stScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        pane.add(stScrollPane);
        secondComparePathStates.addListSelectionListener(new ComparePathStatesClickListener(secondStateData));
        
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
        dominateButton.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
                State s1 = firstComparePathStates.getSelectedValue();
				State s2 = secondComparePathStates.getSelectedValue();
                DominanceFunction pareto = new DominanceFunction.Pareto();
				System.out.println("s1 dominates s2:" + pareto.betterOrEqualAndComparable(s1, s2));
				System.out.println("s2 dominates s1:" + pareto.betterOrEqualAndComparable(s2, s1));
			}
        });
        pane.add(dominateButton);
        
        // A button that executes the 'traverse' function leading to the last clicked state
        // in either window. Also only useful if you set a breakpoint.
        JButton traverseButton = new JButton();
        traverseButton.setText("traverse");
        traverseButton.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				if(lastStateClicked==null){
					return;
				}
				
				Edge backEdge = lastStateClicked.getBackEdge();
				State backState = lastStateClicked.getBackState();
				
				backEdge.traverse(backState);
			}
        });
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
	
	OptimizeType getSelectedOptimizeType(){
		if(opQuick.isSelected()){
			return OptimizeType.QUICK;
		}
		if(opSafe.isSelected()){
			return OptimizeType.SAFE;
		}
		if(opFlat.isSelected()){
			return OptimizeType.FLAT;
		}
		if(opGreenways.isSelected()){
			return OptimizeType.GREENWAYS;
		}
		return OptimizeType.QUICK;
	}
	
    
    protected JComponent makeTextPanel(String text) {
        JPanel panel = new JPanel(false);
        JLabel filler = new JLabel(text);
        filler.setHorizontalAlignment(JLabel.CENTER);
        panel.setLayout(new GridLayout(1, 1));
        panel.add(filler);
        return panel;
    }

	private void initRightPanel(Container pane) {
		/* right panel holds trip pattern and stop metadata */
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BorderLayout());
        pane.add(rightPanel, BorderLayout.LINE_END);

        JTabbedPane rightPanelTabs = new JTabbedPane();

        rightPanel.add(rightPanelTabs, BorderLayout.LINE_END);
        
        // a place to print out the details of a path
        pathStates = new JList<State>();
        JScrollPane stScrollPane = new JScrollPane(pathStates);
        stScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        rightPanelTabs.addTab("path states", stScrollPane);
        
        // when you select a path component state, it prints the backedge's metadata
        pathStates.addListSelectionListener(new ListSelectionListener(){
	        @Override
	        public void valueChanged(ListSelectionEvent e) {
	        	outgoingEdges.clearSelection();
	        	incomingEdges.clearSelection();
		
	        	@SuppressWarnings("unchecked")
				JList<State> theList = (JList<State>)e.getSource();
	        	State st = (State)theList.getSelectedValue();
	        	Edge edge = st.getBackEdge();
	        	reactToEdgeSelection( edge, false );
	        }
        });
         

        metadataList = new JList<String>();
        metadataModel = new DefaultListModel<String>();
        metadataList.setModel(metadataModel);
        JScrollPane mdScrollPane = new JScrollPane(metadataList);
        mdScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        rightPanelTabs.addTab("metadata", mdScrollPane);

        // This is where matched annotations from an annotation search go
        annotationMatches = new JList<GraphBuilderAnnotation>();
        annotationMatches.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                @SuppressWarnings("unchecked")
				JList<GraphBuilderAnnotation> theList = (JList<GraphBuilderAnnotation>) e.getSource();
                
                GraphBuilderAnnotation anno = theList.getSelectedValue();
                if (anno == null)
                    return;
                showGraph.drawAnotation(anno);
            }
        });

        annotationMatchesModel = new DefaultListModel<GraphBuilderAnnotation>();
        annotationMatches.setModel(annotationMatchesModel);
        JScrollPane amScrollPane = new JScrollPane(annotationMatches);
        amScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        rightPanelTabs.addTab("annotations", amScrollPane);

        Dimension size = new Dimension(200, 1600);

        amScrollPane.setMaximumSize(size);
        amScrollPane.setPreferredSize(size);
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
        zoomDefaultButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showGraph.zoomToDefault();
            }
        });
        buttonPanel.add(zoomDefaultButton);

        final JFrame frame = this;

        JButton zoomToNodeButton = new JButton("Zoom to node");
        zoomToNodeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String nodeName = (String) JOptionPane.showInputDialog(frame, "Node id",
                        JOptionPane.PLAIN_MESSAGE);
                Vertex v = getGraph().getVertex(nodeName);
                if (v == null) {
                    System.out.println("no such node " + nodeName);
                } else {
                    showGraph.zoomToVertex(v);
                }
            }
        });
        buttonPanel.add(zoomToNodeButton);

        JButton zoomToLocationButton = new JButton("Zoom to location");
        zoomToLocationButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String result = JOptionPane.showInputDialog("Enter the location (lat lon)");
                if (result == null || result.length() == 0)
                    return;
                String[] tokens = result.split("[\\s,]+");
                double lat = Double.parseDouble(tokens[0]);
                double lon = Double.parseDouble(tokens[1]);
                Coordinate c = new Coordinate(lon, lat);
                showGraph.zoomToLocation(c);
            }
        });
        buttonPanel.add(zoomToLocationButton);

        JButton zoomOutButton = new JButton("Zoom out");
        zoomOutButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showGraph.zoomOut();
            }
        });
        buttonPanel.add(zoomOutButton);

        JButton routeButton2 = new JButton("Route");
        routeButton2.addActionListener(new ActionListener() {
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
        });
        buttonPanel.add(routeButton2);

        JButton findButton = new JButton("Find node");
        findButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String nodeName = (String) JOptionPane.showInputDialog(frame, "Node id",
                        JOptionPane.PLAIN_MESSAGE);
                Vertex v = getGraph().getVertex(nodeName);
                if (v == null) {
                    System.out.println("no such node " + nodeName);
                } else {
                    showGraph.highlightVertex(v);
                    ArrayList<Vertex> l = new ArrayList<Vertex>();
                    l.add(v);
                    verticesSelected(l);
                }
            }
        });
        buttonPanel.add(findButton);

        JButton findEdgeButton = new JButton("Find edge");
        findEdgeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String edgeName = (String) JOptionPane.showInputDialog(frame, "Edge name like",
                        JOptionPane.PLAIN_MESSAGE);
                for (Vertex gv : getGraph().getVertices()) {
                    for (Edge edge : gv.getOutgoing()) {
                        if (edge.getName() != null && edge.getName().contains(edgeName)) {
                            showGraph.highlightVertex(gv);
                            ArrayList<Vertex> l = new ArrayList<Vertex>();
                            l.add(gv);
                            verticesSelected(l);
                        }
                    }
                }
            }
        });
        buttonPanel.add(findEdgeButton);

        JButton checkButton = new JButton("Check graph");
        checkButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                checkGraph();
            }
        });
        buttonPanel.add(checkButton);

        JButton traceButton = new JButton("Trace");
        traceButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                trace();
            }
        });
        buttonPanel.add(traceButton);

        // annotation search button
        JButton annotationButton = new JButton("Find annotations");
        annotationButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                findAnnotation();
            }
        });
        buttonPanel.add(annotationButton);

        JButton findEdgeByIdButton = new JButton("Find edge ID");
        findEdgeByIdButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String edgeIdStr = (String) JOptionPane.showInputDialog(frame, "Edge ID",
                        JOptionPane.PLAIN_MESSAGE);
                Integer edgeId = Integer.parseInt(edgeIdStr);
                Edge edge = getGraph().getEdgeById(edgeId);
                if (edge != null) {
                    showGraph.highlightEdge(edge);
                    showGraph.highlightVertex(edge.getFromVertex());
                } else {
                    System.out.println("Found no edge with ID " + edgeIdStr);
                }
            }
        });
        buttonPanel.add(findEdgeByIdButton);
        
        JButton snapButton = new JButton("Snap location");
        snapButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                LOG.error("StreetIndex.getClosestPointOnStreet no longer exists.");
            }
        });
        buttonPanel.add(snapButton);
	}
	
		
	    private void getMetadata(Object selected) {
	        Class<?> c = selected.getClass();
	        Field[] fields;
	        while (c != null && c != Object.class) {
	            metadataModel.addElement("Class:" + c);
	            fields = c.getDeclaredFields();
	            for (int i = 0; i < fields.length; i++) {
	                Field field = fields[i];
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
		
		private void reactToEdgeSelection(Edge selected, boolean outgoing){
	        if (selected == null) {
	            return;
	        }
	        showGraph.highlightEdge(selected);
	
	        /* for turns, highlight the outgoing street's ends */
	        if (selected instanceof StreetEdge) {
	            List<Vertex> vertices = new ArrayList<Vertex>();
	            List<Edge> edges = new ArrayList<Edge>();
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
        nearbyVertices = new JList<DisplayVertex>();
        nearbyVertices.setVisibleRowCount(4);
        JScrollPane nvScrollPane = new JScrollPane(nearbyVertices);
        vertexDataPanel.add(nvScrollPane);
        nearbyVertices.addListSelectionListener(new ListSelectionListener() {
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
        });
        
        // listener useful for both incoming and outgoing edge list panes
        // when a different edge is selected, change up the pattern pane and list of nearby nodes
        ListSelectionListener edgeChanged = new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {

                @SuppressWarnings("unchecked")
				JList<Edge> edgeList = (JList<Edge>) e.getSource();
                
                Edge selected = (Edge) edgeList.getSelectedValue();
                
                boolean outgoing = (edgeList==outgoingEdges);
                reactToEdgeSelection( selected, outgoing );
            }

        };

        // outgoing edges
        JLabel ogeLabel = new JLabel("Outgoing edges");
        vertexDataPanel.add(ogeLabel);
        outgoingEdges = new JList<Edge>();
        outgoingEdges.setVisibleRowCount(4);
        JScrollPane ogeScrollPane = new JScrollPane(outgoingEdges);
        vertexDataPanel.add(ogeScrollPane);
        outgoingEdges.addListSelectionListener(edgeChanged);

        // incoming edges
        JLabel iceLabel = new JLabel("Incoming edges");
        vertexDataPanel.add(iceLabel);
        incomingEdges = new JList<Edge>();
        JScrollPane iceScrollPane = new JScrollPane(incomingEdges);
        vertexDataPanel.add(iceScrollPane);
        incomingEdges.addListSelectionListener(edgeChanged);

        // paths list
        JLabel pathsLabel = new JLabel("Paths");
        vertexDataPanel.add(pathsLabel);
        pathsList = new JList<PathPrinter>();
        
        popup = new JPopupMenu();
        JMenuItem compareMenuItem = new JMenuItem("compare");
        compareMenuItem.addActionListener(new OnPopupMenuClickListener());
        popup.add(compareMenuItem);
        
        // make paths list right-clickable
        pathsList.addMouseListener(new MouseListener(){
			@Override
			public void mouseClicked(MouseEvent e) {
				if( SwingUtilities.isRightMouseButton(e) ){
					@SuppressWarnings("unchecked")
					JList<PathPrinter> list = (JList<PathPrinter>)e.getSource();
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
        });
        pathsList.addListSelectionListener(new ListSelectionListener(){
			@Override
			public void valueChanged(ListSelectionEvent ev) {
				
				PathPrinter pp = ((PathPrinter) pathsList.getSelectedValue());
				if(pp==null){
					return;
				}
				GraphPath path = pp.gp;
				
				DefaultListModel<State> pathModel = new DefaultListModel<State>();
				for( State st : path.states ){
					pathModel.addElement( st );
				}
				pathStates.setModel( pathModel );
				
				showGraph.highlightGraphPath(path);		
			}
	
        });
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
        setSourceVertexButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Object selected = nearbyVertices.getSelectedValue();
                if (selected != null) {
                    sourceVertex.setText(selected.toString());
                }
            }
        });
        routingPanel.add(setSourceVertexButton);
        sourceVertex = new JTextField();
        routingPanel.add(sourceVertex);

        // row: sink vertex
        JButton setSinkVertexButton = new JButton("set sink");
        setSinkVertexButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Object selected = nearbyVertices.getSelectedValue();
                if (selected != null) {
                    sinkVertex.setText(selected.toString());
                }
            }
        });
        routingPanel.add(setSinkVertexButton);
        sinkVertex = new JTextField();
        routingPanel.add(sinkVertex);

        // row: set date
        JButton resetSearchDateButton = new JButton("now ->");
        resetSearchDateButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                searchDate.setText(dateFormat.format(new Date()));
            }
        });
        routingPanel.add(resetSearchDateButton);
        searchDate = new JTextField();
        searchDate.setText(dateFormat.format(new Date()));
        routingPanel.add(searchDate);



        // row: launch, continue, and clear path search
        JButton routeButton = new JButton("path search");
        routeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String from = sourceVertex.getText();
                String to = sinkVertex.getText();
                route(from, to);
            }
        });
        routingPanel.add(routeButton);
        JButton continueButton = new JButton("continue");
        continueButton.addActionListener(new ActionListener() {
	        public void actionPerformed(ActionEvent e) {
	        	//TODO continue search
	        }
        });
        routingPanel.add(continueButton);
        JButton clearRouteButton = new JButton("clear path");
        clearRouteButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showGraph.highlightGraphPath(null);
                showGraph.clearHighlights();
                showGraph.resetSPT();
            }
        });
        routingPanel.add(clearRouteButton);
        
        //label: search time elapsed
        searchTimeElapsedLabel = new JLabel("search time elapsed:");
        routingPanel.add(searchTimeElapsedLabel);
        
        //option: don't use graphical callback. useful for doing a quick profile
        dontUseGraphicalCallbackCheckBox = new JCheckBox("no graphics");
        routingPanel.add(dontUseGraphicalCallbackCheckBox);
	}

    protected void trace() {
        DisplayVertex selected = (DisplayVertex) nearbyVertices.getSelectedValue();
        if (selected == null) {
            return;
        }
        Vertex v = selected.vertex;

        if (tracingVertex != v) {
            tracingVertex = v;
            closed = new HashSet<Vertex>();
            open = new HashSet<Vertex>();
            open.add(v);
            seen = new HashSet<Vertex>();
        }
        HashSet<Vertex> newOpen = new HashSet<Vertex>();
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
        HashSet<Vertex> seenVertices = new HashSet<Vertex>();
        DisplayVertex selected = (DisplayVertex) nearbyVertices.getSelectedValue();
        if (selected == null) {
            System.out.println("no vertex selected");
            return;
        }
        Vertex v = selected.vertex;
        System.out.println("initial vertex: " + v);
        Queue<Vertex> toExplore = new LinkedList<Vertex>();
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

        HashSet<Vertex> seenVertices = new HashSet<Vertex>();
        Collection<Vertex> allVertices = getGraph().getVertices();
        Vertex v = allVertices.iterator().next();
        System.out.println("initial vertex: " + v);
        Queue<Vertex> toExplore = new LinkedList<Vertex>();
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

        System.out.println("After investigation, visited " + seenVertices.size() + " of "
                + allVertices.size());

        /* now, let's find an unvisited vertex */
        for (Vertex u : allVertices) {
            if (!seenVertices.contains(u)) {
                System.out.println("unvisited vertex" + u);
                break;
            }
        }
    }

    protected void route(String from, String to) {
        Date when;
        // Year + 1900
        try {
            when = dateFormat.parse(searchDate.getText());
        } catch (ParseException e) {
            searchDate.setText("Format: " + dateFormat.toPattern());
            return;
        }
        TraverseModeSet modeSet = new TraverseModeSet();
        modeSet.setWalk(walkCheckBox.isSelected());
        modeSet.setBicycle(bikeCheckBox.isSelected());
        modeSet.setFerry(ferryCheckBox.isSelected());
        modeSet.setRail(trainCheckBox.isSelected());
        modeSet.setTram(trainCheckBox.isSelected());
        modeSet.setSubway(trainCheckBox.isSelected());
        modeSet.setFunicular(trainCheckBox.isSelected());
        modeSet.setGondola(trainCheckBox.isSelected());
        modeSet.setBus(busCheckBox.isSelected());
        modeSet.setCableCar(busCheckBox.isSelected());
        modeSet.setCar(carCheckBox.isSelected());
        // must set generic transit mode last, and only when it is checked
        // otherwise 'false' will clear trainish and busish
        if (transitCheckBox.isSelected())
            modeSet.setTransit(true);
        RoutingRequest options = new RoutingRequest(modeSet);
        options.setArriveBy(arriveByCheckBox.isSelected());
        options.setWalkBoardCost(Integer.parseInt(boardingPenaltyField.getText()) * 60); // override low 2-4 minute values
        // TODO LG Add ui element for bike board cost (for now bike = 2 * walk)
        options.setBikeBoardCost(Integer.parseInt(boardingPenaltyField.getText()) * 60 * 2);
        // there should be a ui element for walk distance and optimize type
        options.setOptimize( getSelectedOptimizeType() );
        options.setMaxWalkDistance(Integer.parseInt(maxWalkField.getText()));
        options.setDateTime(when);
        options.setFromString(from);
        options.setToString(to);
        options.walkSpeed = Float.parseFloat(walkSpeed.getText());
        options.bikeSpeed = Float.parseFloat(bikeSpeed.getText());
        options.softWalkLimiting = ( softWalkLimiting.isSelected() );
        options.softWalkPenalty = (Float.parseFloat(softWalkPenalty.getText()));
        options.softWalkOverageRate = (Float.parseFloat(this.softWalkOverageRate.getText()));
        options.numItineraries = 1;
        System.out.println("--------");
        System.out.println("Path from " + from + " to " + to + " at " + when);
        System.out.println("\tModes: " + modeSet);
        System.out.println("\tOptions: " + options);
        
        options.numItineraries = ( Integer.parseInt( this.nPaths.getText() ) );
        
        // apply callback if the options call for it
        // if( dontUseGraphicalCallbackCheckBox.isSelected() ){
        // TODO perhaps avoid using a GraphPathFinder and go one level down the call chain directly to a GenericAStar
        // TODO perhaps instead of giving the pathservice a callback, we can just put the visitor in the routing request
        GraphPathFinder finder = new GraphPathFinder(router);

        long t0 = System.currentTimeMillis();
        // TODO: check options properly intialized (AMB)
        List<GraphPath> paths = finder.graphPathFinderEntryPoint(options);
        long dt = System.currentTimeMillis() - t0;
        searchTimeElapsedLabel.setText( "search time elapsed: "+dt+"ms" );
        
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
        showGraph.setSPTFlattening( Float.parseFloat(sptFlattening.getText()) );
        showGraph.setSPTThickness( Float.parseFloat(sptThickness.getText()) );
        showGraph.redraw();
        
        options.cleanup();
    }
    
	private void showPathsInPanel(List<GraphPath> paths) {
		// show paths in a list panel
		DefaultListModel<PathPrinter> data = new DefaultListModel<PathPrinter>();
		for(GraphPath gp : paths ){
			data.addElement( new PathPrinter(gp) );
		}
		pathsList.setModel(data);
	}

    protected void findAnnotation() {
        Set<Class<? extends GraphBuilderAnnotation>> gbaClasses = Sets.newHashSet();
        for (GraphBuilderAnnotation gba : graph.getBuilderAnnotations()) {
            gbaClasses.add(gba.getClass());
        }

        @SuppressWarnings("unchecked")
        Class<? extends GraphBuilderAnnotation> variety = (Class<? extends GraphBuilderAnnotation>) JOptionPane
                .showInputDialog(null, // parentComponent; TODO: set correctly
                        "Select the type of annotation to find", // question
                        "Select annotation", // title
                        JOptionPane.QUESTION_MESSAGE, // message type
                        null, // no icon
                        gbaClasses.toArray(), // options (built above)
                        StopUnlinked.class // default value
                );

        // User clicked cancel
        if (variety == null)
            return;

        // loop over the annotations and save the ones of the requested type
        annotationMatchesModel.clear();
        for (GraphBuilderAnnotation anno : graph.getBuilderAnnotations()) {
            if (variety.isInstance(anno)) {
                annotationMatchesModel.addElement(anno);
            }
        }

        System.out.println("Found " + annotationMatchesModel.getSize() + " annotations of type "
                + variety);

    }

    public void verticesSelected(final List<Vertex> selected) {
        // sort vertices by name
        Collections.sort(selected, new Comparator<Vertex>() {
            @Override
            public int compare(Vertex arg0, Vertex arg1) {
                return arg0.getLabel().compareTo(arg1.getLabel());
            }

        });
        ListModel<DisplayVertex> data = new VertexList(selected);
        nearbyVertices.setModel(data);

        // pick out an intersection vertex and find the path
        // if the spt is already available
        Vertex target=null;
        for(Vertex vv : selected){
        	if( vv instanceof IntersectionVertex ){
        		target = vv;
        		break;
        	}
        }
        if(target!=null && spt!=null){
        	List<GraphPath> paths = spt.getPaths(target,true);
        	showPathsInPanel( paths );
        }
    }

    public Graph getGraph() {
        return graph;
    }

}
