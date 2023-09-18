# Custom Google Drive Adapter for SAP Integration Suite

This repository contains a custom Google Drive adapter for SAP Integration Suite, designed to facilitate seamless integration between SAP systems and Google Drive. This adapter allows you to connect your SAP Integration Suite with Google Drive, enabling the exchange of data, documents, and other resources between these platforms.

## Features

- **Bi-Directional Integration:** This adapter supports both inbound and outbound data transfer between SAP and Google Drive, allowing you to push data from SAP to Google Drive and pull data from Google Drive into SAP.

- **Secure Authentication:** The adapter provides secure authentication mechanisms to ensure that your data remains protected during the integration process.

- **Customizable Configuration:** You can easily configure the adapter to suit your specific integration needs, including specifying folders and document types within Google Drive.

- **Real-time and Batch Processing:** The adapter supports real-time and batch processing, giving you flexibility in how you exchange data between SAP and Google Drive.

- **Sender and Receiver Adapter:** This adapter support both Sender and Receiver type configuration.

- **Scheduler Support:** Sender adapter can be configured to run as per the schedule.

- **Externalised Parameters:** All the parameters of the adapter can be externalised.

- **Simple Camel Expression Support:** Simple Camel expressions are supported as input variables.

- **Archive and Delete Support:** Archiving with or without added timestamp supported.

- **Huge File Upload Support:** A resumable upload method is used to upload huge data in chunks.

## Getting Started

To get started with this custom Google Drive adapter for SAP Integration Suite, follow these steps:

1. **Download the Adapter File:** Download the ESA file from the [target folder](target/build/asutosh.drive.esa).

2. **Importing Custom Integration Adapter:** import custom integration adapter in your integration package. Please go through the [documentation](https://help.sap.com/docs/cloud-integration/sap-cloud-integration/importing-custom-integration-adapter-in-cloud-foundry-environment) for detailed step-by-step guide.

3. **Setup Authentication:** Create Security Material (Type OAuth2 Authorization Code (Generic)). Follow the [Wiki](Documentation/createSecurityMaterial.md) section for detailed guide.

4. **Configure and Deploy:** Configure the adapter in the iFlow and deploy it to your SAP Integration Suite tenant. Follow the [Wiki](Documentation/configureAndDeploy.md) section for detailed guide.

## Documentation

For detailed documentation on how to configure, deploy, and use this custom Google Drive adapter for SAP Integration Suite, please refer to the [Wiki](Documentation) section of this repository.

### Google Drive API v3 Documentation

For more information about the Google Drive API v3, refer to the official documentation:

- [Google Drive API Overview](https://developers.google.com/drive/api/guides/about-sdk)
- [Authentication Overview](https://developers.google.com/workspace/guides/create-credentials#oauth-client-id)
- [API Reference](https://developers.google.com/drive/api/reference/rest/v3)
- [Uploading Files](https://developers.google.com/drive/api/guides/manage-uploads)
- [Search for files and folders](https://developers.google.com/drive/api/guides/search-files)
- [Download & export files](https://developers.google.com/drive/api/guides/manage-downloads)
- [Manage file metadata](https://developers.google.com/drive/api/guides/file)
- [Handle Errors](https://developers.google.com/drive/api/guides/handle-errors)
- [Google API Console](https://console.cloud.google.com/)


## Contributing

Contributions to this project are welcome! If you find any issues, have feature requests, or would like to contribute to the development of this adapter, please feel free to open an issue or submit a pull request.

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

## Support

If you encounter any problems or have questions regarding this adapter, please feel free to [contact me](mailto:asutoshmaharana23@gmail.com).

---

**Note:** This is a custom adapter developed by the community and is not officially endorsed or supported by SAP or Google. Use it at your own discretion and ensure that it complies with your organization's security and compliance policies.

