package org.opentripplanner.graph_builder.services.osm;

import org.opentripplanner.graph_builder.module.osm.PortlandCustomNamer;
import org.opentripplanner.openstreetmap.model.OSMWithTags;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.graph.Graph;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * For when CreativeNamePicker/WayPropertySet is just not powerful enough.
 * 
 * @author novalis
 * 
 */
public interface CustomNamer {

    public String name(OSMWithTags way, String defaultName);

    public void nameWithEdge(OSMWithTags way, StreetEdge edge);

    public void postprocess(Graph graph);

    public void configure(JsonNode config);

    public class CustomNamerFactory {

        /**
         * Create a custom namer if needed, return null if not found / by default.
         */
        public static CustomNamer fromConfig(JsonNode config) {
            String type = null;
            if (config == null) {
                /* Empty block, fallback to default */
                return null;
            } else if (config.isTextual()) {
                /* Simplest form: { osmNaming : "portland" } */
                type = config.asText();
            } else if (config.has("type")) {
                /* Custom namer with a type: { osmNaming : { type : "foobar", param1 : 42 } } */
                type = config.path("type").asText(null);
            }
            if (type == null) {
                return null;
            }

            CustomNamer retval;
            switch (type) {
            case "portland":
                retval = new PortlandCustomNamer();
                break;
            default:
                throw new IllegalArgumentException(String.format("Unknown osmNaming type: '%s'",
                        type));
            }
            // Configure the namer
            retval.configure(config);
            return retval;
        }
    }
}
