package org.opentripplanner.api;

import java.util.Map;

import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.onebusaway.gtfs.model.Route;
import org.opentripplanner.api.model.AgencyAndIdSerializer;
import org.opentripplanner.routing.edgetype.TransitBoardAlight;
import org.opentripplanner.routing.graph.Graph;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;

/**
 * This file contains experimental classes demonstrating how to avoid using Jersey.
 * It would work well with ReflectiveQueryScraper.
 * Of course then the API docs would have to be maintained manually.
 * @author abyrd
 */
public class OTPHttpHandler extends HttpHandler {

    private final ObjectMapper xmlMapper = new XmlMapper();
    private final ObjectMapper jsonMapper = new ObjectMapper();
    private final Map <String, OTPHandler> handlers = Maps.newHashMap(); 
    private final Graph graph;
    
    public OTPHttpHandler (Graph graph) {
        this.graph = graph;
        handlers.put("routes", new RoutesHandler());
        handlers.put("plan",   new PlanHandler());
        Module module = AgencyAndIdSerializer.makeModule();
        xmlMapper.registerModule(module);
        jsonMapper.registerModule(module);
    }
    
    @Override
    public void service(Request req, Response resp) throws Exception {
        try {
            OTPRequest oreq = new OTPRequest(req, graph);
            Object result = handlers.get(oreq.action).handle(oreq);
            ObjectMapper mapper;
            if (oreq.sfmt == SerializeFormat.XML) {
                resp.setContentType("application/xml");
                mapper = xmlMapper;
            } else {
                resp.setContentType("application/json");
                mapper = jsonMapper;
            }
            resp.setStatus(200);
            mapper.writeValue(resp.getNIOOutputStream(), result);            
        } catch (Exception ex) {
            resp.setStatus(500);
            resp.setContentType("text/plain");
            resp.getNIOWriter().write("Error: " + ex.toString());
        }
    }

}

interface OTPHandler { public Object handle (OTPRequest oreq); }

class RoutesHandler implements OTPHandler {
    @Override
    public Object handle (OTPRequest oreq) {
        Map<String, Route> routes = Maps.newHashMap();
        for (TransitBoardAlight ba : Iterables.filter(oreq.graph.getEdges(), TransitBoardAlight.class)) {
            Route route = ba.getPattern().route;
            routes.put(route.getId().toString(), route);
        }
        if (oreq.id != null) {
            return routes.get(oreq.id);
        } else {
            return routes;
        }
    }
}

class PlanHandler implements OTPHandler {
    @Override
    public Object handle (OTPRequest oreq) {
        return oreq.params;
    }
}

enum SerializeFormat { XML, JSON }

class OTPRequest {
    
    Graph graph;
    String[] parts;
    String action;
    String id;
    SerializeFormat sfmt;
    Map<String,String> params = Maps.newHashMap();

    public OTPRequest (Request req, Graph graph) {
        this.graph = graph;
        for (String key : req.getParameterNames()) {
            params.put(key, req.getParameter(key));
        }
        String path = req.getPathInfo();
        sfmt = SerializeFormat.JSON;
        if (req.getHeader("Accept").contains("application/xml")) {
            sfmt = SerializeFormat.XML;
        }
        if (req.getHeader("Accept").contains("application/json")) {
            sfmt = SerializeFormat.JSON;
        }
        if (path.endsWith(".xml")) {
            path = path.substring(0, path.length() - 4);
            sfmt = SerializeFormat.XML;
        };
        if (path.endsWith(".json")) {
            path = path.substring(0, path.length() - 5);
            sfmt = SerializeFormat.JSON;
        };
        parts = path.split("/");
        // path always begins with a slash, so part 0 is empty
        if (parts.length > 1) action = parts[1];
        if (parts.length > 2) id = parts[2];
    }
    
}