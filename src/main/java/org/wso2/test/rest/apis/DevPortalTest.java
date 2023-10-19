package org.wso2.test.rest.apis;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.wso2.test.apache.client.HttpClient;
import org.wso2.test.WSTestException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class DevPortalTest {

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_RED = "\u001B[31m";
    String token;
    HttpClient client;
    String appid;
    String kmid;
    String applicationtoken;
    String consumerKey;
    String consumerSecret;
    String carbonport;

    String passthruonport;

    public DevPortalTest(HttpClient client, String port, String pp) {
        this.carbonport = port;
        this.client=client;
        this.passthruonport = pp;
        try {
            token=generateToken();
        } catch (IOException e) {
            throw new WSTestException(e);
        }
        if(token==null){
            System.out.println(ANSI_RED +"####### Token generation returned null for devportal rest APIs, stopping the test. Please check the apim logs ########" + ANSI_RESET);
            System.exit(0);
        }

    }

    private String generateToken() throws IOException {
        String accessToken;
        String dcrPayload = "  {\n" +
                "  \"callbackUrl\":\"www.google.lk\",\n" +
                "  \"clientName\":\"rest_api_devportal\",\n" +
                "  \"owner\":\"admin\",\n" +
                "  \"grantType\":\"client_credentials password refresh_token\",\n" +
                "  \"saasApp\":true\n" +
                "  }";
        String dcrURL = "https://localhost:"+carbonport+"/client-registration/v0.17/register";
        String basicToken = Base64.getEncoder().encodeToString("admin:admin".getBytes(StandardCharsets.UTF_8));
        HttpResponse response= client.invoke("POST", dcrPayload, dcrURL,null,basicToken);
        if(response.getStatusLine().getStatusCode()==200){
            String tokenURL = "https://localhost:"+passthruonport+"/token?grant_type=password&username=admin&password=admin&scope=apim:subscribe%20apim:api_key";
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

    public boolean addApplication(){
        boolean isapplicationadded = false;
        String url = "https://localhost:"+carbonport+"/api/am/store/v1/applications";
        String payload = "{\"name\":\"test5\"," +
                "\"throttlingPolicy\":\"10PerMin\"," +
                "\"description\":\"wso2 test suite\"," +
                "\"tokenType\":\"JWT\",\"groups\":null," +
                "\"attributes\":{}}";

        HttpResponse addApplicationResponse= client.invoke("POST", payload, url,token,null);
        if(addApplicationResponse.getStatusLine().getStatusCode()==201) {
            isapplicationadded = true;
            System.out.println(ANSI_GREEN+"#### Test 06: Creating application via devportal rest passed "+ ANSI_RESET);
            JsonObject responsejson = null;
            try {
                responsejson = new JsonParser().parse(EntityUtils.toString(addApplicationResponse.getEntity())).getAsJsonObject();
            } catch (IOException e) {
                throw new WSTestException(e);
            }
            appid = responsejson.get("applicationId").getAsString();
        }else {
            System.out.println(ANSI_RED +"#### Test 06: Creating application via devportal rest failed "+ ANSI_RESET);
            System.out.println(ANSI_RED +"#### check the APIM logs, aborting the test"+ ANSI_RESET);
            System.exit(0);
        }
        return isapplicationadded;
    }

    public boolean subscribeApi(String apiid, boolean issecond){
        boolean issubscribed = false;
        String payload;
        String url = "https://localhost:"+carbonport+"/api/am/store/v1/subscriptions";
        if(!issecond) {
            payload = "{\"apiId\":\"" + apiid + "\"," +
                    "\"applicationId\":\"" + appid + "\"," +
                    "\"throttlingPolicy\":\"Bronze\"}";
        }else {
            payload = "{\"apiId\":\"" + apiid + "\"," +
                    "\"applicationId\":\"" + appid + "\"," +
                    "\"throttlingPolicy\":\"Unlimited\"}";
        }
        HttpResponse addApplicationResponse= client.invoke("POST", payload, url,token,null);
        if(addApplicationResponse.getStatusLine().getStatusCode()==201) {
            issubscribed = true;
            System.out.println(ANSI_GREEN+"#### Test : Subscribing WS API via devportal rest passed "+ ANSI_RESET);
        }else {
            System.out.println(ANSI_RED +"#### Test : Subscribing WS API via devportal rest failed "+ ANSI_RESET);
            System.out.println(ANSI_RED +"#### check the APIM logs, aborting the test"+ ANSI_RESET);
            System.exit(0);
        }
        return issubscribed;
    }

    public boolean geneateKeys(){
        this.getKeyManager();
        boolean iskeygenerated = false;
        String url = "https://localhost:"+carbonport+"/api/am/store/v1/applications/"+appid+"/generate-keys";
        String payload = "{\"keyType\":\"PRODUCTION\"," +
                "\"grantTypesToBeSupported\":[\"refresh_token\"," +
                "\"urn:ietf:params:oauth:grant-type:saml2-bearer\"," +
                "\"password\",\"client_credentials\",\"iwa:ntlm\"," +
                "\"urn:ietf:params:oauth:grant-type:device_code\"," +
                "\"urn:ietf:params:oauth:grant-type:jwt-bearer\"]," +
                "\"callbackUrl\":\"\"," +
                "\"additionalProperties\":{\"application_access_token_expiry_time\":\"-1\"," +
                "\"user_access_token_expiry_time\":\"N/A\",\"refresh_token_expiry_time\":\"N/A\"," +
                "\"id_token_expiry_time\":\"N/A\"},\"keyManager\":\""+kmid+"\"," +
                "\"validityTime\":-1,\"scopes\":[\"default\"]}\n";
        HttpResponse generateKeysResponse= client.invoke("POST", payload, url,token,null);
        if(generateKeysResponse.getStatusLine().getStatusCode()==200) {
            JsonObject responsejson = null;
            try {
                responsejson = new JsonParser().parse(EntityUtils.toString(generateKeysResponse.getEntity())).getAsJsonObject();
            } catch (IOException e) {
                throw new WSTestException(e);
            }
            applicationtoken  = responsejson.get("token").getAsJsonObject().get("accessToken").getAsString();
            iskeygenerated = true;
            System.out.println(ANSI_GREEN+"#### Test : Generating the application access token passed "+ ANSI_RESET);
        }else {
            System.out.println(ANSI_RED +"#### Test : Generating application access token via devportal rest failed "+ ANSI_RESET);
            System.out.println(ANSI_RED +"#### check the APIM logs, aborting the test"+ ANSI_RESET);
            System.exit(0);
        }
        return iskeygenerated;
    }

    public boolean getKeyManager(){
        boolean getkeymanager = false;
        String url = "https://localhost:"+carbonport+"/api/am/store/v1/key-managers";
        HttpResponse getKeymanagersResponse= client.invoke("GET", null, url,token,null);
        if(getKeymanagersResponse.getStatusLine().getStatusCode()==200) {
            JsonObject responsejson = null;
            try {
                responsejson = new JsonParser().parse(EntityUtils.toString(getKeymanagersResponse.getEntity())).getAsJsonObject();
            } catch (IOException e) {
                throw new WSTestException(e);
            }
            kmid  = responsejson.get("list").getAsJsonArray().get(0).getAsJsonObject().get("id").getAsString();
            getkeymanager = true;
            System.out.println(ANSI_GREEN+"#### Test : Getting key managers passed "+ ANSI_RESET);
        }else {
            System.out.println(ANSI_RED +"#### Test : Getting keymanagers via devportal rest failed "+ ANSI_RESET);
            System.out.println(ANSI_RED +"#### check the APIM logs, aborting the test"+ ANSI_RESET);
            System.exit(0);
        }
        return getkeymanager;
    }

    public boolean getDefaultAppId(){
        boolean getdefaultappid = false;
        String url = "https://localhost:"+carbonport+"/api/am/store/v1/applications";
        HttpResponse getDefaultAppIdResponse= client.invoke("GET", null, url,token,null);
        if(getDefaultAppIdResponse.getStatusLine().getStatusCode()==200) {
            JsonObject responsejson = null;
            try {
                responsejson = new JsonParser().parse(EntityUtils.toString(getDefaultAppIdResponse.getEntity())).getAsJsonObject();
            } catch (IOException e) {
                throw new WSTestException(e);
            }
            appid = responsejson.get("list").getAsJsonArray().get(0).getAsJsonObject().get("applicationId").getAsString();
            System.out.println(ANSI_GREEN+"#### Test : Getting default application id passed "+ ANSI_RESET);
            getdefaultappid = true;
        }else {
            System.out.println(ANSI_RED +"#### Test : Getting default application id via dev portal rest failed "+ ANSI_RESET);
            System.out.println(ANSI_RED +"#### check the APIM logs, aborting the test"+ ANSI_RESET);
            System.exit(0);
        }
        return getdefaultappid;
    }

    public boolean getOauthKeys(){
        boolean getoauthkeys = false;
        String url = "https://localhost:"+carbonport+"/api/am/store/v1/applications/"+appid+"/oauth-keys";
        HttpResponse getoauthkeysResponse= client.invoke("GET", null, url,token,null);
        if(getoauthkeysResponse.getStatusLine().getStatusCode()==200) {
            JsonObject responsejson = null;
            try {
                responsejson = new JsonParser().parse(EntityUtils.toString(getoauthkeysResponse.getEntity())).getAsJsonObject();
            } catch (IOException e) {
                throw new WSTestException(e);
            }
            consumerKey = responsejson.get("list").getAsJsonArray().get(0).getAsJsonObject().get("consumerKey").getAsString();
            consumerSecret = responsejson.get("list").getAsJsonArray().get(0).getAsJsonObject().get("consumerSecret").getAsString();
            System.out.println(ANSI_GREEN+"#### Test : Getting default application keys passed "+ ANSI_RESET);
            getoauthkeys = true;
        }else {
            System.out.println(ANSI_RED +"#### Test : Getting default application keys via dev portal rest failed "+ ANSI_RESET);
            System.out.println(ANSI_RED +"#### check the APIM logs, aborting the test"+ ANSI_RESET);
            System.exit(0);
        }
        return getoauthkeys;

    }

    public void generateDefaultToken(){
        String url  = "https://localhost:"+passthruonport+"/token";
        HttpResponse generateDefaultTokenResponse= client.getToken(url, consumerKey, consumerSecret);
        if(generateDefaultTokenResponse.getStatusLine().getStatusCode()==200) {
            JsonObject responsejson = null;
            try {
                responsejson = new JsonParser().parse(EntityUtils.toString(generateDefaultTokenResponse.getEntity())).getAsJsonObject();
            } catch (IOException e) {
                throw new WSTestException(e);
            }
            applicationtoken = responsejson.get("access_token").getAsString();
            System.out.println(ANSI_GREEN+"#### Test : Getting default application token passed "+ ANSI_RESET);
        }else {
            System.out.println(ANSI_RED +"#### Test : Getting default application token via dev portal rest failed "+ ANSI_RESET);
            System.out.println(ANSI_RED +"#### check the APIM logs, aborting the test"+ ANSI_RESET);
            System.exit(0);
        }

    }

    public void deleteSubscription(String apiid){
        String url = "https://localhost:"+carbonport+"/api/am/store/v1/subscriptions/";
        String[] subsid = getSubscription(apiid);
        String subs1url = url+subsid[0];
        HttpResponse deleteSubscriptionnResponse= client.invoke("DELETE", null, subs1url,token,null);
        if(deleteSubscriptionnResponse.getStatusLine().getStatusCode()==200) {
            System.out.println(ANSI_GREEN+"#### Test : Deleting subscription passed "+subsid[0]+ ANSI_RESET);
        }else {
            System.out.println(ANSI_RED +"#### Test : Deleting subscription: id"+subsid[0]+"via dev portal rest failed "+ ANSI_RESET);
            System.out.println(ANSI_RED +"#### check the APIM logs, aborting the test"+ ANSI_RESET);
            System.exit(0);
        }
        String subs2url = url+subsid[1];
        deleteSubscriptionnResponse= client.invoke("DELETE", null, subs2url,token,null);
        if(deleteSubscriptionnResponse.getStatusLine().getStatusCode()==200) {
            System.out.println(ANSI_GREEN+"#### Test : Deleting subscription passed id "+subsid[1]+ ANSI_RESET);
        }else {
            System.out.println(ANSI_RED +"#### Test : Deleting subscription: id  "+subsid[1]+"via dev portal rest failed "+ ANSI_RESET);
            System.out.println(ANSI_RED +"#### check the APIM logs, aborting the test"+ ANSI_RESET);
            System.exit(0);
        }

    }

    public String[] getSubscription(String apiid){

        String[] subsid = new String[2];
        String url = "https://localhost:"+carbonport+"/api/am/store/v1/subscriptions?apiId="+apiid;
        HttpResponse getSubscriptionResponse= client.invoke("GET", null, url,token,null);
        if(getSubscriptionResponse.getStatusLine().getStatusCode()==200) {
            JsonObject responsejson = null;
            try {
                responsejson = new JsonParser().parse(EntityUtils.toString(getSubscriptionResponse.getEntity())).getAsJsonObject();
            } catch (IOException e) {
                throw new WSTestException(e);
            }
            subsid[0]=responsejson.get("list").getAsJsonArray().get(0).getAsJsonObject().get("subscriptionId").getAsString();
            subsid[1]=responsejson.get("list").getAsJsonArray().get(1).getAsJsonObject().get("subscriptionId").getAsString();
            System.out.println(ANSI_GREEN+"#### Test : Getting api subscription details passed "+ ANSI_RESET);
        }else {
            System.out.println(ANSI_RED +"#### Test : Getting api subscription details id via dev portal rest failed "+ ANSI_RESET);
            System.out.println(ANSI_RED +"#### check the APIM logs, aborting the test"+ ANSI_RESET);
            System.exit(0);
        }
        return subsid;
    }
    public String getToken(){
        return applicationtoken;
    }
}
