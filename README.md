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
<li>scriptpath:path-to-python-websocket-servers-cript</li>
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
      "url":"ws://localhost:7777"},
    "production_endpoints":{
      "url":"ws://localhost:7777"}
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
      "url":"ws://localhost:7777"},
    "production_endpoints":{
      "url":"ws://localhost:7777"}
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

## websocket.py <br />

```js

import asyncio
import websockets

async def handle_connection(websocket, path):
    async for message in websocket:
        await websocket.send("Received: " + message)

start_server = websockets.serve(handle_connection, "localhost", 7777)

asyncio.get_event_loop().run_until_complete(start_server)
asyncio.get_event_loop().run_forever()

```
if the required python packages are not installed, you need to install those. before running the test, let's verify whether the python server can be run with python3 websocket.py <br />

## How to run the client <br />
**java -jar WSTesting-1.0-jar-with-dependencies.jar path-to-config.properties-file**

if you need to run the test cases again, make sure to delete all the created APIs, applications, and throttling policies. And also, make sure to revert the change in _cors_request_handler_.xml <br />
