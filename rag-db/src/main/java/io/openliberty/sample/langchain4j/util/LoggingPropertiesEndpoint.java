package io.openliberty.sample.langchain4j.util;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("properties")
public class LoggingPropertiesEndpoint {

    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Response setSharedProperties() {
        Globals.setEnableLog(false);
        return Response.ok("Updated shared class successfully.")
                       .type(MediaType.TEXT_PLAIN)
                       .build();
    }

}