package main.java;

import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();

        boolean withTimeout = Boolean.parseBoolean(System.getenv().getOrDefault("WITH_TIMEOUT", "false"));
        String URL = System.getenv().getOrDefault("URL", "http://httpbin.org/get");

        if (withTimeout) {
            int timeout = 500; // seconds
            RequestConfig config = RequestConfig.custom()
                    .setConnectTimeout(timeout * 1000)
                    .setConnectionRequestTimeout(timeout * 1000)
                    .setSocketTimeout(timeout * 1000).build();

            Header header = new BasicHeader("Keep-Alive", "timeout=5");

            httpClientBuilder
                    .setDefaultHeaders(List.of(header))
                    .setDefaultRequestConfig(config)
                    .setConnectionReuseStrategy((httpResponse, httpContext) -> true)
                    .setKeepAliveStrategy((response, context) -> 5 * 1000);

        }

        HttpClient httpClient = httpClientBuilder
                .build();

        while (true) {
            try {
                System.out.println("** START OF REQUEST **");

                HttpGet getMethod = new HttpGet(URL);
                if (withTimeout) {
                    Header header = new BasicHeader("keep-alive", "timeout=5");
                    getMethod.setHeader(header);
                }
                HttpResponse response = null;

                response = httpClient.execute(getMethod);

                System.out.println("\t\t[HEADERS]");
                Arrays.stream(response.getAllHeaders()).forEach(e -> System.out.println(e.toString()));

                System.out.println("\t\t[BODY]");
                response.getEntity().writeTo(System.out);
                System.out.println();
                System.out.println("== END OF REQUEST ==");
                Thread.sleep(1000);
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
