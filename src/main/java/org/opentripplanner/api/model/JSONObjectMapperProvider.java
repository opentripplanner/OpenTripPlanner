package org.opentripplanner.api.model;

import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * AgencyAndId is a third-party class in One Bus Away which represents a GTFS element's ID,
 * including an agency name as identifier scope information since more than one feed may be loaded
 * at once.
 * 
 * While this works when there is only one agency per feed, the true scope of identifiers is the
 * feed, and the same agency could appear in multiple feeds. We don't want the key "agencyId" to
 * appear in the final OTP API because it will eventually not represent an agency.
 * 
 * See this ticket: https://github.com/opentripplanner/OpenTripPlanner/issues/1352
 * 
 * And this proposal to gtfs-changes:
 * https://groups.google.com/d/msg/gtfs-changes/zVjEoNIPr_Y/4ngWCajPoS0J
 * 
 * Our solution is to serialize the AgencyAndId as a single string with a separator character
 * between the agency and ID. In future versions this scoped identifier will actually represent a
 * feed and ID. The important thing is that the API will remain the same, and identifiers fetched
 * from one API result can be used in another request with no conflicts.
 * 
 * Since AgencyAndId is a third-party class, we can't modify it with a custom serialization method
 * or annotations. Instead, we have to let Jackson know which custom serializer class applies to the
 * third-party type. According to http://wiki.fasterxml.com/JacksonHowToCustomSerializers "Jackson
 * 1.7 added ability to register serializers and deserializes via Module interface. This is the
 * recommended way to add custom serializers."
 * 
 * A Jackson "Module" is a group of extensions to default functionality, used for example to support
 * serializing new data types. Modules are registered with an ObjectMapper, which constructs
 * ObjectWriters, which are used to do the final JSON writing. In OTP the ObjectWriter construction
 * and JSON writing are performed automatically by Jersey.
 */

@Provider
@Produces(MediaType.APPLICATION_JSON)
public class JSONObjectMapperProvider implements ContextResolver<ObjectMapper> {

    private final ObjectMapper mapper;

    /**
     * Pre-instantiate a Jackson ObjectMapper that will be handed off to all incoming Jersey
     * requests, and used to construct the ObjectWriters that will produce JSON responses.
     */
    public JSONObjectMapperProvider() {
        // Create a module, i.e. a group of one or more Jackson extensions.
        // Our module includes a single class-serializer relationship.
        // Constructors are available for both unnamed, unversioned throwaway modules
        // and named, versioned reusable modules.
        mapper = new ObjectMapper()
                .registerModule(AgencyAndIdSerializer.makeModule())
                .setSerializationInclusion(Include.NON_NULL); // skip null fields
    }

    /**
     * When serializing any kind of result, use the same ObjectMapper. The "type" parameter will be
     * the type of the object being serialized, so you could provide different ObjectMappers for
     * different result types.
     */
    @Override
    public ObjectMapper getContext(Class<?> type) {
        return mapper;
    }

}
