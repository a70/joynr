/*
 * #%L
 * %%
 * Copyright (C) 2011 - 2017 BMW Car IT GmbH
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

#ifndef CHILDMESSAGEROUTER_H
#define CHILDMESSAGEROUTER_H

#include <memory>
#include <mutex>
#include <string>
#include <unordered_set>

#include "joynr/AbstractMessageRouter.h"
#include "joynr/JoynrExport.h"
#include "joynr/PrivateCopyAssign.h"

#include "joynr/Logger.h"
#include "joynr/MessageQueue.h"

namespace boost
{
namespace asio
{
class io_service;
} // namespace asio
} // namespace boost

namespace joynr
{

class IMessaging;
class IMessagingStubFactory;
class IMulticastAddressCalculator;
class JoynrMessage;
class SteadyTimer;
class ThreadPoolDelayedScheduler;

namespace system
{
class Address;
class RoutingProxy;
} // namespace system

/**
  * Class MessageRouter receives incoming JoynrMessages on the ClusterController
  * and forwards them either to a remote ClusterController or to a LibJoynr on the machine.
  *
  *  1 extracts the destination participant ID and looks up the EndpointAddress in the
  *MessagingEndpointDirectory
  *  2 creates a <Middleware>MessagingStub by calling MessagingStubFactory.create(EndpointAddress
  *addr)
  *  3 forwards the message using the <Middleware>MessagingStub.send(JoynrMessage msg)
  *
  *  In sending, a ThreadPool of default size 6 is used with a 500ms default retry interval.
  */

class JOYNR_EXPORT LibJoynrMessageRouter : public joynr::AbstractMessageRouter
{
public:
    LibJoynrMessageRouter(
            std::shared_ptr<const joynr::system::RoutingTypes::Address> incomingAddress,
            std::shared_ptr<IMessagingStubFactory> messagingStubFactory,
            boost::asio::io_service& ioService,
            std::unique_ptr<IMulticastAddressCalculator> addressCalculator,
            int maxThreads = 1,
            std::unique_ptr<MessageQueue> messageQueue = std::make_unique<MessageQueue>());

    ~LibJoynrMessageRouter() override;

    /*
     * Implement methods from IMessageRouter
     */
    void route(const JoynrMessage& message, std::uint32_t tryCount = 0) final;

    void addNextHop(
            const std::string& participantId,
            const std::shared_ptr<const joynr::system::RoutingTypes::Address>& inprocessAddress,
            std::function<void()> onSuccess = nullptr) final;

    void removeNextHop(
            const std::string& participantId,
            std::function<void()> onSuccess,
            std::function<void(const joynr::exceptions::ProviderRuntimeException&)> onError) final;

    void addMulticastReceiver(
            const std::string& multicastId,
            const std::string& subscriberParticipantId,
            const std::string& providerParticipantId,
            std::function<void()> onSuccess,
            std::function<void(const joynr::exceptions::ProviderRuntimeException&)> onError) final;

    void removeMulticastReceiver(
            const std::string& multicastId,
            const std::string& subscriberParticipantId,
            const std::string& providerParticipantId,
            std::function<void()> onSuccess,
            std::function<void(const joynr::exceptions::ProviderRuntimeException&)> onError) final;

    /*
     * Method specific to LibJoynrMessageRouter
     */
    void setParentRouter(std::unique_ptr<joynr::system::RoutingProxy> parentRouter,
                         std::shared_ptr<const joynr::system::RoutingTypes::Address> parentAddress,
                         std::string parentParticipantId);

    friend class MessageRunnable;

private:
    DISALLOW_COPY_AND_ASSIGN(LibJoynrMessageRouter);
    ADD_LOGGER(LibJoynrMessageRouter);

    bool isParentMessageRouterSet();
    void addNextHopToParent(std::string participantId,
                            std::function<void(void)> callbackFct = nullptr,
                            std::function<void(const joynr::exceptions::ProviderRuntimeException&)>
                                    onError = nullptr);

    std::unique_ptr<joynr::system::RoutingProxy> parentRouter;
    std::shared_ptr<const joynr::system::RoutingTypes::Address> parentAddress;
    std::shared_ptr<const joynr::system::RoutingTypes::Address> incomingAddress;
    std::unordered_set<std::string> runningParentResolves;
    mutable std::mutex parentResolveMutex;

    void removeRunningParentResolvers(const std::string& destinationPartId);
};

} // namespace joynr
#endif // CHILDMESSAGEROUTER_H
