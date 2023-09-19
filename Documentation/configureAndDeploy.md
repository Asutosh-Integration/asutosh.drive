# Deploying a Custom Google Drive Adapter in SAP Integration Suite

In this guide, we will explore two methods for deploying a Custom Google Drive Adapter in SAP Integration Suite:

1. **Direct Upload using Web UI**
2. **Upload using API**

## Method 1: Direct Upload using Web UI

*Note: This method may take long time to upload.*

### Step 1: Log in to SAP Integration Suite

1. Navigate to the SAP Integration Suite Web UI.
2. Log in to your SAP Integration Suite account.

### Step 2: Access the Integration Package

1. In the SAP Integration Suite Web UI, go to the "Design" section.
2. Locate and select the integration package where you want to deploy the Custom Google Drive Adapter.

### Step 3: Download the Adapter File

1. Download the `asutosh.drive.esa` file from the [build path](../target/build/asutosh.drive.esa).
2. Save it locally.

### Step 4: Upload the Custom Adapter

1. Inside the integration package, click "Edit," then navigate to the "Artifacts" tab.
2. Click the "Add" button and select "Integration Adapter" to create a new custom integration adapter.
3. Click the "Browse" button to select the `asutosh.drive.esa` file for the adapter.
4. Click "Ok."

### Step 5: Deploy the Integration Flow

1. Navigate to the "Action" button and select the "Deploy" option.
2. Monitor the deployment to ensure it's successful.

## Method 2: Upload using API

### Step 1: Obtain Service Keys

1. Navigate to the [BTP Cockpit](https://emea.cockpit.btp.cloud.sap/).
2. Go to your respective Sub-account and click on "Instances and Subscriptions" to check for the service instance.
3. Check for the Service `Process Integration Runtime` and Plan `api`. If not found, quickly navigate to `Service Marketplace` and search for `Process Integration Runtime` service.
4. Create a new instance and choose Plan as `api`.
5. Provide an appropriate Instance Name and click on the next button. Select `AuthGroup_IntegrationDeveloper` and `WorkspacePackagesEdit` roles. Then click on the "Create" button.
6. Now navigate to the instance and create a Service Key for the instance by providing an appropriate `Service Key Name`.
7. Copy the JSON file to a secure location.

### Step 2: Postman Setup

1. Navigate to the [SAP Business Accelerator Hub](https://api.sap.com/api/IntegrationContent/overview).
2. Download the API Specification as JSON.
3. Go to the Postman application and click on the `import` button. Drag and drop the JSON specification and click on the `import` button to create the collection.
4. Navigate to the `Integration Content` collection.
5. Go to the `Authorization` tab. Select `OAuth 2.0` as the `Type` and `Client Credentials` as the `Grant Type`.
6. Add the `Access Token URL`, `Client ID`, and `Client Secret` as per the Service Key JSON file created in Step 1.
7. Now navigate to the `Variables` tab and give your Cloud Integration API URL (e.g., `https://<cf name>.it-<cf id>.cfapps.jp10.hana.ondemand.com/api/v1`. You can get the same URL from the Service Key JSON `url` field + `/api/v1`).
8. Now navigate to the `Integration Content/IntegrationPackages/Get all integration packages.` request and Add the header `x-csrf-token` with the value `fetch`. Click on the `Send` button and copy the `x-csrf-token` header value and package `Id` from the response.

### Step 3: Import the Adapter

1. Clone GitHub repo to your local PC either using `git clone https://github.com/Asutosh-Integration/asutosh.drive.git` command (if you have `git` installed) or else you can directly Download ZIP from GitHub.
2. Now Open the Project in any IDE of your choice and run `src/main/java/asutosh/google/FileToBase64AndSaveToFile.java` file.
3. Now navigate to `target/build/asutosh.drive-Base64.txt` path and copy the file content (Base64 encoded string of `asutosh.drive.esa` file).
4. Navigate to the Postman tool and `Integration Content/IntegrationAdapterDesigntimeArtifacts/Import integration adapter artifact` request.
5. Go to the body and add below JSON (After replacing `Base64Content` and `packageID` with the copied file content and Integration package ID).
   ```json
   {
     "PackageId": "packageID",
     "ArtifactContent": "Base64Content"
   }
   ```
6. Add the header `X-CSRF-Token` with the value received in the previous request. 
7. Use the Send button to import the custom adapter into the SAP Integration Suite tenant.

### Step 4: Deployment

1. Navigate to the SAP Integration Suite Web UI and deploy the adapter.
2. Ensure that the Custom Google Drive Adapter is successfully deployed.

Congratulations! You have successfully deployed a Custom Google Drive Adapter in SAP Integration Suite using either of the above two different methods.

**Note**: For more detailed instructions and troubleshooting tips, refer to SAP Integration Suite [documentation](https://help.sap.com/docs/cloud-integration/sap-cloud-integration/importing-custom-integration-adapter-in-cloud-foundry-environment) or consult your SAP support resources.