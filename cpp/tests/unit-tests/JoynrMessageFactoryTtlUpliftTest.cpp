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
#include <chrono>
#include <cstdint>
#include <string>

#include <gtest/gtest.h>

#include "joynr/BroadcastSubscriptionRequest.h"
#include "joynr/DispatcherUtils.h"
#include "joynr/JoynrMessageFactory.h"
#include "joynr/MessagingQos.h"
#include "joynr/MulticastPublication.h"
#include "joynr/MulticastSubscriptionRequest.h"
#include "joynr/Reply.h"
#include "joynr/Request.h"
#include "joynr/SubscriptionPublication.h"
#include "joynr/SubscriptionReply.h"
#include "joynr/SubscriptionRequest.h"
#include "joynr/SubscriptionStop.h"

using namespace joynr;

class JoynrMessageFactoryTtlUpliftTest : public ::testing::Test
{
public:
    JoynrMessageFactoryTtlUpliftTest()
            : messageFactory(0),
              senderID("senderId"),
              receiverID("receiverID"),
              ttl(1000),
              ttlUplift(10000),
              upliftedTtl(ttl + ttlUplift),
              messagingQos(),
              factoryWithTtlUplift(ttlUplift)
    {
                  messagingQos.setTtl(ttl);
    }

    void checkMessageExpiryDate(const JoynrMessage& message, const std::int64_t expectedTtl);

protected:
    ADD_LOGGER(JoynrMessageFactoryTtlUpliftTest);
    JoynrMessageFactory messageFactory;
    std::string senderID;
    std::string receiverID;

    const std::int64_t ttl;
    const std::uint64_t ttlUplift;
    const std::int64_t upliftedTtl;
    MessagingQos messagingQos;
    JoynrMessageFactory factoryWithTtlUplift;
};

INIT_LOGGER(JoynrMessageFactoryTtlUpliftTest);

void JoynrMessageFactoryTtlUpliftTest::checkMessageExpiryDate(const JoynrMessage& message, const std::int64_t expectedTtl) {
    const std::int64_t tolerance = 50;
    std::int64_t actualTtl = std::chrono::duration_cast<std::chrono::milliseconds>(
                message.getHeaderExpiryDate() - std::chrono::system_clock::now()).count();
    std::int64_t diff = expectedTtl - actualTtl;
    EXPECT_GE(diff, 0);
    EXPECT_LE(std::abs(diff), tolerance) << "ttl from expiryDate "
                                            + std::to_string(actualTtl) + "ms differs "
                                            + std::to_string(diff) + "ms (more than "
                                            + std::to_string(tolerance) + "ms) from the expected ttl "
                                            + std::to_string(expectedTtl);
}

TEST_F(JoynrMessageFactoryTtlUpliftTest, testDefaultTtlUplift)
{
    Request request;
    JoynrMessage message = messageFactory.createRequest(senderID, receiverID, messagingQos, request);

    checkMessageExpiryDate(message, ttl);
}

TEST_F(JoynrMessageFactoryTtlUpliftTest, testTtlUplift_Request)
{
    Request request;
    JoynrMessage message = factoryWithTtlUplift.createRequest(senderID, receiverID, messagingQos, request);

    checkMessageExpiryDate(message, upliftedTtl);
}

TEST_F(JoynrMessageFactoryTtlUpliftTest, testTtlUplift_Reply_noUplift)
{
    Reply reply;
    JoynrMessage message = factoryWithTtlUplift.createReply(senderID, receiverID, messagingQos, reply);

    checkMessageExpiryDate(message, ttl);
}

TEST_F(JoynrMessageFactoryTtlUpliftTest, testTtlUplift_OneWayRequest)
{
    OneWayRequest oneWayRequest;
    JoynrMessage message = factoryWithTtlUplift.createOneWayRequest(senderID, receiverID, messagingQos, oneWayRequest);

    checkMessageExpiryDate(message, upliftedTtl);
}

TEST_F(JoynrMessageFactoryTtlUpliftTest, testTtlUplift_MulticastPublication)
{
    MulticastPublication publication;
    JoynrMessage message = factoryWithTtlUplift.createMulticastPublication(senderID, messagingQos, publication);

    checkMessageExpiryDate(message, upliftedTtl);
}

TEST_F(JoynrMessageFactoryTtlUpliftTest, testTtlUplift_SubscriptionPublication)
{
    SubscriptionPublication subscriptionPublication;
    JoynrMessage message = factoryWithTtlUplift.createSubscriptionPublication(senderID, receiverID, messagingQos, subscriptionPublication);

    checkMessageExpiryDate(message, upliftedTtl);
}

TEST_F(JoynrMessageFactoryTtlUpliftTest, testTtlUplift_SubscriptionRequest)
{
    SubscriptionRequest request;
    JoynrMessage message = factoryWithTtlUplift.createSubscriptionRequest(senderID, receiverID, messagingQos, request);

    checkMessageExpiryDate(message, upliftedTtl);
}

TEST_F(JoynrMessageFactoryTtlUpliftTest, testTtlUplift_MulticastSubscriptionRequest)
{
    MulticastSubscriptionRequest request;
    JoynrMessage message = factoryWithTtlUplift.createMulticastSubscriptionRequest(senderID, receiverID, messagingQos, request);

    checkMessageExpiryDate(message, upliftedTtl);
}

TEST_F(JoynrMessageFactoryTtlUpliftTest, testTtlUplift_BroadcastSubscriptionRequest)
{
    BroadcastSubscriptionRequest request;
    JoynrMessage message = factoryWithTtlUplift.createBroadcastSubscriptionRequest(senderID, receiverID, messagingQos, request);

    checkMessageExpiryDate(message, upliftedTtl);
}

TEST_F(JoynrMessageFactoryTtlUpliftTest, testTtlUplift_SubscriptionReply_noUplift)
{
    SubscriptionReply reply;
    JoynrMessage message = factoryWithTtlUplift.createSubscriptionReply(senderID, receiverID, messagingQos, reply);

    checkMessageExpiryDate(message, ttl);
}

TEST_F(JoynrMessageFactoryTtlUpliftTest, testTtlUplift_SubscriptionStop)
{
    SubscriptionStop subscriptionStop;
    JoynrMessage message = factoryWithTtlUplift.createSubscriptionStop(senderID, receiverID, messagingQos, subscriptionStop);

    checkMessageExpiryDate(message, upliftedTtl);
}

TEST_F(JoynrMessageFactoryTtlUpliftTest, testTtlUpliftWithLargeTtl)
{
    const JoynrTimePoint expectedTimePoint = DispatcherUtils::getMaxAbsoluteTime();
    Request request;

    std::int64_t ttl;
    MessagingQos messagingQos;
    JoynrMessage message;
    JoynrTimePoint timePoint;

    ttl = INT64_MAX;
    messagingQos.setTtl(ttl);
    message = factoryWithTtlUplift.createRequest(senderID, receiverID, messagingQos, request);
    timePoint = message.getHeaderExpiryDate();
    EXPECT_EQ(expectedTimePoint, timePoint) << "expected timepoint: "
                                               + std::to_string(expectedTimePoint.time_since_epoch().count())
                                               + " actual: "
                                               + std::to_string(timePoint.time_since_epoch().count());

    // TODO uncomment failing tests
    // after overflow checks in DispatcherUtils.convertTtlToAbsoluteTime are fixed

//    ttl = DispatcherUtils::getMaxAbsoluteTime().time_since_epoch().count();
//    messagingQos.setTtl(ttl);
//    message = factoryWithTtlUplift.createRequest(senderID, receiverID, messagingQos, request);
//    timePoint = message.getHeaderExpiryDate();
//    EXPECT_EQ(expectedTimePoint, timePoint);

//    ttl = DispatcherUtils::getMaxAbsoluteTime().time_since_epoch().count() - ttlUplift;
//    messagingQos.setTtl(ttl);
//    message = factoryWithTtlUplift.createRequest(senderID, receiverID, messagingQos, request);
//    timePoint = message.getHeaderExpiryDate();
//    EXPECT_EQ(expectedTimePoint, timePoint);

    std::int64_t now = std::chrono::time_point_cast<std::chrono::milliseconds>(
                std::chrono::system_clock::now()).time_since_epoch().count();
    ttl = DispatcherUtils::getMaxAbsoluteTime().time_since_epoch().count()
            - ttlUplift
            - now;
    messagingQos.setTtl(ttl);
    message = factoryWithTtlUplift.createRequest(senderID, receiverID, messagingQos, request);
//    timePoint = message.getHeaderExpiryDate();
//    EXPECT_EQ(expectedTimePoint, timePoint) << "expected timepoint: "
//                                               + std::to_string(expectedTimePoint.time_since_epoch().count())
//                                               + " actual: "
//                                               + std::to_string(timePoint.time_since_epoch().count());
    checkMessageExpiryDate(message, expectedTimePoint.time_since_epoch().count() - now);

//    ttl = DispatcherUtils::getMaxAbsoluteTime().time_since_epoch().count()
//            - ttlUplift
//            - std::chrono::time_point_cast<std::chrono::milliseconds>(
//                std::chrono::system_clock::now()).time_since_epoch().count() + 1;
//    messagingQos.setTtl(ttl);
//    message = factoryWithTtlUplift.createRequest(senderID, receiverID, messagingQos, request);
//    timePoint = message.getHeaderExpiryDate();
//    EXPECT_EQ(expectedTimePoint, timePoint);
}
