package org.opentripplanner.jags.test;

import java.io.File;

import org.opentripplanner.jags.core.Graph;
import org.opentripplanner.jags.edgetype.loader.GTFSHopLoader;
import org.opentripplanner.jags.edgetype.loader.NetworkLinker;
import org.opentripplanner.jags.gtfs.GtfsContext;
import org.opentripplanner.jags.gtfs.GtfsLibrary;

public class ConstantsForTests {
  
  public static final String CALTRAIN_GTFS = "src/test/resources/caltrain_gtfs.zip";
  
  public static final String PORTLAND_GTFS = "src/test/resources/google_transit.zip";
  
  private static ConstantsForTests instance = null;
 
  private Graph portlandGraph = null;
  private GtfsContext portlandContext = null;
  
  private ConstantsForTests() {
    
  }
  
  public static ConstantsForTests getInstance() {
	  if (instance == null) {
		  instance = new ConstantsForTests(); 
	  }
	  return instance;
  }
  
  public GtfsContext getPortlandContext() {
	  setupPortland();
	  return portlandContext;
  }
  
  public Graph getPortlandGraph() {
	 if (portlandGraph == null) {
		 setupPortland();
	 }
	 return portlandGraph;
  }

  private void setupPortland() {
	try {
		 portlandContext = GtfsLibrary.readGtfs(new File(ConstantsForTests.PORTLAND_GTFS));
		 portlandGraph = new Graph();
		 GTFSHopLoader hl = new GTFSHopLoader(portlandGraph, portlandContext);
		 hl.load();
	 } catch (Exception e) {
		 e.printStackTrace();
		 throw new RuntimeException(e);
	 }
	 NetworkLinker nl = new NetworkLinker(portlandGraph);
	 nl.createLinkage();
  }
}
