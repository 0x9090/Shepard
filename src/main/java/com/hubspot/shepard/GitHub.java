package com.hubspot.shepard;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class GitHub {
    private String url;
    private String user;
    private String pass;
    private String endpoint;
    private CloseableHttpClient httpClient;

    GitHub(String user, String pass, String url) throws IOException {
        this.url = url;
        this.user = user;
        this.pass = pass;
        this.endpoint = "https://" + url + "/api/v3/";
    }

    GitHub(String user, String pass) throws IOException {
        this.user = user;
        this.pass = pass;
        this.endpoint = "https://" + url + "/api/v3/";
    }

    public Map<String, ArrayList<String>> getPoms(String repoName) throws IOException, NullPointerException {
        CloseableHttpResponse response;
        HttpGet httpGet;
        String authHeader, content, pomURL, pom;
        JSONObject jsonObject1, jsonObject2, jsonObject3;
        Map<String, ArrayList<String>> pomMap = new HashMap<String, ArrayList<String>>();
        ArrayList<String> metaDataArray = new ArrayList<String>();

        try {
            httpClient = HttpClients.createDefault();
            authHeader = "Basic " + new String(Base64.encodeBase64((user + ":" + pass).getBytes()));
            String getStr = this.endpoint + "search/code?q=pom.xml+in%3Apath+repo%3A" + URLEncoder.encode(repoName, "UTF-8"); // searching for pom.xml
            httpGet = new HttpGet(getStr);
            httpGet.addHeader("Authorization", authHeader);
            response = httpClient.execute(httpGet);
            content = EntityUtils.toString(response.getEntity());
            response.close();

            jsonObject1 = (JSONObject)JSONValue.parse(content); // parsing search results
            long count = (long)jsonObject1.get("total_count");
            if (count == 0) {
                System.err.println("No pom.xml files found in this project.");
                return null;
            }
            JSONArray items = (JSONArray)jsonObject1.get("items");
            for (int i = 0; i < count; i++) { // looping on found poms
                jsonObject2 = (JSONObject)items.get(i);
                pomURL = (String)jsonObject2.get("url"); // pom URL
                httpGet = new HttpGet(pomURL);
                httpGet.addHeader("Authorization", authHeader);
                response = httpClient.execute(httpGet); // get contents of pom url
                content = EntityUtils.toString(response.getEntity());
                jsonObject3 = (JSONObject)JSONValue.parse(content); // parse encoded pom.xml data from json
                pom = (String)jsonObject3.get("content");
                pom = new String(Base64.decodeBase64(pom.getBytes())); // store pom.xml contents as string

                metaDataArray.add(pom);

                pomMap.put(pomURL, metaDataArray);
                response.close();
            }
        } catch (Exception e) {
            System.err.println(e);
            return null;
        }
        return pomMap;
    }

    public String[] getOrganizations(String user) {
        return new String[0];
    }

    public Map<String, String> getRepos() throws IOException {
        CloseableHttpResponse response;
        HttpGet httpGet;
        String authHeader;
        String content;
        Object object;
        JSONArray jsonArray;
        Map<String, String> repos = new HashMap<>();

        try {
            httpClient = HttpClients.createDefault();
            authHeader = "Basic " + new String(Base64.encodeBase64((user + ":" + pass).getBytes()));
            httpGet = new HttpGet(this.endpoint + "user/repos");
            httpGet.addHeader("Authorization", authHeader);
            response = httpClient.execute(httpGet);
            content = EntityUtils.toString(response.getEntity());
            object = JSONValue.parse(content);
            jsonArray = (JSONArray)object;

            for (Object aJsonArray : jsonArray) {
                JSONObject jsonObject = (JSONObject) aJsonArray;
                repos.put(jsonObject.get("full_name").toString(), jsonObject.get("clone_url").toString());
            }

            response.close();
            return repos;

        } catch(Exception e) {
            System.err.println(e);
            return null;
        }
    }
}
