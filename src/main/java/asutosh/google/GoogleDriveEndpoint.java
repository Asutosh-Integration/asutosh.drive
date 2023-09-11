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
    private String greetingsMessage;
    
    @UriParam
    private boolean useFormater;

	public boolean getUseFormater() {
		return useFormater;
	}

	public void setUseFormater(boolean useFormater) {
		this.useFormater = useFormater;
	}

	public String getGreetingsMessage() {
		return greetingsMessage;
	}

	public void setGreetingsMessage(String greetingsMessage) {
		this.greetingsMessage = greetingsMessage;
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
}
