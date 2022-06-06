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

@Path("routers/{ignoreRouterId}/add_edge")
public class TestGraphAddEdge {

    @Deprecated @PathParam("ignoreRouterId")
    private String ignoreRouterId;

    @Context
    private OTPServer otpServer;

    @PUT
    @Produces(MediaType.TEXT_PLAIN)
    public Response GraphAddEdge(@QueryParam("car_speed_update_file_path") String file_path) {
        System.out.println(file_path);
        List<String> file_lines = ReadFromFile(file_path);
        String[] line_split;
        String l1, l2;
        int time;
        float car_speed;
        int total_requests, successful_requests;
        total_requests = successful_requests = 0;
        for (String line : file_lines) {
            line_split = line.split(" ", 0);
            l1 = line_split[0];
            l2 = line_split[1];
            time = convertTimeToMins(line_split[2]);
            car_speed = Float.parseFloat(line_split[3]);

            successful_requests += AddCarSpeedToEdge(otpServer.getRouter().graph, l1, l2, time, car_speed);
            total_requests += 1;

            System.out.println(line);
            System.out.println(successful_requests);
        }
        String message = "Out of " + Integer.toString(total_requests) + " requests, " +
                Integer.toString(successful_requests) + "requests succeeded";
        return Response.status(200).entity(message).build();
    }

    private List<String> ReadFromFile(String file_path) {
        BufferedReader reader;
        List<String> file_lines = new ArrayList<>();
        try {
            reader = new BufferedReader(new FileReader(file_path));
            String line = reader.readLine();
            while (line != null) {
                file_lines.add(line);
                // read next line
                line = reader.readLine();
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return file_lines;
    }

    private int AddCarSpeedToEdge(Graph graph, String l1, String l2, int time, float car_speed) {
        String l1_org = l1, l2_org = l2;
        l1 = "osm:node:" + l1_org; l2 = "osm:node:" + l2_org;
        Vertex v1 = graph.getVertex(l1), v2 = graph.getVertex(l2);
        for (StreetEdge e: v1.getOutgoing().stream()
                .filter(StreetEdge.class::isInstance)
                .map(StreetEdge.class::cast)
                .collect(Collectors.toList())) {
            if (e.getToVertex() == v2) {
                e.addCarSpeed(time, car_speed);
                return 1;
            } else if (e.getToVertex() instanceof SplitterVertex) {     // v1 -> v2 became v1 -> v -> v2 due to split.
                System.out.println(e);
                SplitterVertex v = (SplitterVertex) e.getToVertex();
                if (v.nextNodeId == Long.parseLong(l2_org)) {
                    e.addCarSpeed(time, car_speed);
                    for (StreetEdge e2: v.getOutgoing().stream()
                            .filter(StreetEdge.class::isInstance)
                            .map(StreetEdge.class::cast)
                            .collect(Collectors.toList())) {
                        System.out.println(e2);
                        if (e2.getToVertex() == v2) {
                            e2.addCarSpeed(time, car_speed);
                            return 1;
                        }
                    }
                }
            }
        }
        return 0;
    }

    private int convertTimeToMins(String time) {
        String[] time_split = time.split(":", 0);
        return Integer.parseInt(time_split[0])*60 + Integer.parseInt(time_split[1]);
    }
}
