package org.idev.tools.hms;


import com.eclipsesource.json.JsonArray;
import fi.iki.elonen.Method;
import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.Response;
import fi.iki.elonen.Status;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HttpServer extends NanoHTTPD implements  Runnable{
    protected List<MockRule> rules;
    protected List<String> failures;
    private boolean receivedShutdownRequest = false;


    public HttpServer(String hostname, int port) {
        super(hostname, port);
    }
    /**
     * Reset rules and failures
     */
    protected void reset() {
        this.rules = new ArrayList<MockRule>();
        this.failures = new ArrayList<String>();
    }

    @Override
    public Response serve(HTTPSession session) {

        // Stop server
        if (session.getUri().equals("/__shutdown")) {
            return this.serveShutdown(session);
        }

        // Check all expectations are met
        if (session.getUri().equals("/__check")) {
            return this.serveCheck(session);
        }

        // Register a new expectation
        if (session.getUri().equals("/__expect")) {
            return this.serveExpect(session);
        }

        // Try to match a registered mock rule
        return this.serveMockRule(session);
    }

    /**
     * Shutdown the server
     * <p/>
     * Returns:
     * - 201 Accepted
     */
    private Response serveShutdown(HTTPSession session) {
        this.receivedShutdownRequest = true;
        return new Response(Status.ACCEPTED, MIME_PLAINTEXT, "Shutting down...");
    }

    /**
     * Check all expectations are met and build a Response
     * <p/>
     * Returns:
     * - 200 OK if all expectations were met
     * - 409 Conflict otherwise
     */
    private Response serveCheck(HTTPSession session) {

        // Check limit
        for (MockRule rule : this.rules) {
            try {
                rule.checkLimit();

                // Limit exhausted or not met for this mock rule
            } catch (IllegalStateException ex) {
                this.failures.add(ex.getMessage());
            }
        }

        // Success
        if (0 == this.failures.size()) {
            this.reset();
            return new Response("All expectations met");
        }

        // There are failures; return them all as a JSON array
        JsonArray lines = new JsonArray();
        for (String failure : this.failures) {
            lines.add(failure);
        }

        this.reset();
        return new Response(Status.CONFLICT, "application/json", lines.toString());
    }

    /**
     * Register a new expectation
     * <p/>
     * Returns:
     * - 200 OK if expectations was registered
     * - 400 Bad Request otherwise
     */
    private Response serveExpect(HTTPSession session) {

        if (!Method.POST.equals(session.getMethod())) {
            return new Response(Status.BAD_REQUEST, MIME_PLAINTEXT, "Error: Define a new mock rule using a POST request");
        }

        try {
            this.rules.add(MockRule.readFrom(this.readBody(session)));

            // Cannot read/parse body
        } catch (IOException ex) {
            return new Response(Status.BAD_REQUEST, MIME_PLAINTEXT, "Failed to read from body: " + ex.toString());

            // Missing required members
        } catch (IllegalArgumentException ex) {
            return new Response(Status.BAD_REQUEST, MIME_PLAINTEXT, "Invalid mock rule configuration: " + ex.getMessage());
        }

        // Success
        return new Response("Rule added");
    }

    /**
     * Try to find matching mock rule
     * <p/>
     * Returns:
     * - Status defined by the matching mock rule (if any)
     * - 400 Bad Request otherwise
     */
    private Response serveMockRule(HTTPSession session) {


        String body = null;

        // Read request body
        if (Method.PUT.equals(session.getMethod()) || Method.POST.equals(session.getMethod())) {
            try {
                body = this.readBody(session);
            } catch (IOException ex) {
            }
        }
//        System.out.println("Look after: ");
//        System.out.println("Method = " + session.getMethod());
//
//        System.out.println("URI = " + session.getUri());
//        System.out.println("headers = "+ session.getHeaders());
//        System.out.println("Body = " + body);
//        System.out.println("Look in rules = " + rules);

        // Try to find a matching mock rule
        for (MockRule rule : this.rules) {
            if (rule.matches(session.getMethod(), session.getUri(), session.getHeaders(), body)) {

                // Build response
                try {
                    return rule.getResponse();

                    // Limit exceeded for this mock rule
                } catch (IllegalStateException ex) {
                    return new Response(Status.NOT_FOUND, MIME_PLAINTEXT, ex.getMessage());
                }
            }
        }

        // No mock rule match found
        String err = session.getMethod().name() + " request to \"" + session.getUri() + "\" did not match any rule";

        this.failures.add(err);
        return new Response(Status.NOT_FOUND, MIME_PLAINTEXT, err);
    }

    /**
     * Read the contents of body
     *
     * @throws IllegalStateException when the "Content-Type" header is missing
     */
    private String readBody(HTTPSession session) throws IOException {
        int size;

        // GET and HEAD requests have no body
        if (Method.GET.equals(session.getMethod()) || Method.HEAD.equals(session.getMethod())) {
            return null;
        }

        // Along with the body, the client must also provide the "Content-Length" header
        Map<String, String> headers = session.getHeaders();
        if (!headers.containsKey("content-length")) {
            throw new IllegalStateException("The \"Content-Type\" header is missing");
        }
        size = Integer.parseInt(headers.get("content-length"));
        if (0 == size) return "";

        // Read from input stream until EOS or size is reached
        InputStream is = session.getInputStream();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buff = new byte[1024];
        int read = 0;
        while (size > 0 && -1 != (read = is.read(buff, 0, Math.min(1024, size)))) {
            size -= read;
            if (0 != read) {
                baos.write(buff, 0, read);
            }
        }

        return new String(baos.toByteArray(), "UTF-8");
    }

    @Override
    public void run() {

        // Start server
        try {
            System.out.println("Starting server ...");
            this.start();
        } catch (IOException ex) {
            System.out.println("Couldn't start server:\n" + ex);
            System.exit(-1);
        }
        System.out.println("Done");

        // Sleep forever
        while (true) {
            try {

                // Check shutdown request
                if (this.receivedShutdownRequest) {
                    Thread.sleep(1000);
                    this.stop();
                    break;
                }

                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                this.stop();
            }
        }
    }

}
