package org.opentripplanner.routing.core;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents noteworthy events or errors that occur during the graphbuilding process.
 * 
 * This is in the routing subproject (rather than graphbuilder) to avoid making routing  
 * depend on the entire graphbuilder subproject. Graphbuilder already depends on routing. 
 * 
 * @author andrewbyrd
 */
public class GraphBuilderAnnotation implements Serializable {

	private static final long serialVersionUID = 20111201L; // YYYYMMDD

	private static final Logger LOG = LoggerFactory.getLogger(GraphBuilderAnnotation.class);

	private Object[] refs;
	
	private Variety variety;

	public GraphBuilderAnnotation (Variety variety, Object... refs) {
		this.refs = refs;
		this.variety = variety;
	}
	
	public Collection<Object> getReferencedObjects() {
		return Arrays.asList(refs);
	}

	public String getMessage() {
		return variety.getMessage(refs);
	}

	public Severity getLevel() {
		return variety.severity;
	}
	
	public void log(Logger log) {
    	String message = getMessage();
    	/* Yes, this is insane. But SLF4J hides log4j's ability to programmatically set log level. */
    	switch (getLevel()) {
    	case ERROR :
    		log.error(message);
    		break;
    	case WARN :
    		log.warn(message);
    		break;
    	default :
    		log.info(message);
    	}
	}
	
	public enum Severity {
		INFO,
		WARN,
		ERROR
	}

	public enum Variety {
		OVERTAKING_TRIP(Severity.WARN, ""),
		DUPLICATE_TRIP(Severity.WARN, ""),
		UNLINKED_STOP (Severity.WARN, "Stop %s not near any streets; it will not be usable");
		private final Severity severity;
		private final String formatString;
		Variety (Severity severity, String formatString) {
			this.severity = severity;
			this.formatString = formatString;
		}
		public String getMessage(Object... refs){
			return String.format(formatString, refs);
		}
	}
	
 	public static void logSummary (Iterable<GraphBuilderAnnotation> gbas) {
 		// an enummap would be nice, but Integers are immutable...
 		int[] counts = new int[Variety.values().length];
		LOG.info("Summary (number of each type of annotation):");
 		for (GraphBuilderAnnotation gba : gbas)
 			++counts[gba.variety.ordinal()];
 		for (Variety v : Variety.values())
 			LOG.warn("    {} - {}", v.toString(), counts[v.ordinal()]);
 	}
 	
}
