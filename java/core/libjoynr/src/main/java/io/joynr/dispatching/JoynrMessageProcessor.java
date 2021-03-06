package io.joynr.dispatching;

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

import joynr.JoynrMessage;

/**
 * Implementations of this interface are used by the {@link JoynrMessageFactory} in order
 * to allow applications to provide logic which processes messages further after they've
 * been created, but before they've been signed and sent.
 */
public interface JoynrMessageProcessor {

    /**
     * This method is passed in a joynr message, which it can then process, e.g. add or change headers,
     * encrypt the payload, etc., and then returns a new message which is then used for further processing
     * and transmitting.
     *
     * @param joynrMessage the message to process.
     * @return the message which should be used.
     */
    JoynrMessage process(JoynrMessage joynrMessage);
}
