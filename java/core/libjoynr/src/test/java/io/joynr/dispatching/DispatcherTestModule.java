package io.joynr.dispatching;

/*
 * #%L
 * %%
 * Copyright (C) 2011 - 2015 BMW Car IT GmbH
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

import static io.joynr.runtime.JoynrInjectionConstants.JOYNR_SCHEDULER_CLEANUP;
import io.joynr.dispatching.rpc.RpcUtils;
import io.joynr.dispatching.subscription.PublicationManager;
import io.joynr.dispatching.subscription.PublicationManagerImpl;
import io.joynr.dispatching.subscription.SubscriptionManager;
import io.joynr.dispatching.subscription.SubscriptionManagerImpl;
import io.joynr.messaging.IMessageReceivers;
import io.joynr.messaging.MessageReceiver;
import io.joynr.messaging.MessageReceivers;
import io.joynr.messaging.MessageSender;
import io.joynr.messaging.routing.MessageRouter;
import io.joynr.messaging.routing.MessageRouterImpl;
import io.joynr.proxy.JoynrMessagingConnectorFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

import joynr.Request;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

public class DispatcherTestModule extends AbstractModule {

    @Override
    protected void configure() {

        bind(MessageSender.class).to(MessageSenderReceiverMock.class);
        bind(IMessageReceivers.class).to(MessageReceivers.class).asEagerSingleton();
        bind(MessageReceiver.class).to(MessageSenderReceiverMock.class);
        bind(RequestReplySender.class).to(RequestReplySenderImpl.class);
        bind(MessageRouter.class).to(MessageRouterImpl.class);
        bind(RequestReplyDispatcher.class).to(RequestReplyDispatcherImpl.class);
        bind(Dispatcher.class).to(DispatcherImpl.class);
        bind(SubscriptionManager.class).to(SubscriptionManagerImpl.class);
        bind(PublicationManager.class).to(PublicationManagerImpl.class);

        requestStaticInjection(RpcUtils.class, Request.class, JoynrMessagingConnectorFactory.class);

        ThreadFactory namedThreadFactory = new ThreadFactoryBuilder().setNameFormat("joynr.Cleanup-%d").build();
        ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor(namedThreadFactory);
        bind(ScheduledExecutorService.class).annotatedWith(Names.named(JOYNR_SCHEDULER_CLEANUP))
                                            .toInstance(cleanupExecutor);
    }

}