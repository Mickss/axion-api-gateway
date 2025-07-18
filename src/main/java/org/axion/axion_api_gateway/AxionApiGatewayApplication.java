package org.axion.axion_api_gateway;

import io.javalin.Javalin;
import io.javalin.http.Context;
import org.axion.axion_api_gateway.config.AppConfig;
import org.axion.axion_api_gateway.config.ConfigNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AxionApiGatewayApplication {

    private static final Logger log = LoggerFactory.getLogger(AxionApiGatewayApplication.class);

    private static AppConfig appConfig;
    private static UserSession userSession;

    public static void main(String[] args) {
        appConfig = new AppConfig();
        userSession = new UserSession(appConfig);

        Javalin app = Javalin.create(
                        config -> config.bundledPlugins.enableCors(
                                cors -> cors.addRule(
                                        rule -> {
                                            rule.allowHost(
                                                    "https://app.disc-golf.pl",
                                                    "http://localhost:5173",
                                                    "https://localhost:7065"
                                            );
                                            rule.allowCredentials = true;
                                        }
                                )))
                .start(24001);

        app.get("/api/current-session", AxionApiGatewayApplication::currentSession);

        app.get("/api/*", AxionApiGatewayApplication::forwardRequest);
        app.post("/api/*", AxionApiGatewayApplication::forwardRequest);
        app.put("/api/*", AxionApiGatewayApplication::forwardRequest);
        app.patch("/api/*", AxionApiGatewayApplication::forwardRequest);
        app.delete("/api/*", AxionApiGatewayApplication::forwardRequest);

        app.exception(RequestValidationException.class, (e, ctx) -> ctx.status(e.getHttpStatus().getStatusCode())
                .result(e.getMessage()));
    }

    private static void currentSession(Context ctx) {
        UserDTO userDTO = userSession.getLoggedInUser(ctx.req());
        ctx.status(200);
        ctx.json(userDTO);
    }

    private static void forwardRequest(Context ctx) {
        log.info("Will forward request: {}", ctx.req().getRequestURI());
        String requestURI = ctx.req().getRequestURI();
        UserDTO userDTO = null;
        if (!isPublicEndpoint(requestURI)) {
            userDTO = userSession.getLoggedInUser(ctx.req());
        } else {
            log.info("Skipping JWT validation for public endpoint: {}", requestURI);
        }
        RequestHandler requestHandler = new RequestHandler(appConfig);
        try {
            String targetUrl = requestHandler.prepareTargetUrl(ctx);
            requestHandler.forwardRequestForUrl(ctx, targetUrl, userDTO);
        } catch (ConfigNotFoundException e) {
            log.warn("Cannot recognise target URL for path: {}", ctx.path());
            ctx.status(404).result("Cannot recognise target URL for path: " + ctx.path());
        }
    }

    private static boolean isPublicEndpoint(String requestURI) {
        String[] uriSegments = requestURI.split("/");
        if (uriSegments.length > 3) {
            return uriSegments[3].equals("public");
        }
        return false;
    }
}
