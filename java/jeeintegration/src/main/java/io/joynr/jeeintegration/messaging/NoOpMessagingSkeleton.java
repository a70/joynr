package io.joynr.jeeintegration.messaging;

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

import com.google.inject.Inject;

import io.joynr.messaging.FailureAction;
import io.joynr.messaging.IMessagingSkeleton;
import io.joynr.messaging.mqtt.JoynrMqttClient;
import io.joynr.messaging.mqtt.MqttClientFactory;
import joynr.JoynrMessage;

/**
 * Because the messaging stub will refuse to send a message via MQTT unless a messaging skeleton has been registered
 * for the MqttAddress type, we bind a dummy implementation in this module which simply does nothing (no operation -
 * NoOp).
 */
public class NoOpMessagingSkeleton implements IMessagingSkeleton {

    private MqttClientFactory mqttClientFactory;
    private JoynrMqttClient mqttClient;

    @Inject
    public NoOpMessagingSkeleton(MqttClientFactory mqttClientFactory) {
        this.mqttClientFactory = mqttClientFactory;
    }

    @Override
    public void transmit(JoynrMessage message, FailureAction failureAction) {
    }

    @Override
    public void transmit(String serializedMessage, FailureAction failureAction) {
    }

    @Override
    public void init() {
        mqttClient = mqttClientFactory.create();
        mqttClient.setMessageListener(this);
        mqttClient.start();
    }

    @Override
    public void shutdown() {
        mqttClient.shutdown();
    }

}