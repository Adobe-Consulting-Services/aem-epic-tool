/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.adobe.acs.epic.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.TreeMap;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class JcrNodeContentHandler extends DefaultHandler {

    public static enum CONTEXT {
        SUBASSET("dam:Asset", "/content/dam/.*?/subassets/.*"),
        ASSET("dam:Asset", "/content/dam/.*"),
        DAM(null, "/content/dam"),
        PAGE_CONTENT("cq:PageContent", null),
        USERS(null, "/home/users"),
        GROUPS(null, "/home/groups"),
        TAGS(null, "/etc/tags"),
        IGNORE("NO_MATCH", "//NO_MATCH//");
        String jcrType;
        String path;

        CONTEXT(String type, String path) {
            this.jcrType = type;
            this.path = path;
        }

        public static CONTEXT fromTypeAndPath(String t, String p) {
            for (CONTEXT c : CONTEXT.values()) {
                if (c.path != null && !p.matches(c.path)) {
                    continue;
                }
                if (c.jcrType == null || (t != null && t.equalsIgnoreCase(c.jcrType))) {
                    return c;
                }
            }
            return null;
        }
    }

    Stack<String> pathStack = new Stack<>();
    Stack<CONTEXT> contextStack = new Stack<>();
    Map<String, List<String>> typesFound = new TreeMap<>();
    String location;

    public void setLocation(String loc) {
        location = loc;
    }

    public Map<String, List<String>> getTypesFound() {
        return typesFound;
    }

    @Override
    public void startDocument() throws SAXException {
        typesFound.clear();
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
        String basePath = pathStack.isEmpty() ? "" : pathStack.peek();
        if (basePath.isEmpty()) {
            pathStack.push("/");
        } else {
            pathStack.push(basePath + "/" + localName);
        }
        String primaryType = atts.getValue("jcr:primaryType");
        String resourceType = atts.getValue("sling:resourceType");
        if (primaryType == null) {
            primaryType = "nt:unstructured";
        }
        CONTEXT currentContext = contextStack.isEmpty() ? null : contextStack.peek();
        CONTEXT context = CONTEXT.fromTypeAndPath(primaryType, location + pathStack.peek());
        if (context != null) {
            contextStack.add(context);
        } else if (contextStack.size() > 0) {
            contextStack.push(contextStack.peek());
        }
        if (primaryType.equals("nt:unstructured") && currentContext != null) {
            switch (currentContext) {
                case PAGE_CONTENT:
                    primaryType = "Page Component";
                    break;
                case IGNORE:
                    return;
            }
        }
        String type = resourceType != null ? primaryType + ";" + resourceType : primaryType;
        if (!typesFound.containsKey(type)) {
            typesFound.put(type, new ArrayList<>());
        }
        typesFound.get(type).add(pathStack.peek());
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (!contextStack.isEmpty()) {
            contextStack.pop();
        }
        
        if (!pathStack.isEmpty()) {
            pathStack.pop();
        }
    }
}
