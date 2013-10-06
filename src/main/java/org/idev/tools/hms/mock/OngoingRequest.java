package org.idev.tools.hms.mock;

import fi.iki.elonen.Method;
import fi.iki.elonen.Status;

import java.util.HashMap;
import java.util.Map;

/**
 * Class that maps the following properties of a request
 * {
 * "request": {                                -- required
 * "limit" : 2,                              -- optional, default infinite
 * "method" : "GET",                         -- optional
 * "uri" : "/server/1",                      -- required
 * "headers" : {                             -- optional
 * "Accept" : "text/plain"
 * },
 * "body" : "text to match"                  -- optional
 * },
 */

public class OngoingRequest {
    protected String uri = null;
    protected Method method = null;
    protected String body = null;
    protected Map<String, String> headers = new HashMap<String, String>();
    protected Integer limit = null;

    protected MockServer mockServer;

    /**
     * Default constructor. Path and method are mandatory paramenters
     *
     * @param uri
     * @param method
     * @param mockServer
     */
    public OngoingRequest(String uri, Method method, MockServer mockServer) {
        this.method = method;
        this.uri = uri;
        this.mockServer = mockServer;
    }

    public OngoingResponse thenReturn(String body) {
        return new OngoingResponse(this, Status.OK, body);
    }
    public OngoingResponse thenReturn(Status status) {
        //status 200 by default
        return new OngoingResponse(this, status);
    }

    public OngoingRequest withHeader(String headerName, String headerValue) {
        headers.put(headerName, headerValue);
        return this;
    }

    public OngoingRequest withBody(String body) {
        this.body = body;
        return this;
    }

    public OngoingRequest withLimit(Integer limit) {
        this.limit = limit;
        return this;
    }


}
