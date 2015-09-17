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

define("joynr/messaging/browser/BrowserMessagingStub", [ "joynr/util/Util"
], function(Util) {

    /**
     * @name BrowserMessagingStub
     * @constructor
     *
     * @param {Object} settings
     * @param {String} [settings.windowId] the destination windowId to send the messages to, defaults to defaultWindowId of master tab
     * @param {WebMessagingStub} settings.webMessagingStub an initialized sender that has the default window already set
     */
    function BrowserMessagingStub(settings) {
        Util.checkProperty(settings, "Object", "settings");
        Util.checkPropertyIfDefined(settings.windowId, "String", "settings.windowId");
        Util.checkProperty(
                settings.webMessagingStub,
                "WebMessagingStub",
                "settings.webMessagingStub");

        /**
         * @name BrowserMessagingStub#transmit
         * @function
         *
         * @param {JoynrMessage} joynrMessage the joynr message to transmit
         */
        this.transmit = function transmit(joynrMessage) {
            Util.checkProperty(joynrMessage, "JoynrMessage", "joynrMessage");

            return settings.webMessagingStub.transmit({
                windowId : settings.windowId,
                message : joynrMessage
            });
        };

    }

    return BrowserMessagingStub;

});