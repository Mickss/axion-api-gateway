package org.axion.axion_api_gateway;

import io.javalin.Javalin;
import okhttp3.*;

import java.io.IOException;
import java.util.Objects;

public class AxionApiGatewayApplication {
	private static final OkHttpClient client = new OkHttpClient();

	public static void main(String[] args) {
		Javalin app = Javalin.create().start(7000);

		app.get("/api/service1/*", ctx -> forwardRequest(ctx, "http://localhost:8001"));
		app.post("/api/service1/*", ctx -> forwardRequest(ctx, "http://localhost:8001"));
		app.put("/api/service1/*", ctx -> forwardRequest(ctx, "http://localhost:8001"));
		app.delete("/api/service1/*", ctx -> forwardRequest(ctx, "http://localhost:8001"));
		// Add more routes as needed

		app.get("/api/service2/*", ctx -> forwardRequest(ctx, "http://localhost:8002"));
		app.post("/api/service2/*", ctx -> forwardRequest(ctx, "http://localhost:8002"));
		app.put("/api/service2/*", ctx -> forwardRequest(ctx, "http://localhost:8002"));
		app.delete("/api/service2/*", ctx -> forwardRequest(ctx, "http://localhost:8002"));
		// Add more routes as needed
	}

	private static void forwardRequest(io.javalin.http.Context ctx, String targetBaseUrl) {
		String targetUrl = targetBaseUrl + ctx.path().replace("/api/service1", "");

		Request.Builder requestBuilder = new Request.Builder().url(targetUrl).method(ctx.method().name(), createRequestBody(ctx));
		ctx.headerMap().forEach(requestBuilder::addHeader);

		Request request = requestBuilder.build();

		client.newCall(request).enqueue(new Callback() {
			@Override
			public void onFailure(Call call, IOException e) {
				ctx.status(500).result("Internal Server Error: " + e.getMessage());
			}

			@Override
			public void onResponse(Call call, Response response) throws IOException {
				ctx.status(response.code());
				response.headers().forEach(pair -> ctx.header(pair.getFirst(), pair.getSecond()));
				ctx.result(Objects.requireNonNull(response.body()).string());
			}
		});
	}

	private static RequestBody createRequestBody(io.javalin.http.Context ctx) {
		if ("GET".equals(ctx.method()) || "DELETE".equals(ctx.method())) {
			return null;
		}
		return RequestBody.create(ctx.bodyAsBytes(), MediaType.parse(ctx.contentType()));
	}
}
