package org.wso2.test.websocket.server;

import org.wso2.test.WSTestException;


public class PythonServer {

    public void run(String scriptpath){
        try {
            // Specify the Python script to run
            String pythonScript = scriptpath;

            // Create a ProcessBuilder for running the Python script
            ProcessBuilder processBuilder = new ProcessBuilder("python3", pythonScript);

            // Start the process
            Process process = processBuilder.start();

        } catch (Exception e) {
            throw new WSTestException(e);
        }
    }
}
