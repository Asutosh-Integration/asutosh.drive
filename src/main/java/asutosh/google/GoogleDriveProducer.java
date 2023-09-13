/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package asutosh.google;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sap.it.api.securestore.AccessTokenAndUser;
import com.sap.it.asdk.cmd.parser.model.BlueprintMetadataModel;
import org.apache.camel.Exchange;
import org.apache.camel.WrappedFile;
import org.apache.camel.impl.DefaultProducer;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sap.it.api.securestore.SecureStoreService;
import com.sap.it.api.securestore.UserCredential;
import com.sap.it.api.securestore.exception.SecureStoreException;
import com.sap.it.api.ITApiFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;

import org.apache.camel.Exchange;
import com.sap.it.api.msglog.adapter.AdapterMessageLog;
import com.sap.it.api.msglog.adapter.AdapterMessageLogFactory;
import com.sap.it.api.msglog.adapter.AdapterTraceMessage;
import com.sap.it.api.msglog.adapter.AdapterTraceMessageType;

/**
 * The www.Sample.com producer.
 */
public class GoogleDriveProducer extends DefaultProducer {
    private static final transient Logger LOG = LoggerFactory.getLogger(GoogleDriveProducer.class);
    private final AdapterMessageLogFactory msgLogFactory;
    private GoogleDriveEndpoint endpoint;

    public GoogleDriveProducer(GoogleDriveEndpoint endpoint) {
        super(endpoint);
        this.endpoint = endpoint;
        msgLogFactory = (AdapterMessageLogFactory) this.endpoint.getCamelContext().getRegistry()
                .lookupByName(com.sap.it.api.msglog.adapter.AdapterMessageLogFactory.class.getName());
        ;
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
    }

    public void process(final Exchange exchange) throws Exception {
        InputStream is = exchange.getIn().getBody(InputStream.class);

        String greetingMessage = endpoint.getGreetingsMessage();
        String operation = endpoint.getOperation();
        String filePath = endpoint.getFilePath();

        if (filePath.startsWith("/") && filePath != null) {
            filePath = filePath.substring(1);
        } else {
            throw new Exception("Wrong File path");
        }

        SecureStoreService secureStoreService = ITApiFactory.getService(SecureStoreService.class, null);
        AccessTokenAndUser accessTokenAndUser = secureStoreService.getAccesTokenForOauth2AuthorizationCodeCredential(greetingMessage);

        String UserName = accessTokenAndUser.getUser();
        String Token = accessTokenAndUser.getAccessToken();

        if (Token != null) {
            LOG.debug(Token);
            writeTrace(exchange, Token.getBytes(StandardCharsets.UTF_8), true);
        }

        String fileId = null;
        InputStream payload = null;
        String folderId = null;

        if (operation.equals("DOWNLOAD")) {
            // Create an HttpClient instance
            HttpClient httpClient = HttpClients.createDefault();

            // Start the recursive search
            fileId = searchFileOrFolder(httpClient, Token, "root", filePath);
            exchange.getIn().setHeader("fileId", fileId);

            payload = downloadFile(httpClient, Token, fileId);
            writeTrace(exchange, "Download Successful".getBytes(StandardCharsets.UTF_8), true);

        } else if (operation.equals("UPLOAD")) {
            // Create an HttpClient instance
            HttpClient httpClient = HttpClients.createDefault();
            folderId = searchFileOrFolder(httpClient, Token, "root", filePath.substring(0, filePath.lastIndexOf("/")));
            // Create an HTTP POST request for file upload
            HttpPost httpPost = new HttpPost("https://www.googleapis.com/upload/drive/v3/files?uploadType=resumable");

            // Specify the folder ID as a parent in the file metadata
            String jsonMetadata = "{" +
                    "\"name\": \"" + filePath.substring(filePath.lastIndexOf("/") + 1) + "\"," +
                    "\"parents\": [\"" + folderId + "\"]" +
                    "}";

            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setEntity(new ByteArrayEntity(jsonMetadata.getBytes()));

            // Add the Bearer token to the Authorization header
            httpPost.setHeader("Authorization", "Bearer " + Token);

            // Execute the initial POST request to create the resumable upload session
            HttpResponse response = httpClient.execute(httpPost);

            // Check the response status code
            int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode == 200) {
                // Resumable upload session created successfully
                LOG.trace("1st call successful");
                writeTrace(exchange, "1st call successful".getBytes(StandardCharsets.UTF_8), true);
                // Continue with sending the file data in chunks
                int totalSize = is.available();
                payload = uploadContentInChunks(httpClient, response.getFirstHeader("Location").getValue(), is, Token, totalSize);
                writeTrace(exchange, "2nd call successful".getBytes(StandardCharsets.UTF_8), true);
            } else {
                // Handle errors here
                String err = "1st call unsuccessful" + EntityUtils.toString(response.getEntity());
                writeTrace(exchange, err.getBytes(StandardCharsets.UTF_8), true);
            }
        }
        exchange.getIn().setBody(payload);
    }

    String searchFileOrFolder(HttpClient httpClient, String bearerToken, String folderId, String filePath) throws Exception {
        // Define the API endpoint URL for listing files and folders in the current folder
        String apiUrl = "https://www.googleapis.com/drive/v3/files?q='" + folderId + "'+in+parents+and+trashed=false";

        // Create an HTTP GET request with the API URL
        HttpGet httpGet = new HttpGet(apiUrl);

        // Add the Bearer token to the request header for authentication
        httpGet.addHeader("Authorization", "Bearer " + bearerToken);

        // Execute the request
        HttpResponse response = httpClient.execute(httpGet);

        // Check the response status code
        int statusCode = response.getStatusLine().getStatusCode();

        if (statusCode == 200) {
            // Request was successful
            String responseBody = EntityUtils.toString(response.getEntity());

            // Parse the JSON response using Jackson
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(responseBody);

            // Split the file path into components and ignore the first element (root)
            String[] pathComponents = filePath.split("/");
            if (pathComponents.length > 0) {
                pathComponents = Arrays.copyOfRange(pathComponents, 0, pathComponents.length);
            }

            // Check if the target exists in this folder
            for (JsonNode item : jsonNode.get("files")) {
                if (item.has("name") && item.get("name").asText().equals(pathComponents[0])) {
                    // If it's a folder, continue searching inside the folder
                    if (item.has("mimeType") && item.get("mimeType").asText().equals("application/vnd.google-apps.folder")) {
                        String subfolderId = item.get("id").asText();
                        if (pathComponents.length == 1) {
                            return item.get("id").asText();
                        } else {
                            return searchFileOrFolder(httpClient, bearerToken, subfolderId, String.join("/", Arrays.copyOfRange(pathComponents, 1, pathComponents.length)));
                        }
                    } else if (item.has("mimeType") && !item.get("mimeType").asText().equals("application/vnd.google-apps.folder")) {
                        if (pathComponents.length == 1) {
                            return item.get("id").asText(); // Found the target folder
                        } else {
                            System.err.println("Error: Wrong Path");
                        }
                    }
                }
            }
        } else {
            // Handle errors here
            System.err.println("Error: API request failed with status code " + statusCode);
        }

        return null; // File or folder not found in this folder
    }

    InputStream downloadFile(HttpClient httpClient, String bearerToken, String fileId) throws Exception {
        // Define the API endpoint URL for listing files and folders in the current folder
        String apiUrl = "https://www.googleapis.com/drive/v3/files/" + fileId + "?alt=media";

        // Create an HTTP GET request with the API URL
        HttpGet httpGet = new HttpGet(apiUrl);

        // Add the Bearer token to the request header for authentication
        httpGet.addHeader("Authorization", "Bearer " + bearerToken);

        // Execute the request
        HttpResponse response = httpClient.execute(httpGet);

        // Check the response status code
        int statusCode = response.getStatusLine().getStatusCode();

        if (statusCode == 200) {
            return response.getEntity().getContent();
        }
        return null;
    }

    InputStream uploadContentInChunks(HttpClient httpClient, String uploadUrl, InputStream is, String bearerToken, int totalSize) {
        try {
            byte[] buffer = new byte[256 * 1024 * 100 * 2]; // 256 KB buffer
            int bytesRead;
            long offset = 0;
            while ((bytesRead = is.read(buffer)) != -1) {
                // Calculate the Content-Range header
                long startByte = offset;
                long endByte = offset + bytesRead - 1;
                String contentRange = "bytes " + startByte + "-" + endByte + "/" + totalSize;

                // Create an HTTP PUT request to upload a chunk of content with Content-Range header
                HttpPut httpPut = new HttpPut(uploadUrl);
                httpPut.setHeader("Authorization", "Bearer " + bearerToken);
                httpPut.setHeader("Content-Range", contentRange);

                // Create an InputStreamEntity for the chunk
                InputStreamEntity inputStreamEntity = new InputStreamEntity(new ByteArrayInputStream(buffer, 0, bytesRead));

                // Set the content type (you may adjust this based on your content)
                inputStreamEntity.setContentType("text/plain");

                // Set the entity for the request
                httpPut.setEntity(inputStreamEntity);

                // Execute the PUT request to upload the chunk
                HttpResponse response = httpClient.execute(httpPut);

                // Check the response status code
                int statusCode = response.getStatusLine().getStatusCode();

                if (statusCode == 308) {
                    // Chunk upload successful, continue with the next chunk
                    offset += bytesRead;
                    System.out.println("Uploaded chunk: " + contentRange);
                } else if (statusCode == 200 || statusCode == 201) {
                    // Chunk uploaded successfully
                    return response.getEntity().getContent();
                } else {
                    // Handle errors here
                    return response.getEntity().getContent();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    void writeTrace(Exchange exchange, byte[] traceData, boolean isOutbound) {
        // replace "<adapter type>" by your adapter type!
        String text = isOutbound ? "Sending Google Drive message" : "Receiving Google Drive message";
        // replace "<CMD component ID of the adapter>" by your CMD component ID
        AdapterMessageLog mplLog = msgLogFactory.getMessageLog(exchange, text, "ctype::Adapter/cname::GoogleDrive/vendor::Asutosh/version::1.0.0",
                UUID.randomUUID().toString());
        if (!mplLog.isTraceActive()) {
            return;
        }
        // if you have a fault inbound message then specify AdapterTraceMessageType.RECEIVER_INBOUND_FAULT,
        // if you have a fault outbound message then specify AdapterTraceMessageType.SENDER_OUTBOUND_FAULT
        // for synchronous adapters you may also need AdapterTraceMessageType.SENDER_OUTBOUND and AdapterTraceMessageType.RECEIVER_INBOUND
        AdapterTraceMessageType type = isOutbound ? AdapterTraceMessageType.RECEIVER_OUTBOUND : AdapterTraceMessageType.SENDER_INBOUND;

        AdapterTraceMessage traceMessage = mplLog.createTraceMessage(type, traceData, false);//Setting isTruncated as false assuming traceData is less than 25MB.
        // Encoding is optional, but should be set if available.
        traceMessage.setEncoding("UTF-8");
        // Headers are optional and do not forget to obfuscate security relevant header values.
        mplLog.writeTrace(traceMessage);
    }

}

