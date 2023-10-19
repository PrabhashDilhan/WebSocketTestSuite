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

public class AdminTest {

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_RED = "\u001B[31m";
    String token;

    HttpClient client;
    String carbonport;

    String passthruport;

    public AdminTest(HttpClient client, String port, String passthruport) {
        this.client=client;
        this.carbonport = port;
        this.passthruport=passthruport;
        try {
            token=generateToken();
        } catch (IOException e) {
            throw new WSTestException(e);
        }
        if(token==null){
            System.out.println(ANSI_RED+"####### Token generation returned null for admin rest APIs, stopping the test. Please check the apim logs ########"+ANSI_RESET);
            System.exit(0);
        }

    }

    private String generateToken() throws IOException {
        String accessToken;
        String dcrPayload = "  {\n" +
                "  \"callbackUrl\":\"www.google.lk\",\n" +
                "  \"clientName\":\"rest_api_admin\",\n" +
                "  \"owner\":\"admin\",\n" +
                "  \"grantType\":\"client_credentials password refresh_token\",\n" +
                "  \"saasApp\":true\n" +
                "  }";
        String dcrURL = "https://localhost:"+carbonport+"/client-registration/v0.17/register";
        String basicToken = Base64.getEncoder().encodeToString("admin:admin".getBytes(StandardCharsets.UTF_8));
        HttpResponse response= client.invoke("POST", dcrPayload, dcrURL,null,basicToken);
        if(response.getStatusLine().getStatusCode()==200){
            String tokenURL = "https://localhost:"+passthruport+"/token?grant_type=password&username=admin&password=admin&scope=apim:admin%20apim:tier_view";
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

    public boolean addThrottlePolicy(){
        boolean ispolicyadded = false;
        String url = "https://localhost:"+carbonport+"/api/am/admin/v1/throttling/policies/advanced";
        String payload = "{\"policyName\":\"5PerMin\"" +
                ",\"description\":\"\"," +
                "\"conditionalGroups\":[]," +
                "\"defaultLimit\":{\"requestCount\":{\"timeUnit\":\"min\"," +
                "\"unitTime\":\"1\",\"requestCount\":\"5\"},\"type\":\"REQUESTCOUNTLIMIT\"," +
                "\"bandwidth\":null}}";

        HttpResponse createApiResponse= client.invoke("POST", payload, url,token,null);
        if(createApiResponse.getStatusLine().getStatusCode()==201) {
            ispolicyadded = true;
            System.out.println(ANSI_GREEN+"#### Test 03: Adding throttling policy via admin rest passed "+ANSI_RESET);
        }else {
            System.out.println(ANSI_RED+"#### Test 03: Adding throttling policy via admin rest failed "+ANSI_RESET);
            System.out.println(ANSI_RED+"#### check the APIM logs, aborting the test"+ANSI_RESET);
            System.out.println(ANSI_RED+"#### make sure to delete  the partially created API before running test again"+ANSI_RESET);
            System.exit(0);
        }
        return ispolicyadded;
    }
}
