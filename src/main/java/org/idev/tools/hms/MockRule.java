package org.idev.tools.hms;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonObject.Member;
import com.eclipsesource.json.JsonValue;

import fi.iki.elonen.Method;
import fi.iki.elonen.Response;
import fi.iki.elonen.Status;

/**
 * Mock rule
 *
 */
public class MockRule {
  private Integer requestLimit;
  private Method requestMethod;
  private String requestUri;
  private Map<String, String> requestHeaders;
  private String requestBody;

  private Status responseStatus;
  private String responseMimeType;
  private Map<String, String> responseHeaders;
  private String responseBody;

  private int called;

  /**
   * Constructor
   *
   */
  public MockRule() {
    this.called= 0;
  }
  /**
  /**
   * Create {@link MockRule} from the specified input stream
   *
   * Expected format:
   *  {
   *    "request": {                                -- required
   *      "limit" : 2,                              -- optional, default infinite
   *      "method" : "GET",                         -- optional
   *      "uri" : "/server/1",                      -- required
   *      "headers" : {                             -- optional
   *        "Accept" : "text/plain"
   *      },
   *      "body" : "text to match"                  -- optional
   *    },
   *    "response": {                               -- required
   *      "status" : 200,                           -- required
   *      "mimeType" : "text/html",                 -- optional, default "text/html"
   *      "headers" : {                             -- optional
   *        "Content-Length" : "100"
   *      },
   *      "body" : "Here goes the response body"    -- optional
   *    }
   *  }
   */
  public static MockRule readFrom(String txt) throws IOException, IllegalArgumentException {
    return MockRule.readFrom(JsonObject.readFrom(txt));
  }

  /**
   * Reads from JsonObject
   *
   */
  public static MockRule readFrom(JsonObject json) throws IllegalArgumentException {
    MockRule rule= new MockRule();

    // Check required properties
    JsonValue request= json.get("request");
    if (null == request) {
      throw new IllegalArgumentException("json.request is required");
    }

    JsonValue response= json.get("response");
    if (null == response) {
      throw new IllegalArgumentException("json.response is required");
    }

    // Read request
    JsonObject requestJson= request.asObject();
    for (Member member : requestJson) {
      String name= member.getName();

      if (name.equals("limit")) {
        rule.setRequestLimit(new Integer(member.getValue().asInt()));
        continue;
      }

      if (name.equals("method")) {
        rule.setRequestMethod(member.getValue().asString().trim());
        continue;
      }

      if (name.equals("uri")) {
        rule.setRequestUri(member.getValue().asString().trim());
        continue;
      }

      if (name.equals("headers")) {
        for (Member header: member.getValue().asObject()) {
          rule.addRequestHeader(header.getName(), header.getValue().asString().trim());
        }
        continue;
      }

      if (name.equals("body")) {
        rule.setRequestBody(member.getValue().asString().trim());
        continue;
      }
    }

    // Read response
    JsonObject responseJson= response.asObject();
    for (Member member : responseJson) {
      String name= member.getName();

      if (name.equals("status")) {
        rule.setResponseStatus(member.getValue().asInt());
        continue;
      }

      if (name.equals("mimeType")) {
        rule.setResponseMimeType(member.getValue().asString().trim());
        continue;
      }

      if (name.equals("headers")) {
        for (Member header: member.getValue().asObject()) {
          rule.addResponseHeader(header.getName(), header.getValue().asString().trim());
        }
        continue;
      }

      if (name.equals("body")) {
        rule.setResponseBody(member.getValue().asString().trim());
        continue;
      }
    }

    // Check required members are set
    if (null == rule.getRequestUri() || 0 == rule.getRequestUri().length()) {
      throw new IllegalArgumentException("json.request.uri is required");
    }

    if (null == rule.getResponseStatus()) {
      throw new IllegalArgumentException("json.response.status is required");
    }

    return rule;
  }

  /**
   * Check id this Rule is a match for the specified request
   *
   */
  public boolean matches(Method method, String uri, Map<String, String> headers, String body) {

    // Compare method
    if (null != method && null != this.requestMethod && !this.requestMethod.equals(method)) {
      return false;
    }

    // Compare URI
    if (null != uri && null != this.requestUri) {

      // Regular expression match
      if (
          7 > this.requestUri.length() &&
          this.requestUri.substring(0, 7).equals("regexp:") &&
          !uri.matches(this.requestUri.substring(7))
      ) {
        return false;
      }

      // Exact match
      if (!uri.equals(this.requestUri)) {
        return false;
      }
    }

    // Compare headers
    if (null != headers && null != requestHeaders && !this.mapContainsMap( headers,requestHeaders)) {

      return false;
    }

    // Compare bodies
    if (null != body && null != this.requestBody) {

      // Regular expression match
      if (
          7 > this.requestBody.length() &&
          this.requestBody.substring(0, 7).equals("regexp:") &&
          !body.matches(this.requestBody.substring(7))
      ) {
        return false;
      }

      // Exact match
      if (!body.equals(this.requestBody)) {
        return false;
      }
    }

    // This rule matches the specified request
    return true;
  }

  /**
   * Check this rule limit is null or zero
   *
   */
  public void checkLimit() throws IllegalStateException {

    // No no. No no no no. No no no no. No no there's no limit
    if (null == this.requestLimit) return;

    // Limit set but not consumed; too few requests
    if (this.called < this.requestLimit) {
      throw new IllegalStateException(
        "Too few requests to \"" + this.requestUri + "\". Expected "
        + this.requestLimit + " but received " + this.called
      );
    }

    // Limit set and exceeded; too many requests
    if (this.called < this.requestLimit) {
      throw new IllegalStateException(
        "Too many requests to \"" + this.requestUri + "\". Expected "
        + this.requestLimit + " but received " + this.called
      );
    }
  }

  /**
   * Build the response for this mock rule
   *
   * Note: if count is not null and it is 0, throw IllegalStateException
   */
  public Response getResponse() throws IllegalStateException {
    Response response;

    // Increment called count
    this.called++;

    // Check limit
    if (null != this.requestLimit && this.called > this.requestLimit) {
      throw new IllegalStateException(
        "Request limit for \"" + this.requestUri + "\" already exceeded"
      );
    }

    // Initialize response
    response= new Response(this.responseStatus, this.responseMimeType, this.responseBody);

    // Add response headers
    if (null != this.responseHeaders) {
      for (Map.Entry<String, String> entry: this.responseHeaders.entrySet()) {
        response.addHeader(entry.getKey(), entry.getValue());
      }
    }

    return response;
  }

  /**
   * Check if all keys and values in map1 are found in map2
   *
   * Note: map2 may also contain other keys
   */
  private boolean mapsMatch(Map<String, String> map1, Map<String, String> map2) {

    // Iterate through keys
    for (Map.Entry<String, String> entry: map1.entrySet()) {
      String key= entry.getKey();
      if (!entry.getValue().equals(map2.get(key))) return false;
    }

    // Perfect match
    return true;
  }
    private boolean mapContainsMap(Map<String, String> map1, Map<String, String> map2){
        if(map2.size() == 0){
            return  true;
        }
        Set<String> map2Keys = map2.keySet();
        for (String key : map2Keys){
            String map2Value = map2.get(key);
            if (!(map1.containsKey(key) && map1.get(key).equals(map2Value))){
                return false;
            }
        }
        return true;

    }

  /**
   * Getter for responseStatus
   *
   */
  public Status getResponseStatus() {
    return this.responseStatus;
  }

  /**
   * Getter for requestUri
   *
   */
  public String getRequestUri() {
    return this.requestUri;
  }

  /**
   * Setter for responseBody
   *
   */
  public void setResponseBody(String body) {
    if (null == body || 0 == body.length()) return;
    this.responseBody= body;
  }

  /**
   * Setter for responseMimeType
   *
   */
  public void setResponseMimeType(String mimeType) {
    if (null == mimeType || 0 == mimeType.length()) return;
    this.responseMimeType= mimeType;
  }

  /**
   * Setter for responseHeaders
   *
   */
  private void addResponseHeader(String name, String value) {
    if (null == name || 0 == name.length() || null == value || 0 == value.length()) return;

    if (null == this.responseHeaders) {
      this.responseHeaders= new HashMap<String, String>();
    }
    this.responseHeaders.put(name, value);
  }

  /**
   * Setter for responseStatus
   *
   */
  public void setResponseStatus(int status) {

    // Lookup value
    for (Status st: Status.values()) {
      if (status == st.getRequestStatus()) {
        this.responseStatus= st;
      }
    }

    // Invalid value
    if (null == this.responseStatus) {
      throw new IllegalArgumentException("json.response.status has an invalid value \"" + status + "\"");
    }
  }

  /**
   * Setter for requestBody
   *
   */
  public void setRequestBody(String body) {
    if (null == body || 0 == body.length()) return;
    this.requestBody= body;
  }

  /**
   * Setter for requestHeaders
   *
   */
  private void addRequestHeader(String name, String value) {
    if (null == name || 0 == name.length() || null == value || 0 == value.length()) return;

    if (null == this.requestHeaders) {
      this.requestHeaders= new HashMap<String, String>();
    }
    this.requestHeaders.put(name.toLowerCase(), value);
  }

  /**
   * Setter for requestUri
   *
   */
  public void setRequestUri(String uri) {
    if (null == uri || 0 == uri.length()) return;
    this.requestUri= uri;
  }

  /**
   * Setter for requestMethod
   *
   */
  public void setRequestMethod(String method) {
    if (null == method || 0 == method.length()) return;

    // Lookup value
    for (Method mth: Method.values()) {
        //TODO nu poate fi mth.equals(mth.name())
      if (method.equals(mth.name())) {
        this.requestMethod= mth;
      }
    }

    // Invalid value
    if (null == this.requestMethod) {
      throw new IllegalArgumentException("json.request.uri has an invalid value \"" + method + "\"");
    }
  }

    /**
   * Setter for requestCount
   *
   */
  public void setRequestLimit(Integer limit) {
   if (null == limit) {
      return;
     }
   if (limit <= 0) return;
   this.requestLimit= limit;
  }

    public void setRequestMethod(Method requestMethod) {
        this.requestMethod = requestMethod;
    }

    public void setRequestHeaders(Map<String, String> requestHeaders) {
        this.requestHeaders = requestHeaders;
    }

    public void setResponseHeaders(Map<String, String> responseHeaders) {
        this.responseHeaders = responseHeaders;
    }

    public void setResponseStatus(Status responseStatus) {
        this.responseStatus = responseStatus;
    }

    @Override
    public String toString() {
        return "MockRule{" +
                "requestBody='" + requestBody + '\'' +
                ", requestLimit=" + requestLimit +
                ", requestMethod=" + requestMethod +
                ", requestUri='" + requestUri + '\'' +
                ", requestHeaders=" + requestHeaders +
                ", responseStatus=" + responseStatus +
                ", responseMimeType='" + responseMimeType + '\'' +
                ", responseHeaders=" + responseHeaders +
                ", responseBody='" + responseBody + '\'' +
                ", called=" + called +
                '}';
    }
}