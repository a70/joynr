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
#ifndef SYSTEMSERVICESSETTINGS_H
#define SYSTEMSERVICESSETTINGS_H

#include "joynr/JoynrCommonExport.h"
#include "joynr/Logger.h"

#include <string>

namespace joynr
{

class Settings;

class JOYNRCOMMON_EXPORT SystemServicesSettings
{
public:
    explicit SystemServicesSettings(Settings& settings);
    SystemServicesSettings(const SystemServicesSettings&) = default;
    SystemServicesSettings(SystemServicesSettings&&) = default;

    ~SystemServicesSettings() = default;

    static const std::string& SETTING_DOMAIN();
    static const std::string& SETTING_CC_ROUTINGPROVIDER_AUTHENTICATIONTOKEN();
    static const std::string& SETTING_CC_ROUTINGPROVIDER_PARTICIPANTID();
    static const std::string& SETTING_CC_DISCOVERYPROVIDER_AUTHENTICATIONTOKEN();
    static const std::string& SETTING_CC_DISCOVERYPROVIDER_PARTICIPANTID();

    static const std::string& DEFAULT_SYSTEM_SERVICES_SETTINGS_FILENAME();

    std::string getDomain() const;
    void setJoynrSystemServicesDomain(const std::string& systemServicesDomain);
    std::string getCcRoutingProviderAuthenticationToken() const;
    void setCcRoutingProviderAuthenticationToken(const std::string& authenticationToken);
    std::string getCcRoutingProviderParticipantId() const;
    void setCcRoutingProviderParticipantId(const std::string& participantId);
    std::string getCcDiscoveryProviderAuthenticationToken() const;
    void setCcDiscoveryProviderAuthenticationToken(const std::string& authenticationToken);
    std::string getCcDiscoveryProviderParticipantId() const;
    void setCcDiscoveryProviderParticipantId(const std::string& participantId);

    bool contains(const std::string& key) const;

    void printSettings() const;

private:
    void operator=(const SystemServicesSettings& other);

    Settings& settings;
    ADD_LOGGER(SystemServicesSettings);
    void checkSettings() const;
};

} // namespace joynr
#endif // SYSTEMSERVICESSETTINGS_H
