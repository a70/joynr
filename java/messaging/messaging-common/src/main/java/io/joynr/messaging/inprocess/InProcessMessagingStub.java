package io.joynr.messaging.inprocess;

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

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.joynr.messaging.FailureAction;
import io.joynr.messaging.IMessaging;
import joynr.JoynrMessage;

public class InProcessMessagingStub implements IMessaging {
    private static final Logger LOG = LoggerFactory.getLogger(InProcessMessagingStub.class);

    private final InProcessMessagingSkeleton skeleton;

    @Inject
    public InProcessMessagingStub(InProcessMessagingSkeleton skeleton) {
        this.skeleton = skeleton;
    }

    @Override
    public void transmit(JoynrMessage message, FailureAction failureAction) {
        LOG.trace(">>> OUTGOING >>> {}", message.toLogMessage());
        skeleton.transmit(message, failureAction);
    }

    @Override
    public void transmit(String serializedMessage, FailureAction failureAction) {
        LOG.trace(">>> OUTGOING >>> {}", serializedMessage);
        throw new IllegalStateException("InProcess messaging should not send serialized messages");
    }

}
