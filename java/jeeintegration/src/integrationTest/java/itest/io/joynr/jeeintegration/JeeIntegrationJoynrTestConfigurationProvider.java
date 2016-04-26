/**
 *
 */
package itest.io.joynr.jeeintegration;

/*
 * #%L
 * %%
 * Copyright (C) 2011 - 2016 BMW Car IT GmbH
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import java.util.Properties;

import javax.ejb.Singleton;
import javax.enterprise.inject.Produces;

import io.joynr.jeeintegration.api.JeeIntegrationPropertyKeys;
import io.joynr.jeeintegration.api.JoynrLocalDomain;
import io.joynr.jeeintegration.api.JoynrProperties;

import io.joynr.messaging.MessagingPropertyKeys;

/**
 * A test provider of joynr configuration values for the integration tests of
 * the JEE Integration Bean.
 *
 * @author clive.jevons commissioned by MaibornWolff
 */
@Singleton
public class JeeIntegrationJoynrTestConfigurationProvider {

	@Produces
	@JoynrProperties
	public Properties joynrProperties() {
		Properties joynrProperties = new Properties();
		joynrProperties.setProperty(MessagingPropertyKeys.PROPERTY_SERVLET_CONTEXT_ROOT,
				"/io.joynr.jeeintegration.providerwar/messaging");
		joynrProperties.setProperty(MessagingPropertyKeys.PROPERTY_SERVLET_HOST_PATH, "http://localhost:28585");
		joynrProperties.setProperty(JeeIntegrationPropertyKeys.JEE_INTEGRATION_ENDPOINTREGISTRY_URI, "http://localhost:18080");
		return joynrProperties;
	}

	@Produces
	@JoynrLocalDomain
	public String joynrLocalDomain() {
		return "io.joynr.jeeintegration";
	}

}
