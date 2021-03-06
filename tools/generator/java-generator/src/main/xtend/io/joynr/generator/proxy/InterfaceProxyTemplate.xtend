package io.joynr.generator.proxy
/*
 * !!!
 *
 * Copyright (C) 2011 - 2016 BMW Car IT GmbH
 *
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
 */

import com.google.inject.Inject
import io.joynr.generator.templates.InterfaceTemplate
import io.joynr.generator.templates.util.NamingUtil
import io.joynr.generator.util.JoynrJavaGeneratorExtensions
import io.joynr.generator.util.TemplateBase

class InterfaceProxyTemplate extends InterfaceTemplate {
	@Inject extension JoynrJavaGeneratorExtensions
	@Inject extension NamingUtil
	@Inject extension TemplateBase

	override generate() {
		val interfaceName =  francaIntf.joynrName
		val className = francaIntf.proxyClassName
		val asyncClassName = interfaceName + "Async"
		val syncClassName = interfaceName + "Sync"
		val subscriptionClassName = interfaceName + "SubscriptionInterface"
		val broadcastClassName = interfaceName + "BroadcastInterface"
		val packagePath = getPackagePathWithJoynrPrefix(francaIntf, ".")
		'''

		«warning()»
		package «packagePath»;

		import io.joynr.JoynrVersion;

		@JoynrVersion(major = «majorVersion», minor = «minorVersion»)
		public interface «className» extends «asyncClassName», «syncClassName»«IF francaIntf.attributes.size>0», «subscriptionClassName»«ENDIF»«IF francaIntf.broadcasts.size>0», «broadcastClassName»«ENDIF» {
			public static String INTERFACE_NAME = "«getPackagePathWithoutJoynrPrefix(francaIntf, "/")»/«interfaceName»";
		}
		'''
	}

}