package asutosh.google;


/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.net.URISyntaxException;

import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.impl.DefaultPollingEndpoint;
import org.apache.camel.spi.UriEndpoint;
import org.apache.camel.spi.UriParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a www.Sample.com Camel endpoint.
 */
@UriEndpoint(scheme = "scheme", syntax = "", title = "")
public class GoogleDriveEndpoint extends DefaultPollingEndpoint {
    private GoogleDriveComponent component;

    private transient Logger logger = LoggerFactory.getLogger(GoogleDriveEndpoint.class);

    @UriParam
    private String OauthCred;
    
    @UriParam
    private boolean useFormater;

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    @UriParam
    private String operation;

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    @UriParam
    private String filePath;

    @UriParam
    private Boolean Archive;

    @UriParam
    private String archiveFilePath;

    @UriParam
    private Boolean Delete;

    @UriParam
    private Boolean addTimestamp;

    @UriParam
    private String operationSender;

	public boolean getUseFormater() {
		return useFormater;
	}

	public void setUseFormater(boolean useFormater) {
		this.useFormater = useFormater;
	}

	public String getOauthCred() {
		return OauthCred;
	}

	public void setOauthCred(String oauthCred) {
		this.OauthCred = oauthCred;
	}

	public GoogleDriveEndpoint() {
    }

    public GoogleDriveEndpoint(final String endpointUri, final GoogleDriveComponent component) throws URISyntaxException {
        super(endpointUri, component);
        this.component = component;
    }

    public GoogleDriveEndpoint(final String uri, final String remaining, final GoogleDriveComponent component) throws URISyntaxException {
        this(uri, component);
    }

    public Producer createProducer() throws Exception {
        return new GoogleDriveProducer(this);
    }

    public Consumer createConsumer(Processor processor) throws Exception {
        final GoogleDriveConsumer consumer = new GoogleDriveConsumer(this, processor);
        configureConsumer(consumer);
        return consumer;
    }

    public boolean isSingleton() {
        return true;
    }

    public Boolean getArchive() {
        return Archive;
    }

    public GoogleDriveEndpoint setArchive(Boolean archive) {
        Archive = archive;
        return this;
    }

    public String getArchiveFilePath() {
        return archiveFilePath;
    }

    public GoogleDriveEndpoint setArchiveFilePath(String archiveFilePath) {
        this.archiveFilePath = archiveFilePath;
        return this;
    }

    public Boolean getDelete() {
        return Delete;
    }

    public GoogleDriveEndpoint setDelete(Boolean delete) {
        Delete = delete;
        return this;
    }

    public Boolean getAddTimestamp() {
        return addTimestamp;
    }

    public GoogleDriveEndpoint setAddTimestamp(Boolean addTimestamp) {
        this.addTimestamp = addTimestamp;
        return this;
    }

    public String getOperationSender() {
        return operationSender;
    }

    public GoogleDriveEndpoint setOperationSender(String operationSender) {
        this.operationSender = operationSender;
        return this;
    }
}
