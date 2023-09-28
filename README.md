# WebSocketTestSuite
## config.properties <br />
 
<ul>
<li>serverhome:path-to-wso2-apim-server-home</li>
<li>storepassword:key-store-password</li>
<li>keypassword:key-password</li>
<li>websocketport:8099</li>
<li>carbonport:9443</li>
<li>passthruonport:8243</li>
<li>firstapipath:path-first-api-json</li>
<li>secondapipath:path-seconnd-api-json</li>
<li>corshandlerpath:path-to-_cors_request_handler_.xml</li>
</ul>

## First API <br />
```json

{"name":"test3",
  "version":"v1",
  "context":"/test3",
  "policies":["Bronze"],
  "endpointConfig":{
    "endpoint_type":"http",
    "sandbox_endpoints":{
      "url":"wss://localhost:7777"},
    "production_endpoints":{
      "url":"wss://localhost:7777"}
  },
  "gatewayEnvironments":["Production and Sandbox"],
  "type":"WS"
}

```

## Second API <br />
```json

{"name":"test4",
  "version":"v1",
  "context":"/test4",
  "policies":["Unlimited"],
  "endpointConfig":{
    "endpoint_type":"http",
    "sandbox_endpoints":{
      "url":"wss://localhost:7778"},
    "production_endpoints":{
      "url":"wss://localhost:7778"}
  },
  "gatewayEnvironments":["Production and Sandbox"],
  "type":"WS"
}

```

## _cors_request_handler_.xml <br />

```xml

<sequence xmlns="http://ws.apache.org/ns/synapse" name="_cors_request_handler_">
    <log level="custom">
        <property name="OriginTest===============1111" expression="get-property('WSOrigin')" />
    </log>
    <filter source="boolean($ctx:WSOrigin)" regex="true">
        <then>
            <property name="OriginTest" expression="$ctx:WSOrigin" />
        </then>
    </filter>
    <log level="custom">
        <property name="OriginTest===============222" expression="get-property('OriginTest')" />
    </log>
    <switch xmlns:ns="http://org.apache.synapse/xsd" source="get-property('OriginTest')">
        <case regex="(https):\/\/((wso2.prod))\.london\.(com)$">
            <log level="custom">
                <property name="OriginTest===============case Pass" expression="get-property('WSOrigin')" />
            </log>
            <property name="WSCORSOriginSuccess" value="true" type="BOOLEAN" />
        </case>
        <default>
            <log level="custom">
                <property name="OriginTest===============default" expression="get-property('WSOrigin')" />
            </log>
            <property name="WSCORSOriginSuccess" value="false" type="BOOLEAN" />
        </default>
    </switch>
</sequence>

```

## How to run the client <br />
**java -jar WSTesting-1.0-jar-with-dependencies.jar path-to-config.properties-file**
