package asutosh.google;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sap.it.api.msglog.adapter.AdapterMessageLog;
import com.sap.it.api.msglog.adapter.AdapterTraceMessage;
import com.sap.it.api.msglog.adapter.AdapterTraceMessageType;
import org.apache.camel.Exchange;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.util.EntityUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.UUID;

public class GoogleDriveUtil {

    public static String searchFileOrFolder(HttpClient httpClient, String bearerToken, String folderId, String filePath) throws Exception {
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
                            //Return FolderId
                            return item.get("id").asText();
                        } else {
                            //Recursive function call
                            return searchFileOrFolder(httpClient, bearerToken, subfolderId, String.join("/", Arrays.copyOfRange(pathComponents, 1, pathComponents.length)));
                        }
                    } else if (item.has("mimeType") && !item.get("mimeType").asText().equals("application/vnd.google-apps.folder")) {
                        if (pathComponents.length == 1) {
                            return item.get("id").asText(); // Found the target folder
                        } else {
                            throw new Exception("Error: A File Found with the same name as the Folder Or Wrong path configured.");
                        }
                    }
                }
            }
        } else {
            // Handle errors here
            throw new Exception("Error: API request to search file/folder failed with status code " + statusCode);
        }
        throw new Exception("Error: File/folder not found.");
    }

    public static InputStream downloadFile(HttpClient httpClient, String bearerToken, String fileId) throws Exception {
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
        } else {
            throw new Exception("Error: Download URL is returning error status code: " + statusCode + "\n HTTP Response Body: " + EntityUtils.toString(response.getEntity()));
        }
    }

    public static InputStream uploadContentInChunks(HttpClient httpClient, String uploadUrl, InputStream is, String bearerToken, int totalSize) throws Exception {
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
                } else if (statusCode == 200 || statusCode == 201) {
                    // Chunk uploaded successfully
                    return response.getEntity().getContent();
                } else {
                    // Handle errors here
                    throw new Exception("Error: upload URL is returning error status code: " + statusCode + "\n HTTP Response Body: " + EntityUtils.toString(response.getEntity()));
                }
            }
        } catch (IOException e) {
            throw new Exception(e);
        } catch (Exception e) {
            throw new Exception(e);
        }
        return null;
    }
}
