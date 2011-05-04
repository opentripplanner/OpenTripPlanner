package org.opentripplanner.routing.impl;

import java.io.File;
import java.io.InvalidClassException;
import java.util.Collections;
import java.util.List;

import javax.annotation.PostConstruct;

import org.onebusaway.gtfs.impl.calendar.CalendarServiceImpl;
import org.onebusaway.gtfs.model.calendar.CalendarServiceData;
import org.onebusaway.gtfs.services.calendar.CalendarService;
import org.opentripplanner.model.GraphBundle;
import org.opentripplanner.routing.contraction.ContractionHierarchy;
import org.opentripplanner.routing.contraction.ContractionHierarchySet;
import org.opentripplanner.routing.contraction.ModeAndOptimize;
import org.opentripplanner.routing.core.Graph;
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

  private File _graphPath;

  private boolean _createEmptyGraphIfNotFound = false;

  private ContractionHierarchySet _contractionHierarchySet;

  private CalendarServiceImpl _calendarService;

  private List<GraphRefreshListener> _graphRefreshListeners;

  public void setBundle(GraphBundle bundle) {
    _bundle = bundle;
  }

  public void setGraphPath(File graphPath) {
    _graphPath = graphPath;
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

    if (_bundle != null)
      path = _bundle.getGraphPath();

    if (_graphPath != null)
      path = _graphPath;

    if (path == null || !path.exists()) {
      if (!_createEmptyGraphIfNotFound) {
    	  _log.error("Graph not found. Verify path to stored graph: " + path);
    	  throw new IllegalStateException("graph path not found: " + path);
      }

      /****
       * Create an empty graph if not graph is found
       */
      Graph graph = new Graph();
      List<ModeAndOptimize> modeList = Collections.emptyList();
      setContractionHierarchySet(new ContractionHierarchySet(graph, modeList));
      return;
    }

    try {
      ContractionHierarchySet chs = ContractionHierarchySerializationLibrary.readGraph(path);
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
