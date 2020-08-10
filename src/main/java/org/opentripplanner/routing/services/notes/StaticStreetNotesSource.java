package org.opentripplanner.routing.services.notes;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.opentripplanner.common.model.T2;
import org.opentripplanner.model.StreetNote;
import org.opentripplanner.routing.edgetype.TemporaryPartialStreetEdge;
import org.opentripplanner.routing.graph.Edge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;

/**
 * A notes source of static notes, usually created at graph building stage and not modified
 * thereafter.
 * 
 * @author laurent
 */
public class StaticStreetNotesSource implements StreetNotesSource, Serializable {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(StaticStreetNotesSource.class);

    /**
     * Notes for street edges. No need to synchronize access to the map as they will not be
     * concurrent write access (no notes for temporary edges, we use notes from parent).
     */
    private final SetMultimap<Edge, MatcherAndStreetNote> notesForEdge = HashMultimap
            .<Edge, MatcherAndStreetNote> create();

    /**
     * Set of unique matchers, kept during building phase, used for interning (lots of note/matchers
     * are identical).
     */
    private transient Map<T2<NoteMatcher, StreetNote>, MatcherAndStreetNote> uniqueMatchers = new HashMap<>();

    StaticStreetNotesSource() {
    }

    void addNote(Edge edge, StreetNote note, NoteMatcher matcher) {
        if (LOG.isDebugEnabled())
            LOG.debug("Adding note {} to {} with matcher {}", note, edge, matcher);
        notesForEdge.put(edge, buildMatcherAndAlert(matcher, note));
    }

    /**
     * Return the set of notes applicable for this state / backedge pair.
     * @return The set of notes or null if empty.
     */
    @Override
    public Set<MatcherAndStreetNote> getNotes(Edge edge) {
        /* If the edge is temporary, we look for notes in it's parent edge. */
        if (edge instanceof TemporaryPartialStreetEdge) {
            edge = ((TemporaryPartialStreetEdge) edge).getParentEdge();
        }
        Set<MatcherAndStreetNote> maas = notesForEdge.get(edge);
        if (maas == null || maas.isEmpty()) {
            return null;
        }
        return maas;
    }

    /**
     * Remove all notes attached to this edge. NOTE: this should only be called within a graph
     * building context (or unit testing).
     * 
     * @param edge
     */
    void removeNotes(Edge edge) {
        if (LOG.isDebugEnabled())
            LOG.debug("Removing notes for edge: {}", edge);
        notesForEdge.removeAll(edge);
    }

    /**
     * Create a MatcherAndAlert, interning it if the note and matcher pair is already created. Note:
     * we use the default Object.equals() for matchers, as they are mostly already singleton
     * instances.
     * 
     * @param noteMatcher
     * @param note
     * @return
     */
    private MatcherAndStreetNote buildMatcherAndAlert(NoteMatcher noteMatcher, StreetNote note) {
        T2<NoteMatcher, StreetNote> key = new T2<>(noteMatcher, note);
        MatcherAndStreetNote interned = uniqueMatchers.get(key);
        if (interned != null) {
            return interned;
        }
        MatcherAndStreetNote ret = new MatcherAndStreetNote(noteMatcher, note);
        uniqueMatchers.put(key, ret);
        return ret;
    }
}
