package org.opentripplanner.custom_api;

import org.glassfish.grizzly.http.server.Request;
import org.opentripplanner.api.common.Message;
import org.opentripplanner.api.common.RoutingResource;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.api.mapping.PlannerErrorMapper;
import org.opentripplanner.api.mapping.TripPlanMapper;
import org.opentripplanner.api.mapping.TripSearchMetadataMapper;
import org.opentripplanner.api.model.error.PlannerError;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.RoutingService;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.api.response.RoutingResponse;
import org.opentripplanner.routing.vertextype.SplitterVertex;
import org.opentripplanner.standalone.server.OTPServer;
import org.opentripplanner.standalone.server.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import java.util.*;
import java.util.stream.Collectors;

import org.opentripplanner.routing.graph.*;

@Path("routers/{ignoreRouterId}/CheckVertexExists")
public class GraphVertexExists {

    @Deprecated @PathParam("ignoreRouterId")
    private String ignoreRouterId;

    @Context
    private OTPServer otpServer;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response GraphAddEdge(@QueryParam("VertexLabel") String label) {
        boolean res = otpServer.getRouter().graph.getVertex(label) != null;
        return Response.status(200).entity(res ? "Vertex exists" : "Vertex does not exist").build();
    }
}
