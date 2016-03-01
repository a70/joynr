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
#include "RobustnessTestProvider.h"
#include <thread>
#include "joynr/DispatcherUtils.h"

namespace joynr
{

using joynr::JoynrTimePoint;
using joynr::DispatcherUtils;

INIT_LOGGER(RobustnessTestProvider);

RobustnessTestProvider::RobustnessTestProvider()
        : joynr::tests::robustness::DefaultTestInterfaceProvider()
{
    // Initialize the quality of service settings
    // Set the priority so that the consumer application always uses the most recently
    // started provider
    std::chrono::milliseconds millisSinceEpoch =
            std::chrono::duration_cast<std::chrono::milliseconds>(
                    std::chrono::system_clock::now().time_since_epoch());
    providerQos.setPriority(millisSinceEpoch.count());
    stopBroadcastWithSingleStringParameter = false;
}

void RobustnessTestProvider::methodWithStringParameters(
        const std::string& stringArg,
        std::function<void(const std::string& stringOut)> onSuccess,
        std::function<void(const joynr::exceptions::ProviderRuntimeException&)> onError)
{
    std::ignore = onError;
    JOYNR_LOG_INFO(logger, "***********************");
    JOYNR_LOG_INFO(logger, "* methodWithStringParameters called. stringArg: {}", stringArg);
    JOYNR_LOG_INFO(logger, "***********************");
    std::string stringOut = "received stringArg: " + stringArg;
    onSuccess(stringOut);
}

void RobustnessTestProvider::startFireBroadcastWithSingleStringParameter(
        std::function<void()> onSuccess,
        std::function<void(const joynr::exceptions::ProviderRuntimeException&)> onError)
{
    std::ignore = onError;
    auto stringOut = std::make_shared<std::string>("BroadcastWithSingleStringParameter_stringOut");
    JOYNR_LOG_INFO(logger, "***********************");
    JOYNR_LOG_INFO(logger,
                   "* startFireBroadcastWithSingleStringParameter called. stringOut: {}",
                   *stringOut);
    JOYNR_LOG_INFO(logger, "***********************");

    stopBroadcastWithSingleStringParameter = false;
    std::int64_t period_ms = 250;
    std::int64_t validity_ms = 60000;
    broadcastThread =
            std::thread(&RobustnessTestProvider::fireBroadcastWithSingleStringParameterInternal,
                        this,
                        period_ms,
                        validity_ms,
                        stringOut);
    onSuccess();
}

void RobustnessTestProvider::stopFireBroadcastWithSingleStringParameter(
        std::function<void()> onSuccess,
        std::function<void(const joynr::exceptions::ProviderRuntimeException&)> onError)
{
    std::ignore = onError;
    std::string stringOut = "BroadcastWithSingleStringParameter_stringOut";
    JOYNR_LOG_INFO(logger, "***********************");
    JOYNR_LOG_INFO(logger, "* stopFireBroadcastWithSingleStringParameter called.");
    JOYNR_LOG_INFO(logger, "***********************");

    stopBroadcastWithSingleStringParameter = true;
    onSuccess();
}

void RobustnessTestProvider::fireBroadcastWithSingleStringParameterInternal(
        std::int64_t period_ms,
        std::int64_t validity_ms,
        std::shared_ptr<std::string> stringOut)
{
    JoynrTimePoint decayTime = DispatcherUtils::convertTtlToAbsoluteTime(validity_ms);
    while (std::chrono::system_clock::now() < decayTime &&
           !stopBroadcastWithSingleStringParameter) {
        fireBroadcastWithSingleStringParameter(*stringOut);
        std::this_thread::sleep_for(std::chrono::milliseconds(period_ms));
    }
}

} // namespace joynr