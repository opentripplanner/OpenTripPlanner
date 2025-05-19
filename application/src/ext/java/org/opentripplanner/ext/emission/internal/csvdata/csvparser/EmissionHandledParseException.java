package org.opentripplanner.ext.emission.internal.csvdata.csvparser;

/**
 * Signal a handled parse exception. The error is added to the  issue store - nothing need to
 * be done, the parser may procceed to the next line.
 */
public class EmissionHandledParseException extends Exception {}
