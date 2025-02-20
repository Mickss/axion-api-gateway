package org.axion.axion_api_gateway;

import io.javalin.http.Context;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.axion.axion_api_gateway.config.AppConfig;
import org.axion.axion_api_gateway.config.ConfigNotFoundException;
import org.axion.axion_api_gateway.config.ServiceConfig;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class RequestHandler {

    private static final Logger log = LoggerFactory.getLogger(RequestHandler.class);

    private final OkHttpClient client = new OkHttpClient();
    private final AppConfig appConfig;

    public RequestHandler(AppConfig appConfig) {
        this.appConfig = appConfig;
    }

    public String prepareTargetUrl(Context ctx) throws ConfigNotFoundException {
        String targetPath = ctx.path().replace("/api/", "");
        String targetServiceName = targetPath.split("/")[0];
        ServiceConfig serviceConfig = appConfig.getServiceConfig(targetServiceName);
        String url = String.format("http://localhost:%s/%s", serviceConfig.port(), targetPath.replace(targetServiceName + "/", ""));
        return url + (ctx.queryString() != null ? "?" + ctx.queryString() : "");
    }

    public void forwardRequestForUrl(Context ctx, String targetUrl, @Nullable UserDTO userDTO) {
        Request.Builder requestBuilder = new Request.Builder()
                .url(targetUrl)
                .method(ctx.method().name(), !ctx.body().isEmpty() ? createRequestBody(ctx) : null);
        if (userDTO != null && userDTO.getUserId() != null) {
            requestBuilder.addHeader("X-User-Id", userDTO.getUserId());
        }
        if (userDTO != null && userDTO.getRole() != null) {
            requestBuilder.addHeader("X-User-Role", userDTO.getRole());
        }

        ctx.headerMap().forEach(requestBuilder::addHeader);
        Request request = requestBuilder.build();

        log.info("Forwarding request to: {}", request.url());
        try (Response response = client.newCall(request).execute()) {
            log.info("Response received: {}", response.code());
            ctx.status(response.code());
            response.headers().forEach(responseHeader -> {
                if (!responseHeader.getFirst().equals("Access-Control-Allow-Origin")) {
                    ctx.header(responseHeader.getFirst(), responseHeader.getSecond());
                }
            });
            ctx.result(Objects.requireNonNull(response.body()).string());
        } catch (Exception e) {
            log.error(String.format("Exception while forwarding request to: %s", targetUrl), e);
            ctx.status(500).result("Internal Server Error");
        }
    }

    private static RequestBody createRequestBody(Context ctx) {
        final String contentType = ctx.contentType();
        if (contentType == null) {
            return RequestBody.create(ctx.bodyAsBytes());
        } else {
            return RequestBody.create(ctx.body(), MediaType.parse(contentType));
        }
    }
}
