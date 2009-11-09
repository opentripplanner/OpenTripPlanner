package org.opentripplanner.api.ws;

import org.opentripplanner.api.model.Itinerary;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Date;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sun.jersey.api.NotFoundException;
import com.sun.jersey.api.view.ImplicitProduces;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.opentripplanner.util.DateUtils;

import org.opentripplanner.api.ws.Response;
import org.opentripplanner.api.ws.Request;
import org.opentripplanner.api.ws.RequestInf;
import org.opentripplanner.api.ws.RequestInf.ModeType;
import org.opentripplanner.api.ws.RequestInf.OptimizeType;

/**
 *
 */
// NOTE - /ws/plan is the full path -- see web.xml
@Path("/plan")
@XmlRootElement
public class Planner {

    static final List<Itinerary> it = new LinkedList<Itinerary>();

    private final Request m_request = new Request();

    public Planner(
            @QueryParam(RequestInf.FROM) String from,
            @QueryParam(RequestInf.TO) String to,

            @QueryParam(RequestInf.DATE) String date,
            @QueryParam(RequestInf.TIME) String time,
            @QueryParam(RequestInf.DEPART_AFTER) Boolean departAfter,
            @QueryParam(RequestInf.ARRIVE_BY) Boolean arriveBy,

            @QueryParam(RequestInf.WALK) Double walk,
            @QueryParam(RequestInf.OPTIMIZE) List<OptimizeType> optList,
            @QueryParam(RequestInf.MODE) List<ModeType> modeList,
            @QueryParam(RequestInf.NUMBER_ITINERARIES) Integer max,
            @DefaultValue(MediaType.APPLICATION_JSON) @QueryParam(RequestInf.OUTPUT_FORMAT) String of) {

        m_request.setFrom(from);
        m_request.setTo(to);
        m_request.setDateTime(date, time);

        if (max != null)
            m_request.setNumItineraries(max);
        if (walk != null)
            m_request.setWalk(walk);
        if (arriveBy != null && arriveBy)
            m_request.setArriveBy();
        else if (departAfter != null && departAfter)
            m_request.setDepartAfter();

        if (optList != null && optList.size() > 0)
            m_request.addOptimize(optList);

        if (modeList != null && modeList.size() > 0)
            m_request.addMode(modeList);

        m_request.setOutputFormat(MediaType.valueOf(of));
    }

    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Response getItineraries() {
        return new Response(m_request);
        // m_request.toHtmlString();)
    }
}
