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
package asutosh.google;

import com.sap.it.api.securestore.AccessTokenAndUser;
import com.sap.it.asdk.cmd.parser.model.BlueprintMetadataModel;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sap.it.api.securestore.SecureStoreService;
import com.sap.it.api.securestore.UserCredential;
import com.sap.it.api.securestore.exception.SecureStoreException;
import com.sap.it.api.ITApiFactory;

/**
 * The www.Sample.com producer.
 */
public class GoogleDriveProducer extends DefaultProducer {
    private static final transient Logger LOG = LoggerFactory.getLogger(GoogleDriveProducer.class);
    private GoogleDriveEndpoint endpoint;

	public GoogleDriveProducer(GoogleDriveEndpoint endpoint) {
        super(endpoint);
        this.endpoint = endpoint;
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
    }

    public void process(final Exchange exchange) throws Exception {
        String input = exchange.getIn().getBody(String.class);
		String greetingMessage = endpoint.getGreetingsMessage();

        SecureStoreService secureStoreService = ITApiFactory.getService(SecureStoreService.class, null);
        AccessTokenAndUser accessTokenAndUser = secureStoreService.getAccesTokenForOauth2AuthorizationCodeCredential(greetingMessage);

        String UserName = accessTokenAndUser.getUser();
        String Token = accessTokenAndUser.getAccessToken();

		if (Token != null) {
		    LOG.debug(Token);
		}
		exchange.getIn().setBody(Token);
    }

}
