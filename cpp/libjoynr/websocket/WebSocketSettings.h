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
#ifndef WEBSOCKETSETTINGS_H
#define WEBSOCKETSETTINGS_H

#include <chrono>
#include <string>

#include "joynr/Logger.h"

namespace joynr
{

class Settings;

namespace system
{
namespace RoutingTypes
{
class WebSocketAddress;
} // namespace RoutingTypes
} // namespace system

class WebSocketSettings
{
public:
    static const std::string& SETTING_CC_MESSAGING_URL();
    static const std::string& SETTING_RECONNECT_SLEEP_TIME_MS();

    static const std::string& DEFAULT_WEBSOCKET_SETTINGS_FILENAME();

    explicit WebSocketSettings(Settings& settings);
    WebSocketSettings(const WebSocketSettings&) = default;
    WebSocketSettings(WebSocketSettings&&) = default;

    ~WebSocketSettings() = default;

    std::string getClusterControllerMessagingUrl() const;
    void setClusterControllerMessagingUrl(const std::string& url);
    system::RoutingTypes::WebSocketAddress createClusterControllerMessagingAddress() const;

    std::chrono::milliseconds getReconnectSleepTimeMs() const;
    void setReconnectSleepTimeMs(const std::chrono::milliseconds reconnectSleepTimeMs);

    void printSettings() const;

    bool contains(const std::string& key) const;

private:
    void operator=(const WebSocketSettings& other);

    Settings& settings;
    ADD_LOGGER(WebSocketSettings);
    void checkSettings() const;
};

} // namespace joynr
#endif // WEBSOCKETSETTINGS_H
