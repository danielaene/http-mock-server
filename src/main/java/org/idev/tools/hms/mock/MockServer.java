package org.idev.tools.hms.mock;

import fi.iki.elonen.Method;
import org.idev.tools.hms.HttpServer;
import org.idev.tools.hms.MockRule;

public class MockServer extends HttpServer implements Runnable {

    public MockServer(String hostname, int port) {
        super(hostname, port);
        this.reset();
    }

    public OngoingRequest when(String path, Method method) {
        OngoingRequest request = new OngoingRequest(path,method, this);
        return request;
    }

    protected int addRule(MockRule rule){
        rules.add(rule);
        return rules.indexOf(rule);
    }

    public void replaceRuleAtIndex(MockRule newRule, int index){
        rules.add(index, newRule);
    }

}