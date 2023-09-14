package asutosh.google;
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


import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sap.it.api.ITApiFactory;
import com.sap.it.api.msglog.adapter.AdapterMessageLog;
import com.sap.it.api.msglog.adapter.AdapterMessageLogFactory;
import com.sap.it.api.msglog.adapter.AdapterTraceMessage;
import com.sap.it.api.msglog.adapter.AdapterTraceMessageType;
import com.sap.it.api.securestore.AccessTokenAndUser;
import com.sap.it.api.securestore.SecureStoreService;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.impl.ScheduledPollConsumer;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The Sample.com consumer.
 */
public class GoogleDriveConsumer extends ScheduledPollConsumer {
    private Logger LOG = LoggerFactory.getLogger(GoogleDriveConsumer.class);
    private final AdapterMessageLogFactory msgLogFactory;
    private final GoogleDriveEndpoint endpoint;


    public GoogleDriveConsumer(final GoogleDriveEndpoint endpoint, final Processor processor) {
        super(endpoint, processor);
        this.endpoint = endpoint;
        msgLogFactory = (AdapterMessageLogFactory) this.endpoint.getCamelContext().getRegistry()
                .lookupByName(com.sap.it.api.msglog.adapter.AdapterMessageLogFactory.class.getName());
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
    }

    @Override
    protected int poll() throws Exception {
        Exchange exchange = endpoint.createExchange();

        // create a message body
        String OauthCred = endpoint.getOauthCred();
        String operation = endpoint.getOperationSender();
        String filePath = endpoint.getFilePath();

        if (filePath.startsWith("/") && filePath != null) {
            filePath = filePath.substring(1);
        } else {
            throw new Exception("Wrong File path");
        }

        SecureStoreService secureStoreService = ITApiFactory.getService(SecureStoreService.class, null);
        AccessTokenAndUser accessTokenAndUser = secureStoreService.getAccesTokenForOauth2AuthorizationCodeCredential(OauthCred);

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
            folderId = searchFileOrFolder(httpClient, Token, "root", filePath.substring(0, filePath.lastIndexOf("/")));
            exchange.getIn().setHeader("fileId", fileId);

            payload = downloadFile(httpClient, Token, fileId);
            writeTrace(exchange, "Download Successful".getBytes(StandardCharsets.UTF_8), true);
            Boolean archive = endpoint.getArchive();
            Boolean delete = endpoint.getDelete();

            if (archive) {
                String archivePath = endpoint.getArchiveFilePath();
                if (archivePath.startsWith("/") && archivePath != null) {
                    archivePath = archivePath.substring(1);
                } else {
                    throw new Exception("Wrong File path");
                }
                String archiveFolderId = searchFileOrFolder(httpClient, Token, "root", archivePath.substring(0, archivePath.lastIndexOf("/")));
                String path = archivePath + "\n" + archiveFolderId;
                writeTrace(exchange, path.getBytes(StandardCharsets.UTF_8), true);
                if (!delete) {
                    HttpPost httpPost = new HttpPost("https://www.googleapis.com/drive/v3/files/" + fileId + "/copy");
                    httpPost.addHeader("Authorization", "Bearer " + Token);
                    HttpResponse response = httpClient.execute(httpPost);
                    int statusCode = response.getStatusLine().getStatusCode();

                    if (statusCode == 200) {
                        String success = "Copy Successful";
                        writeTrace(exchange, success.getBytes(StandardCharsets.UTF_8), true);
                        String responseBody = EntityUtils.toString(response.getEntity());

                        // Parse the JSON response using Jackson as the response is less in size
                        ObjectMapper objectMapper = new ObjectMapper();
                        JsonNode jsonNode = objectMapper.readTree(responseBody);

                        fileId = jsonNode.get("id").textValue();
                    } else {
                        // Handle errors here
                        String err = "Copy unsuccessful" + EntityUtils.toString(response.getEntity());
                        writeTrace(exchange, err.getBytes(StandardCharsets.UTF_8), true);
                    }
                }
                String arcFileName = archivePath.substring(archivePath.lastIndexOf("/") + 1);
                Boolean addTimestamp = endpoint.getAddTimestamp();
                if(addTimestamp){
                    Timestamp timestamp = new Timestamp(System.currentTimeMillis());
                    arcFileName = arcFileName.substring(0,arcFileName.lastIndexOf(".") + 1) + timestamp + arcFileName.substring(arcFileName.lastIndexOf(".") + 1);
                }
                HttpPatch httpPatch = new HttpPatch("https://www.googleapis.com/drive/v3/files/" + fileId + "?addParents=" + archiveFolderId + "&removeParents=" + folderId + "&alt=json");
                String jsonMetadata = "{" +
                        "\"name\": \"" + arcFileName + "\"" +
                        "}";
                httpPatch.setHeader("Content-Type", "application/json");
                httpPatch.setHeader("Authorization", "Bearer " + Token);
                httpPatch.setEntity(new ByteArrayEntity(jsonMetadata.getBytes()));

                HttpResponse response = httpClient.execute(httpPatch);
                int statusCode = response.getStatusLine().getStatusCode();

                if (statusCode == 200) {
                    String success = "Archive Successful" + EntityUtils.toString(response.getEntity());
                    writeTrace(exchange, success.getBytes(StandardCharsets.UTF_8), true);
                } else {
                    // Handle errors here
                    String err = "Archieve unsuccessful" + EntityUtils.toString(response.getEntity());
                    writeTrace(exchange, err.getBytes(StandardCharsets.UTF_8), true);
                }
            } else {
                if (delete) {
                    HttpPatch httpPatch = new HttpPatch("https://www.googleapis.com/drive/v3/files/" + fileId);
                    String jsonMetadata = "{\"trashed\":true}";
                    httpPatch.setHeader("Content-Type", "application/json");
                    httpPatch.setHeader("Authorization", "Bearer " + Token);
                    httpPatch.setEntity(new ByteArrayEntity(jsonMetadata.getBytes()));

                    HttpResponse response = httpClient.execute(httpPatch);
                    int statusCode = response.getStatusLine().getStatusCode();

                    if (statusCode == 200) {
                        String success = "Move to Trash Successful" + EntityUtils.toString(response.getEntity());
                        writeTrace(exchange, success.getBytes(StandardCharsets.UTF_8), true);
                    } else {
                        // Handle errors here
                        String err = "Move to Trash unsuccessful" + EntityUtils.toString(response.getEntity());
                        writeTrace(exchange, err.getBytes(StandardCharsets.UTF_8), true);
                    }
                }
            }
        }

        exchange.getIn().setBody(payload);

        try {
            // send message to next processor in the route
            getProcessor().process(exchange);
            return 1; // number of messages polled
        } finally {
            // log exception if an exception occurred and was not handled
            if (exchange.getException() != null) {
                getExceptionHandler().handleException("Error processing exchange", exchange, exchange.getException());
            }
        }
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
