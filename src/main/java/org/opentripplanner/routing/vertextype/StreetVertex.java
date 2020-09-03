package org.opentripplanner.routing.vertextype;

import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.util.I18NString;
import org.opentripplanner.util.LocalizedString;
import org.opentripplanner.util.NonLocalizedString;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

/**
 * Abstract base class for vertices in the street layer of the graph.
 * This includes both vertices representing intersections or points (IntersectionVertices)
 * and Elevator*Vertices.
 */
public abstract class StreetVertex extends Vertex {

    private static final long serialVersionUID = 1L;

    private static final I18NString UNNAMED_STREET = new LocalizedString("unnamedStreet", (String[]) null);

    public StreetVertex(Graph g, String label, Coordinate coord, I18NString streetName) {
        this(g, label, coord.x, coord.y, streetName);
    }

    public StreetVertex(Graph g, String label, double x, double y, I18NString streetName) {
        super(g, label, x, y, streetName);
    }

    /**
     * Creates intersection name out of all outgoing names
     * <p>
     * This can be:
     * - name of the street if it is only 1
     * - unnamedStreed (localized in requested language) if it doesn't have a name
     * - corner of 0 and 1 (localized corner of zero and first street in the corner)
     *
     * @param locale Wanted locale
     * @return already localized street names and non-localized corner of x and unnamedStreet
     */
    public I18NString getIntersectionName(Locale locale) {
        return getNonBogusName(locale).orElseGet(() -> getBogusName(locale));
    }

    private Optional<I18NString> getNonBogusName(Locale locale) {
        List<String> uniqueNames = getOutgoing().stream()
                .filter(e -> e instanceof StreetEdge && !e.hasBogusName())
                .map(e -> e.getName(locale))
                .distinct()
                .limit(2)
                .collect(toList());
        if (uniqueNames.size() > 1) {
            return Optional.of(new LocalizedString("corner", new String[]{uniqueNames.get(0), uniqueNames.get(1)}));
        } else if (uniqueNames.size() == 1) {
            return Optional.of(new NonLocalizedString(uniqueNames.get(0)));
        } else {
            return Optional.empty();
        }
    }

    private I18NString getBogusName(Locale locale) {
        return getOutgoing().stream()
                .filter(e -> e instanceof StreetEdge && e.hasBogusName())
                .map(e -> e.getName(locale))
                .findFirst()
                .map(NonLocalizedString::new)
                .map(I18NString.class::cast)
                .orElse(UNNAMED_STREET);
    }
}
