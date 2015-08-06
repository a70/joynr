package io.joynr.android.robolectric;

/*
 * #%L
 * %%
 * Copyright (C) 2011 - 2013 BMW Car IT GmbH
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

import com.google.inject.Injector;
import com.google.inject.Module;
import io.joynr.integration.AbstractMessagingIntegrationTest;
import io.joynr.joynrandroidruntime.messaging.AndroidLongPollingMessagingModule;
import io.joynr.runtime.JoynrInjectorFactory;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Properties;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = "./src/test/AndroidManifest.xml")
public class MessagingIntegrationTest extends AbstractMessagingIntegrationTest {

    @Before
    public void robolectricSetup() throws Exception {

        // Uncomment to log the verbose android logs to stdout
        //ShadowLog.stream = System.out;
    }

    @Override
    public Injector createInjector(Properties joynrConfig, Module... modules) {

        // Add an Android long polling module to the array of modules
        Module[] androidModules = new Module[modules.length + 1];
        System.arraycopy(modules, 0, androidModules, 0, modules.length);
        androidModules[modules.length] = new AndroidLongPollingMessagingModule();

        return new JoynrInjectorFactory(joynrConfig, androidModules).getInjector();
    }
}