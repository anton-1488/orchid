package com.subgraph.orchid.downloader.request;

import com.subgraph.orchid.Globals;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.CookieManager;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

public final class TorHttpClient {
    private static final Logger log = LoggerFactory.getLogger(TorHttpClient.class);
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .cookieHandler(new CookieManager())
            .executor(Globals.VIRTUAL_EXECUTOR)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .version(HttpClient.Version.HTTP_2)
            .build();

    private TorHttpClient() {
        Globals.addShutdownHook(HTTP_CLIENT::close);
    }

    /**
     * Makes a GET request.
     *
     * @param torRequest get request
     * @return ByteBuffer response body
     */
    public static ByteBuffer sendGetRequest(TorRequest torRequest) {
        try {
            HttpRequest request = cofigureRequest(torRequest);
            HttpResponse<byte[]> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (isSuccessfulResponse(response)) {
                return ByteBuffer.wrap(response.body());
            }
        } catch (Exception e) {
            log.error("Error to execute get request: ", e);
        }
        return ByteBuffer.allocate(0);
    }

    /**
     * Makes a POST request.
     *
     * @param torRequest POST request
     */
    public static void sendPostRequest(TorRequest torRequest) {
        try {
            HttpRequest request = cofigureRequest(torRequest);
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (isSuccessfulResponse(response)) {
                log.debug("Post result: {}", response.body());
            } else {
                log.warn("Post request is failed with status code: {}", response.body());
            }
        } catch (Exception e) {
            log.error("Error to execute post request: ", e);
        }
    }

    /**
     * Utilitary method to decompress bytes, which were compressed .z algorithm.
     *
     * @param original compressed bytes,
     * @return decompressed bytes.
     * @throws IOException if error occurent while decompressing.
     */
    public static @NotNull ByteBuffer decompressBuffer(ByteBuffer original) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Inflater decompressor = new Inflater();
        byte[] decompressBuffer = new byte[4096];
        decompressor.setInput(original);
        int n;
        try {
            while ((n = decompressor.inflate(decompressBuffer)) != 0) {
                output.write(decompressBuffer, 0, n);
            }
            return ByteBuffer.wrap(output.toByteArray());
        } catch (DataFormatException e) {
            throw new IOException("Error decompressing http body: " + e);
        }
    }

    private static boolean isSuccessfulResponse(@NotNull HttpResponse<?> httpResponse) {
        int code = httpResponse.statusCode();
        return code >= 200 && code < 300;
    }

    private static HttpRequest cofigureRequest(@NotNull TorRequest request) {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder().uri(request.path()).timeout(request.timeout());

        Map<String, String> headers = request.headers();
        for (String header : headers.keySet()) {
            requestBuilder.header(header, headers.get(header));
        }

        if (request.method().equals("POST")) {
            requestBuilder.POST(HttpRequest.BodyPublishers.ofString(request.body()));
        } else if (request.method().equals("HEAD")) {
            requestBuilder.HEAD();
        } else {
            requestBuilder.GET();
        }

        return requestBuilder.build();
    }
}