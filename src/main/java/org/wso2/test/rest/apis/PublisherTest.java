package org.wso2.test.rest.apis;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.wso2.test.apache.client.HttpClient;
import org.wso2.test.WSTestException;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class PublisherTest {

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_RED = "\u001B[31m";
    String token;
    HttpClient client;
    String apiid;
    
    String carbonport;

    public PublisherTest(HttpClient client, String port) {
        this.client=client;
        this.carbonport = port;
        try {
            token=generateToken();
        } catch (IOException e) {
            throw new WSTestException(e);
        }
        if(token==null){
            System.out.println(ANSI_RED +"####### Token generation returned null for publisher rest APIs, stopping the test. Please check the apim logs ########" +ANSI_RESET);
            System.exit(0);
        }

    }

    private String generateToken() throws IOException {
        String accessToken;
        String dcrPayload = " {\n" +
                "  \"callbackUrl\":\"www.google.lk\",\n" +
                "  \"clientName\":\"rest_api_publisher\",\n" +
                "  \"owner\":\"admin\",\n" +
                "  \"grantType\":\"client_credentials password refresh_token\",\n" +
                "  \"saasApp\":true\n" +
                "}";
        String dcrURL = "https://localhost:"+carbonport+"/client-registration/v0.17/register";
        String basicToken = Base64.getEncoder().encodeToString("admin:admin".getBytes(StandardCharsets.UTF_8));
        HttpResponse response= client.invoke("POST", dcrPayload, dcrURL,null,basicToken);
        if(response.getStatusLine().getStatusCode()==200){
            String tokenURL = "https://localhost:8244/token?grant_type=password&username=admin&password=admin&scope=apim:api_view%20apim:api_create%20apim:api_delete%20apim:api_publish%20apim:subscription_view%20apim:subscription_block%20apim:external_services_discover%20apim:threat_protection_policy_create%20apim:threat_protection_policy_manage%20apim:document_create%20apim:document_manage%20apim:mediation_policy_view%20apim:mediation_policy_create%20apim:mediation_policy_manage%20apim:client_certificates_view%20apim:client_certificates_add%20apim:client_certificates_update%20apim:ep_certificates_view%20apim:ep_certificates_add%20apim:ep_certificates_update%20apim:publisher_settings%20apim:pub_alert_manage%20apim:shared_scope_manage%20apim:app_import_export%20apim:api_import_export%20apim:api_product_import_export";
            JsonObject responsejson = new JsonParser().parse(EntityUtils.toString(response.getEntity())).getAsJsonObject();
            String basict = responsejson.get("clientId").getAsString()+":"+responsejson.get("clientSecret").getAsString();
            HttpResponse tokenresponse= client.invoke("POST", null, tokenURL,null,Base64.getEncoder().encodeToString(basict.getBytes(StandardCharsets.UTF_8)));
            if(tokenresponse.getStatusLine().getStatusCode()==200){
                JsonObject accessTokenresponsejson = new JsonParser().parse(EntityUtils.toString(tokenresponse.getEntity())).getAsJsonObject();
                accessToken = accessTokenresponsejson.get("access_token").getAsString();
            }else {
                accessToken=null;
            }
        }else {
            accessToken=null;
        }
        return accessToken;
    }

    public boolean createAPI(String path){
        boolean created = false;
        String payload=null;
        try {
            payload = FileUtils.readFileToString(new File(path));
        } catch (IOException e) {
            throw new WSTestException(e);
        }
        String url = "https://localhost:"+carbonport+"/api/am/publisher/v1/apis?openAPIVersion=v3";
        HttpResponse createApiResponse= client.invoke("POST", payload, url,token,null);
        if(createApiResponse.getStatusLine().getStatusCode()==201) {
            created = true;
            System.out.println(ANSI_GREEN +"#### Test : WS API creating  passed " +ANSI_RESET);
            JsonObject responsejson = null;
            try {
                responsejson = new JsonParser().parse(EntityUtils.toString(createApiResponse.getEntity())).getAsJsonObject();
            } catch (IOException e) {
                throw new WSTestException(e);
            }
            apiid = responsejson.get("id").getAsString();
        }else {
            System.out.println(ANSI_RED +"#### Test : WS API creating  failed " +ANSI_RESET);
            System.out.println(ANSI_RED +"#### check the APIM logs, aborting the test" +ANSI_RESET);
            System.out.println(ANSI_RED +"#### make sure to delete  the partically created API before running test again" +ANSI_RESET);
            System.exit(0);
        }
        return created;
    }

    public boolean publishAPI(boolean internalcall, String test){

        boolean published = false;
        String url = "https://localhost:"+carbonport+"/api/am/publisher/v1/apis/change-lifecycle?action=Publish&apiId="+apiid;
        HttpResponse publshApiResponse= client.invoke("POST", null, url,token,null);
        if(publshApiResponse.getStatusLine().getStatusCode()==200) {
            published = true;
            if(!internalcall) {
                System.out.println(ANSI_GREEN +"#### Test 02: WS API publishing passed " +ANSI_RESET);
            }
        }else {
            if(!internalcall){
                System.out.println(ANSI_RED +"#### Test 02: WS API publishing  failed " +ANSI_RESET);
                System.out.println(ANSI_RED +"#### check the APIM logs, aborting the test" +ANSI_RESET);
                System.out.println(ANSI_RED +"#### make sure to delete the created API before running test again" +ANSI_RESET);
            }else {
                System.out.println(ANSI_RED +"#### "+test+": WS API publishing failed " +ANSI_RESET);
            }
            System.exit(0);
        }
        return published;
    }

    public boolean addApiLevelThrottling(){
        boolean addapilevelthrotlling = false;
        String url = "https://localhost:"+carbonport+"/api/am/publisher/v1/apis/"+apiid;
        JsonObject jsonpld = getAPI();
        jsonpld.addProperty("apiThrottlingPolicy","5PerMin");
        String payload = jsonpld.toString();
        HttpResponse addThrottlingResponse= client.invoke("PUT", payload, url,token,null);
        if(addThrottlingResponse.getStatusLine().getStatusCode()==200) {
            addapilevelthrotlling = publishAPI(true,"Test 04");
            System.out.println(ANSI_GREEN +"#### Test 04: WS API level throttle policy adding passed " +ANSI_RESET);
        }else {
            System.out.println(ANSI_RED +"#### Test 04: WS API level throttle policy adding failed, while updating the API  " +ANSI_RESET);
            System.out.println(ANSI_RED +"#### check the APIM logs, aborting the test" +ANSI_RESET);
            System.out.println(ANSI_RED +"#### make sure to delete the created API before running test again" +ANSI_RESET);
            System.exit(0);
        }
        return addapilevelthrotlling;
    }

    public boolean addCorsChanges(String cors){
        boolean addcorschanges = false;
        String url = "https://localhost:"+carbonport+"/api/am/publisher/v1/apis/"+apiid;
        JsonObject jsonpld = getAPI();
        JsonObject corsjosn = new JsonParser().parse(cors).getAsJsonObject();
        jsonpld.add("corsConfiguration",corsjosn);
        String payload = jsonpld.toString();
        HttpResponse addThrottlingResponse= client.invoke("PUT", payload, url,token,null);
        if(addThrottlingResponse.getStatusLine().getStatusCode()==200) {
            addcorschanges = publishAPI(true,"Test");
            System.out.println(ANSI_GREEN +"#### Test : API level  CORS configs changed " +ANSI_RESET);
        }else {
            System.out.println(ANSI_RED +"#### Test : CORS config change failed" +ANSI_RESET);
            System.out.println(ANSI_RED +"#### check the APIM logs, aborting the test" +ANSI_RESET);
            System.out.println(ANSI_RED +"#### make sure to delete the created API amd related artefacts before running test again" +ANSI_RESET);
            System.exit(0);
        }
        return addcorschanges;
    }

    public boolean deleteCorsChange(){
        boolean deletecorschange = false;
        String url = "https://localhost:"+carbonport+"/api/am/publisher/v1/apis/"+apiid;
        JsonObject jsonpld = getAPI();
        jsonpld.remove("corsConfiguration");
        String payload = jsonpld.toString();
        HttpResponse addThrottlingResponse= client.invoke("PUT", payload, url,token,null);
        if(addThrottlingResponse.getStatusLine().getStatusCode()==200) {
            deletecorschange = publishAPI(true,"Test");
            System.out.println(ANSI_GREEN +"#### Test : CORS changes deleted " +ANSI_RESET);
        }else {
            System.out.println(ANSI_RED +"#### Test : WS API level CORS changes deleting failed" +ANSI_RESET);
            System.out.println(ANSI_RED +"#### check the APIM logs, aborting the test" +ANSI_RESET);
            System.out.println(ANSI_RED +"#### make sure to delete the created API before running test again" +ANSI_RESET);
            System.exit(0);
        }
        return deletecorschange;
    }

    public boolean deleteApiLevelThrottling(){
        boolean deleteapilevelthrotlling = false;
        String url = "https://localhost:"+carbonport+"/api/am/publisher/v1/apis/"+apiid;
        JsonObject jsonpld = getAPI();
        jsonpld.remove("apiThrottlingPolicy");
        String payload = jsonpld.toString();
        HttpResponse addThrottlingResponse= client.invoke("PUT", payload, url,token,null);
        if(addThrottlingResponse.getStatusLine().getStatusCode()==200) {
            deleteapilevelthrotlling = publishAPI(true,"Test 04");
            System.out.println(ANSI_GREEN +"#### Test 05: WS API level throttle policy deleting passed " +ANSI_RESET);
        }else {
            System.out.println(ANSI_RED +"#### Test 05: WS API level throttle policy deleting failed, while updating the API" +ANSI_RESET);
            System.out.println(ANSI_RED +"#### check the APIM logs, aborting the test" +ANSI_RESET);
            System.out.println(ANSI_RED +"#### make sure to delete the created API before running test again" +ANSI_RESET);
            System.exit(0);
        }
        return deleteapilevelthrotlling;
    }
    public JsonObject getAPI(){

        JsonObject apiobj = null;
        String url = "https://localhost:"+carbonport+"/api/am/publisher/v1/apis/"+apiid;
        HttpResponse getApiResponse= client.invoke("GET", null, url,token,null);
        if(getApiResponse.getStatusLine().getStatusCode()==200) {
            try {
                apiobj = new JsonParser().parse(EntityUtils.toString(getApiResponse.getEntity())).getAsJsonObject();
            } catch (IOException e) {
                throw new WSTestException(e);
            }
        }else {
            System.out.println(ANSI_RED +"#### Test 04: WS API level throttle policy adding failed, while fetching the API " +ANSI_RESET);
            System.out.println(ANSI_RED +"#### check the APIM logs, aborting the test" +ANSI_RESET);
            System.out.println(ANSI_RED +"#### make sure to delete  the created API before running test again" +ANSI_RESET);
            System.exit(0);
        }
        return apiobj;
    }

    public void deleteAPI(){
        String url = "https://localhost:"+carbonport+"/api/am/publisher/v1/apis/"+apiid;
        HttpResponse deleteSubscriptionnResponse= client.invoke("DELETE", null, url,token,null);
        if(deleteSubscriptionnResponse.getStatusLine().getStatusCode()==200) {
            System.out.println(ANSI_GREEN+"#### Test : Deleting API passed "+ ANSI_RESET);
        }else {
            System.out.println(ANSI_RED +"#### Test : Deleting API via publisher rest failed "+ ANSI_RESET);
            System.out.println(ANSI_RED +"#### check the APIM logs, aborting the test"+ ANSI_RESET);
            System.exit(0);
        }
    }

    public void copyFile(String path, File dest){
        try {
                if (dest.exists()) {
                    org.apache.commons.io.FileUtils.deleteQuietly(dest);
                    System.out.println(ANSI_YELLOW  +"Sleeping for 10sec and waiting to undeploy _cors_request_handler_" +ANSI_RESET);
                    Thread.sleep(10000);
                }
                FileUtils.copyFile(new File(path), dest);
                System.out.println(ANSI_YELLOW  +"Sleeping for 10sec and waiting to deploy _cors_request_handler_" +ANSI_RESET);
                Thread.sleep(10000);

        }catch (Exception e){
            throw new WSTestException(e);
        }
    }

    public String getApiid(){
        return apiid;
    }

}
