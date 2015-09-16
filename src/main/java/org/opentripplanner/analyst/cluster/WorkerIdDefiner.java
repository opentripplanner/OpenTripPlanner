package org.opentripplanner.analyst.cluster;

import ch.qos.logback.core.PropertyDefinerBase;

/**
 * A class that allows the logging framework to access the worker ID; with a custom logback config
 * this can be used to print the machine ID with each log message. This is useful if you have multiple
 * workers logging to the same log aggregation service.
 *
 * This does seem like it should be a static class of AnalystWorker, but AnalystWorker needs this
 * class loaded to initialize its logger which is a static field, so it would have to be at the top
 * of the file, above the logger definition, which is ugly and confusing so we leave it as its own
 * bona fide class.
 *
 * It would seem that Mapped Diagnostic Contexts would be ideal for this purpose, but they are
 * thread-scoped, and computation takes place in multiple threads; we need this to be JVM-scoped.
 */
public class WorkerIdDefiner extends PropertyDefinerBase {
    @Override public String getPropertyValue() {
        return AnalystWorker.machineId;
    }
}