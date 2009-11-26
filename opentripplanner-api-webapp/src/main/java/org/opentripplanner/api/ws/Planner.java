package org.opentripplanner.api.ws;

import java.util.List;
import java.util.Vector;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jettison.json.JSONException;
import org.opentripplanner.api.model.Itinerary;
import org.opentripplanner.api.model.Leg;
import org.opentripplanner.api.model.Place;
import org.opentripplanner.api.model.TripPlan;
import org.opentripplanner.api.ws.RequestInf.ModeType;
import org.opentripplanner.api.ws.RequestInf.OptimizeType;
import org.opentripplanner.narrative.model.Narrative;
import org.opentripplanner.narrative.model.NarrativeItem;
import org.opentripplanner.narrative.model.NarrativeSection;
import org.opentripplanner.narrative.services.NarrativeService;
import org.opentripplanner.util.GeoJSONBuilder;
import org.opentripplanner.util.PolylineEncoder;
import org.springframework.beans.factory.annotation.Autowired;

import com.sun.jersey.api.spring.Autowire;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;

/**
 *
 */
// NOTE - /ws/plan is the full path -- see web.xml
@Path("/plan")
@XmlRootElement
@Autowire
public class Planner {

    private NarrativeService _narrativeService;

    @Autowired
    public void setNarrativeService(NarrativeService narrativeService) {
        _narrativeService = narrativeService;
    }

    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Response getItineraries(
            @QueryParam(RequestInf.FROM) String fromPlace,
            @QueryParam(RequestInf.TO) String toPlace,
            @QueryParam(RequestInf.DATE) String date,
            @QueryParam(RequestInf.TIME) String time,
            @QueryParam(RequestInf.DEPART_AFTER) Boolean departAfter,
            @QueryParam(RequestInf.ARRIVE_BY) Boolean arriveBy,
            @QueryParam(RequestInf.WALK) Double walk,
            @QueryParam(RequestInf.OPTIMIZE) List<OptimizeType> optList,
            @QueryParam(RequestInf.MODE) List<ModeType> modeList,
            @QueryParam(RequestInf.NUMBER_ITINERARIES) Integer max,
            @DefaultValue(MediaType.APPLICATION_JSON) @QueryParam(RequestInf.OUTPUT_FORMAT) String of)
            throws JSONException {

        Request request = new Request();
        request.setFrom(fromPlace);
        request.setTo(toPlace);
        request.setDateTime(date, time);

        if (max != null)
            request.setNumItineraries(max);
        if (walk != null)
            request.setWalk(walk);
        if (arriveBy != null && arriveBy)
            request.setArriveBy();
        else if (departAfter != null && departAfter)
            request.setDepartAfter();

        if (optList != null && optList.size() > 0)
            request.addOptimize(optList);

        if (modeList != null && modeList.size() > 0)
            request.addMode(modeList);

        request.setOutputFormat(MediaType.valueOf(of));

        List<Narrative> narratives = _narrativeService.plan(request.getFrom(), request.getTo(),
                request.getDateTime(), request.isArriveBy());

        TripPlan plan = new TripPlan();

        for (Narrative narrative : narratives) {

            Itinerary itinerary = new Itinerary();
            plan.addItinerary(itinerary);

            for (NarrativeSection section : narrative.getSections()) {
                Leg leg = new Leg();
                leg.mode = getTransportationModeForSection(section);

                leg.legGeometry = PolylineEncoder.createEncodings(section.getGeometry());
                leg.from = getPlaceForSection(section, true);
                leg.to = getPlaceForSection(section, false);
                itinerary.addLeg(leg);
            }
        }

        Response response = new Response(request);
        response.plan = plan;
        return response;
    }

    private String getTransportationModeForSection(NarrativeSection section) {
        if (section.getMode() == null)
            return "Bus";
        switch (section.getMode()) {
        case WALK:
            return "Walk";
        case BUS:
            return "Bus";
        case TRAM:
            return "Tram";
        default:
            return "Other";
        }
    }

    private Place getPlaceForSection(NarrativeSection section, boolean isFrom) throws JSONException {

        Point point = getEndPoint(section, isFrom);

        Place place = new Place();
        place.name = "name";
        place.geometry = GeoJSONBuilder.getGeometryAsJsonString(point);
        return place;
    }

    private Point getEndPoint(NarrativeSection section, boolean first) {

        Geometry geometry = getGeometry(section, first);

        if (geometry instanceof Point) {
            return (Point) geometry;
        } else if (geometry instanceof LineString) {
            LineString lineString = (LineString) geometry;
            return first ? lineString.getStartPoint() : lineString.getEndPoint();
        } else {
            return geometry.getCentroid();
        }
    }

    private Geometry getGeometry(NarrativeSection section, boolean first) {
        if (section.getGeometry() != null)
            return section.getGeometry();
        Vector<NarrativeItem> items = section.getItems();
        NarrativeItem item = first ? items.get(0) : items.get(items.size() - 1);
        return item.getGeometry();
    }
}
