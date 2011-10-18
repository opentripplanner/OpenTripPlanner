/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (props, at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.routing.impl;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.List;

import javassist.bytecode.ClassFile;
import javassist.util.proxy.FactoryHelper;

import javax.annotation.PostConstruct;

import org.onebusaway.gtfs.impl.calendar.CalendarServiceImpl;
import org.onebusaway.gtfs.model.calendar.CalendarServiceData;
import org.onebusaway.gtfs.services.calendar.CalendarService;
import org.opentripplanner.model.GraphBundle;
import org.opentripplanner.routing.contraction.ContractionHierarchySet;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.services.GraphRefreshListener;
import org.opentripplanner.routing.services.GraphService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link GraphService} that loads the graph from a file.
 * 
 * You can specify the location of the graph in a number of ways:
 * 
 * 1) Call {@link #setBundle(GraphBundle)} to set the graph bundle location
 * 
 * 2) Call {@link #setGraphPath(File)} to set the graph file path directly
 * 
 * 3) Call {@link #setContractionHierarchySet(ContractionHierarchySet)} to
 * specify the graph itself.
 * 
 * @author bdferris
 * @see GraphService
 */
public class GraphServiceImpl implements GraphService {

  private static final Logger _log = LoggerFactory.getLogger(GraphServiceImpl.class);
	
  private GraphBundle _bundle;

  private boolean _createEmptyGraphIfNotFound = false;

  private ContractionHierarchySet _contractionHierarchySet;

  private CalendarServiceImpl _calendarService;

  private List<GraphRefreshListener> _graphRefreshListeners;

  public void setBundle(GraphBundle bundle) {
    _bundle = bundle;
  }

  /**
   * By default, we throw an exception if the graph path is not found. Set this
   * to true to indicate that a default empty graph should be creaetd instead.
   * 
   * @param createEmptyGraphIfNotFound
   */
  public void setCreateEmptyGraphIfNotFound(boolean createEmptyGraphIfNotFound) {
    _createEmptyGraphIfNotFound = createEmptyGraphIfNotFound;
  }

  public void setContractionHierarchySet(
      ContractionHierarchySet contractionHierarchySet) {
    _contractionHierarchySet = contractionHierarchySet;

    CalendarServiceData data = _contractionHierarchySet.getService(CalendarServiceData.class);
    if (data != null) {
      CalendarServiceImpl calendarService = new CalendarServiceImpl();
      calendarService.setData(data);
      _calendarService = calendarService;
    } else {
      _calendarService = null;
    }
  }

  @Autowired
  public void setGraphRefreshListeners(
      List<GraphRefreshListener> graphRefreshListeners) {
    _graphRefreshListeners = graphRefreshListeners;
  }

  /****
   * {@link GraphService} Interface
   ****/

  @Override
  @PostConstruct
  // This means it will run on startup
  public void refreshGraph() {
    readGraph();
    notifyListeners();
  }

  @Override
  public ContractionHierarchySet getContractionHierarchySet() {
    return _contractionHierarchySet;
  }

  @Override
  public Graph getGraph() {
    return _contractionHierarchySet.getGraph();
  }

  @Override
  public CalendarService getCalendarService() {
    return _calendarService;
  }

  /****
   * Private Methods
   ****/

  private void readGraph() {

        File path = null;
        if (_bundle == null) {
            throw new RuntimeException("setBundle() mustbe called before readGraph()");
        }
        try {

            File extraClassPath = _bundle.getExtraClassPath();
            URL[] url;

            url = new URL[] { new URL("file://" + extraClassPath + "/") };
            ClassLoader oldLoader = getClass().getClassLoader();
            URLClassLoader loader = new URLClassLoader(url, oldLoader);

            path = _bundle.getGraphPath();

            if (path == null || !path.exists()) {
                if (!_createEmptyGraphIfNotFound) {
                    _log.error("Graph not found. Verify path to stored graph: " + path);
                    throw new IllegalStateException("graph path not found: " + path);
                }

                /****
                 * Create an empty graph if not graph is found
                 */
                Graph graph = new Graph();
                graph.setBundle(_bundle);
                List<TraverseOptions> modeList = Collections.emptyList();
                setContractionHierarchySet(new ContractionHierarchySet(graph, modeList));
                return;
            }

            ContractionHierarchySet chs = new GraphSerializationLibrary(loader).readGraph(path);
            setContractionHierarchySet(chs);
        } catch (Exception ex) {
            throw new IllegalStateException("error loading graph from " + path, ex);
        }
  }

  private void notifyListeners() {
    if( _graphRefreshListeners == null)
      return;
    for( GraphRefreshListener listener : _graphRefreshListeners)
      listener.handleGraphRefresh(this);
  }

}
