package org.opentripplanner.netex.loader.parser;

import org.opentripplanner.netex.index.NetexEntityIndex;
import org.rutebanken.netex.model.VersionFrame_VersionStructure;
import org.slf4j.Logger;

import java.util.Collection;

/**
 * An abstract parser of given type T. Enforce two steps parsing:
 * <ol>
 *     <li>parse(...)</li>
 *     <li>setResultOnIndex(...)</li>
 * </ol>
 */
@SuppressWarnings("SameParameterValue")
abstract class NetexParser<T> {

    /** Perform parsing and keep the parsed objects internally. */
    abstract void parse(T node);

    /** Add the result - the parsed objects - to the index. */
    abstract void setResultOnIndex(NetexEntityIndex netexIndex);


    /* static methods for logging unhandled elements - this ensure consistent logging. */

    /**
     * Currently a lot of elements on a frame is skipped. If any of these elements are pressent
     * we print a warning for elements that might be relevant for OTP and an info message for
     * none relevant elements.
     */
    static void verifyCommonUnusedPropertiesIsNotSet(Logger log, VersionFrame_VersionStructure rel) {
        // Direct members of VersionFrame_VersionStructure
        warnOnMissingMapping(log, rel.getTypeOfFrameRef());
        warnOnMissingMapping(log, rel.getBaselineVersionFrameRef());
        warnOnMissingMapping(log, rel.getCodespaces());
        warnOnMissingMapping(log, rel.getFrameDefaults());
        warnOnMissingMapping(log, rel.getVersions());
        warnOnMissingMapping(log, rel.getTraces());
        warnOnMissingMapping(log, rel.getContentValidityConditions());

        // Members of super class DataManagedObjectStructure
        informOnElementIntentionallySkipped(log, rel.getKeyList());
        informOnElementIntentionallySkipped(log, rel.getExtensions());
        warnOnMissingMapping(log, rel.getBrandingRef());
    }


    /**
     * Log a warning for Netex elements witch is not mapped. There might be something wrong
     * with the data or there might be something wrong with the Netex data import(ignoring these
     * elements). The element should be relevant to OTP. OTP do not support Netex 100%, but
     * elements in Nordic profile, see https://enturas.atlassian.net/wiki/spaces/PUBLIC/overview
     * should be supported.
     * <p>
     * If you get this warning and think the element should be mapped, please feel free to report
     * an issue on GitHub.
     */
    static void warnOnMissingMapping(Logger log, Object rel) {
        if(rel == null) return;
        if(rel instanceof Collection) throw new IllegalArgumentException("Do not pass in collections to this method.");
        log.warn("Netex import - Element mapping is missing for {}.", rel.getClass().getName());
    }

    /**
     * Unsupported elements are not relevant for Transit Routing, if you really think they
     * should be used in transit routing feel free to report an issue OTP GitHub.
     */
    static void informOnElementIntentionallySkipped(Logger log, Object rel) {
        if(rel == null) return;
        if(rel instanceof Collection) throw new IllegalArgumentException("Do not pass in collections to this method.");
        log.info("Netex import - Element skipped: {}", rel.getClass().getName());
    }

}
