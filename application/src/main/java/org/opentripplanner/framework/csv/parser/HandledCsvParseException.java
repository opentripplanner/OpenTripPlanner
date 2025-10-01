package org.opentripplanner.framework.csv.parser;

/**
 * Signal a handled CSV parse exception. The error is added to the issue store - nothing needs to
 * be done, the parser may proceed to the next line.
 */
public class HandledCsvParseException extends Exception {}
