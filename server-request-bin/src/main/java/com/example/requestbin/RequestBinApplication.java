package com.example.requestbin;

import org.apache.commons.io.IOUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@SpringBootApplication
public class RequestBinApplication {

    public static void main(String[] args) {
        SpringApplication.run(RequestBinApplication.class, args);
    }

    class RequestBin {

        private final Map<String, String> headers;
        private final String content;
        private final String url;
        private final String origin;
        private final Map<String, String> params;

        public RequestBin(Map<String, String> headers, String content, String url, String origin, Map<String, String> params) {
            this.headers = headers;
            this.content = content;
            this.url = url;
            this.origin = origin;
            this.params = params;
        }

        public Map<String, String> getHeaders() {
            return headers;
        }

        public String getContent() {
            return content;
        }

        public String getUrl() {
            return url;
        }

        public String getOrigin() {
            return origin;
        }

        public Map<String, String> getParams() {
            return params;
        }

        @Override
        public String toString() {
            return "RequestBin{" + '\n' +
                    "\t headers=" + headers + '\n' +
                    "\t content=" + (content.length() == 0 ? "<empty>" : content ) + '\n' +
                    "\t url='" + url + '\n' +
                    "\t origin='" + origin + '\n' +
                    "\t params=" + params + '\n' +
                    '}';
        }
    }

    @RestController
    class RequestBinController {

        @GetMapping(value = "/**", produces = APPLICATION_JSON_VALUE)
        @ResponseBody
        public ResponseEntity<RequestBin> get(HttpServletRequest request, @RequestHeader Map<String, String> headers,
                              @RequestParam Map<String,String> allRequestParams) throws IOException {
            HttpHeaders responseHeaders = new HttpHeaders();
            boolean withTimeout = Boolean.parseBoolean(System.getenv().getOrDefault("WITH_TIMEOUT", "false"));

            if (withTimeout) {
                responseHeaders.set("Connection",
                        "Keep-Alive");
                responseHeaders.set("Keep-Alive",
                        "timeout=5, max=1000");
            }
            return ResponseEntity.ok()
                    .headers(responseHeaders)
                    .body(getRequestBin(request, headers, allRequestParams));
        }

        @PostMapping(value = "/**", consumes = APPLICATION_JSON_VALUE)
        @ResponseBody
        public ResponseEntity<RequestBin> post(HttpServletRequest request, @RequestHeader Map<String, String> headers,
                               @RequestParam Map<String,String> allRequestParams) throws IOException {
            HttpHeaders responseHeaders = new HttpHeaders();
            boolean withTimeout = Boolean.parseBoolean(System.getenv().getOrDefault("WITH_TIMEOUT", "false"));

            if (withTimeout) {
                responseHeaders.set("Connection",
                        "Keep-Alive");
                responseHeaders.set("Keep-Alive",
                        "timeout=5, max=1000");
            }
            return ResponseEntity.ok()
                    .headers(responseHeaders)
                    .body(getRequestBin(request, headers, allRequestParams));
        }

        private RequestBin getRequestBin(HttpServletRequest request, Map<String, String> headers, Map<String, String> allRequestParams) throws IOException {
            String fullURL = request.getRequestURL().toString();
            final String json = IOUtils.toString(request.getInputStream(), StandardCharsets.UTF_8).replaceAll("\\s+", "");
            String origin = HttpUtils.getRequestIP(request);
            var requestBin = new RequestBin(headers, json, fullURL, origin, allRequestParams);
            System.out.println(requestBin);

            return requestBin;
        }
    }
}