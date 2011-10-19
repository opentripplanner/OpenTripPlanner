package org.gvsig.scolab.otp;
/* gvSIG. Geographic Information System of the Valencian Government
 *
 * Copyright (C) 2007-2008 Infrastructures and Transports Department
 * of the Valencian Government (CIT)
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, 
 * MA  02110-1301, USA.
 * 
 */

/*
 * AUTHORS (In addition to CIT):
 * 2010 Software Colaborativo (www.scolab.es)   development
 */



import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;

import org.gvsig.gui.beans.swing.JFileChooser;
import org.opentripplanner.routing.contraction.ContractionHierarchySet;
import org.opentripplanner.routing.core.Edge;
import org.opentripplanner.routing.core.EdgeNarrative;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.impl.ContractionHierarchySerializationLibrary;

import com.hardcode.gdbms.engine.values.Value;
import com.hardcode.gdbms.engine.values.ValueFactory;
import com.iver.andami.PluginServices;
import com.iver.andami.iconthemes.IIconTheme;
import com.iver.andami.plugins.Extension;
import com.iver.andami.ui.mdiManager.IWindow;
import com.iver.cit.gvsig.fmap.MapContext;
import com.iver.cit.gvsig.fmap.core.FPoint2D;
import com.iver.cit.gvsig.fmap.core.FShape;
import com.iver.cit.gvsig.fmap.core.GeneralPathX;
import com.iver.cit.gvsig.fmap.core.IGeometry;
import com.iver.cit.gvsig.fmap.core.ShapeFactory;
import com.iver.cit.gvsig.fmap.core.v02.FConverter;
import com.iver.cit.gvsig.fmap.drivers.ConcreteMemoryDriver;
import com.iver.cit.gvsig.fmap.layers.FLayer;
import com.iver.cit.gvsig.fmap.layers.LayerFactory;
import com.iver.cit.gvsig.project.documents.view.gui.IView;
import com.vividsolutions.jts.geom.Geometry;

public class LoadOTPGraphExtension extends Extension {

	private ConcreteMemoryDriver driverNodes;
	private ConcreteMemoryDriver driverEdges;

	public void execute(String actionCommand) {
		IWindow w = PluginServices.getMDIManager().getActiveWindow();
		MapContext mc = ((IView) w).getMapControl().getMapContext();
		
		File defaultF = null;
		JFileChooser fileChooser = new JFileChooser("GTFS_PATH", defaultF);
		fileChooser.setDialogTitle("Select OTP Graph.obj file");
		fileChooser.setAcceptAllFileFilterUsed(true);

		int ok = fileChooser.showOpenDialog((Component) PluginServices
				.getMDIManager().getActiveWindow());
		if (ok != JFileChooser.APPROVE_OPTION)
			return;

		PluginServices.getMDIManager().setWaitCursor();

		
		createMemoryDrivers(fileChooser.getSelectedFile().getPath());
		
		FLayer lyrNodes = LayerFactory.createLayer("NodesOTP", driverNodes, mc.getProjection());
		FLayer lyrEdges = LayerFactory.createLayer("EdgesOTP", driverEdges, mc.getProjection());
		
		mc.beginAtomicEvent();
		mc.getLayers().addLayer(lyrEdges);
		mc.getLayers().addLayer(lyrNodes);
		mc.endAtomicEvent();
		PluginServices.getMDIManager().restoreCursor();
	}

	public void initialize() {
		IIconTheme iconTheme = PluginServices.getIconTheme();
		URL iconUrl = getClass().getClassLoader().getResource("images/tripplanner.png");
		iconTheme.registerDefault("load_otp_graph", iconUrl);
	}
	
	public void createMemoryDrivers(String graphObjPath) {
		try {
			ContractionHierarchySet ggContraction = ContractionHierarchySerializationLibrary.readGraph(new File(graphObjPath));
			Graph gg = ggContraction.getGraph();
			
			driverNodes = new ConcreteMemoryDriver();
			driverNodes.setShapeType(FShape.POINT);
			driverNodes.getTableModel().addColumn("Label");
			driverNodes.getTableModel().addColumn("Type");
			Object[] row = new Object[2];
			
			Collection<Vertex> vertices = gg.getVertices();
			Iterator<Vertex> it = vertices.iterator();
			FPoint2D editableFeatureP = null;
			
			Hashtable<Edge, Edge> edges = new Hashtable<Edge, Edge>(); 
			
			while (it.hasNext()) {
				Vertex gv = it.next();
				Vertex v = gv.vertex;
				Iterator<Edge> out = gv.getOutgoing().iterator();
				while (out.hasNext()) {
					Edge e = out.next();
					edges.put(e, e);
				}
				row = new Object[2];
				row[0] = ValueFactory.createValue(v.getLabel());
				row[1] = ValueFactory.createValue(v.getClass().getName());
				editableFeatureP = new FPoint2D(v.getX(), v.getY());
				driverNodes.addShape(editableFeatureP, row);
			}
			driverEdges = new ConcreteMemoryDriver();
			driverEdges.setShapeType(FShape.LINE);
			driverEdges.getTableModel().addColumn("Type");
			driverEdges.getTableModel().addColumn("Name");
			driverEdges.getTableModel().addColumn("Mode");
			Iterator<Edge> edgeIterator = edges.values().iterator();
			Object[] rowEdge = new Object[3];
			while (edgeIterator.hasNext()) {
				Edge e = edgeIterator.next();
				if (e instanceof EdgeNarrative) {
					EdgeNarrative en = (EdgeNarrative) e;
					rowEdge = new Value[3];
					rowEdge[0] = ValueFactory.createValue(e.getClass().getName());
					rowEdge[1] = ValueFactory.createValue(en.getName());
					if (en.getMode() != null)
						rowEdge[2] = ValueFactory.createValue(en.getMode().toString());
					IGeometry geom = null;
					if (en.getGeometry() != null) {
						Geometry geomJTS = en.getGeometry();
						geom = FConverter.jts_to_igeometry(geomJTS);
					}
					else
					{
						GeneralPathX gp = new GeneralPathX();
						gp.moveTo(en.getFromVertex().getX(), en.getFromVertex().getY());
						gp.lineTo(en.getToVertex().getX(), en.getToVertex().getY());				
						geom = ShapeFactory.createPolyline2D(gp);					
					}
					if (rowEdge[1].toString() == null)
						rowEdge[1] = ValueFactory.createValue(" ");
					driverEdges.addGeometry(geom, rowEdge);
				}				
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}


	public boolean isEnabled() {
		IWindow w = PluginServices.getMDIManager().getActiveWindow();
		if (w instanceof IView) {
			return true;
		}
		return false;

	}

	public boolean isVisible() {
		IWindow w = PluginServices.getMDIManager().getActiveWindow();
		if (w instanceof IView)
			return true;
		return false;

	}

}
