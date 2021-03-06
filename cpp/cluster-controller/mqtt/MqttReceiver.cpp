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
#include "MqttReceiver.h"

#include <chrono>

#include "joynr/system/RoutingTypes/MqttAddress.h"
#include "joynr/serializer/Serializer.h"

namespace joynr
{

INIT_LOGGER(MqttReceiver);

MqttReceiver::MqttReceiver(const MessagingSettings& settings,
                           const std::string& channelIdForMqttTopic,
                           const std::string& receiverId)
        : channelIdForMqttTopic(channelIdForMqttTopic),
          globalClusterControllerAddress(),
          receiverId(receiverId),
          channelCreated(false),
          mosquittoSubscriber(settings, globalClusterControllerAddress)
{
    std::string brokerUri =
            "tcp://" + settings.getBrokerUrl().getBrokerChannelsBaseUrl().getHost() + ":" +
            std::to_string(settings.getBrokerUrl().getBrokerChannelsBaseUrl().getPort());
    system::RoutingTypes::MqttAddress receiveMqttAddress(brokerUri, channelIdForMqttTopic);
    globalClusterControllerAddress = joynr::serializer::serializeToJson(receiveMqttAddress);

    mosquittoSubscriber.registerChannelId(channelIdForMqttTopic);
}

MqttReceiver::~MqttReceiver()
{
    mosquittoSubscriber.stop();
}

void MqttReceiver::updateSettings()
{
}

void MqttReceiver::startReceiveQueue()
{
    JOYNR_LOG_DEBUG(logger, "startReceiveQueue");

    mosquittoSubscriber.start();
}

void MqttReceiver::stopReceiveQueue()
{
    JOYNR_LOG_DEBUG(logger, "stopReceiveQueue");

    mosquittoSubscriber.stop();
}

const std::string& MqttReceiver::getGlobalClusterControllerAddress() const
{
    return globalClusterControllerAddress;
}

bool MqttReceiver::tryToDeleteChannel()
{
    return true;
}

bool MqttReceiver::isConnected()
{
    return mosquittoSubscriber.isSubscribedToChannelTopic();
}

void MqttReceiver::registerReceiveCallback(
        std::function<void(const std::string&)> onTextMessageReceived)
{
    mosquittoSubscriber.registerReceiveCallback(onTextMessageReceived);
}

void MqttReceiver::subscribeToTopic(const std::string& topic)
{
    mosquittoSubscriber.subscribeToTopic(topic);
}

void MqttReceiver::unsubscribeFromTopic(const std::string& topic)
{
    mosquittoSubscriber.unsubscribeFromTopic(topic);
}

} // namespace joynr
