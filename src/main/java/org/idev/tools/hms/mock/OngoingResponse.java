package org.idev.tools.hms.mock;

import fi.iki.elonen.Status;
import org.idev.tools.hms.MockRule;

import java.util.HashMap;
import java.util.Map;

/**
 * Class that maps the following properties:
 * "body": {                               -- required
 * "status" : 200,                           -- required
 * "mimeType" : "text/html",                 -- optional, default "text/html"
 * "headers" : {                             -- optional
 * "Content-Length" : "100"
 * },
 * "body" : "Here goes the body body"    -- optional
 * }
 */
public class OngoingResponse {
    protected OngoingRequest ongoingRequest;
    protected Status status;
    protected String body;
    protected Map<String, String> headers = new HashMap<String, String>();
    protected String mimeType = "text/html";

    private Integer mockRuleId;

    public OngoingResponse(OngoingRequest ongoingRequest,Status status) {
        this(ongoingRequest, status, null);
    }

    public OngoingResponse(OngoingRequest ongoingRequest, Status status, String body) {
        this.status = status;
        this.ongoingRequest = ongoingRequest;

        this.body = body;

        MockRule rule = createMockRule();
        mockRuleId= ongoingRequest.mockServer.addRule(rule);
    }


    public OngoingResponse withHeader(String headerName, String headerValue) {
        headers.put(headerName, headerValue);

        MockRule rule = createMockRule();
        ongoingRequest.mockServer.replaceRuleAtIndex(rule,mockRuleId);
        return this;
    }

    //TODO this can not be a header ?
    public OngoingResponse withMimeTypey(String mimeType) {
        this.mimeType = mimeType;

        MockRule rule = createMockRule();
        ongoingRequest.mockServer.replaceRuleAtIndex(rule,mockRuleId);
        return this;
    }

    private MockRule createMockRule() {
        MockRule rule = new MockRule();
        rule.setRequestUri(ongoingRequest.uri);
        rule.setRequestMethod(ongoingRequest.method);
        rule.setRequestBody(ongoingRequest.body);
        rule.setRequestHeaders(ongoingRequest.headers);
        rule.setRequestLimit(ongoingRequest.limit);

        rule.setResponseStatus(status);
        rule.setResponseBody(body);
        rule.setResponseMimeType(mimeType);
        rule.setRequestHeaders(headers);

        return rule;
    }
}

