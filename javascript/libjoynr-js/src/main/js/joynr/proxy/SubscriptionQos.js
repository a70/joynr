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

define(
        "joynr/proxy/SubscriptionQos",
        [
            "joynr/util/UtilInternal",
            "joynr/system/LoggerFactory"
        ],
        function(Util, LoggerFactory) {

            var defaultSettings;

            /**
             * @classdesc
             * Base class representing the subscription quality of service settings.
             * This class stores quality of service settings used for subscriptions to
             * <b>attributes and broadcasts</b> in generated proxy objects.<br/>
             * Subclasses are {@link PeriodicSubscriptionQos},
             * {@link OnChangeSubscriptionQos}, and
             * {@link OnChangeWithKeepAliveSubscriptionQos}.<br/>
             * The provider will send notifications until the expiry date is reached. The
             * subscription will automatically expire after the expiry date is reached.
             *
             * @summary
             * Constructor of SubscriptionQos object used for subscriptions to
             * <b>attributes and events</b> in generated proxy objects.
             *
             * @constructor
             * @name SubscriptionQos
             *
             * @param {Object}
             *            [settings] the settings object for the constructor call
             * @param {Number}
             *            [settings.expiryDate] Deprecated parameter. Use settings.expiryDateMs instead
             * @param {Number}
             *            [settings.expiryDateMs=0] The expiry date is the end date of the
             *            subscription. This value is provided in milliseconds
             *            (since 1970-01-01T00:00:00.000).<br/>
             *            <br/>
             *            <b>Minimum and Default Values:</b>
             *            <ul>
             *              <li>minimum value: {@link SubscriptionQos.MIN_EXPIRY_MS}</li>
             *              <li>default value: {@link SubscriptionQos.NO_EXPIRY_DATE}</li>
             *            </ul>
             * @param {Number}
             *            [settings.publicationTtl=10 000] Time to live for publication
             *            messages.<br/>
             *            <br/>
             *            If a notification message can not be delivered within its time
             *            to live, it will be deleted from the system. This value is
             *            provided in milliseconds.<br/>
             *            <br/>
             *            <b>Minimum and Default Values:</b>
             *            <ul>
             *              <li>minimum value: {@link SubscriptionQos.MIN_PUBLICATION_TTL}</li>
             *              <li>default value: {@link SubscriptionQos.DEFAULT_PUBLICATION_TTL}</li>
             *            </ul>
             *
             * @returns {SubscriptionQos}
             *            a subscription Qos Object for subscriptions on <b>attributes
             *            and events</b>
             */
            function SubscriptionQos(settings) {
                if (!(this instanceof SubscriptionQos)) {
                    // in case someone calls constructor without new keyword (e.g. var c
                    // = Constructor({..}))
                    return new SubscriptionQos(settings);
                }

                var log = LoggerFactory.getLogger("joynr.proxy.SubscriptionQos");

                /**
                 * Used for serialization.
                 * @name SubscriptionQos#_typeName
                 * @type String
                 */
                Util.objectDefineProperty(this, "_typeName", "joynr.SubscriptionQos");
                Util.checkPropertyIfDefined(settings, "Object", "settings");
                if (settings) {
                    if (settings.expiryDate !== undefined) {
                        log
                                .warn("SubscriptionQos has been invoked with deprecated settings member \"expiryDate\". "
                                    + "By 2017-01-01, the expiry date can only be specified with member \"expiryDateMs\".");
                        settings.expiryDateMs = settings.expiryDate;
                        settings.expiryDate = undefined;
                    }
                    Util.checkPropertyIfDefined(
                            settings.expiryDateMs,
                            "Number",
                            "settings.expiryDateMs");
                    Util.checkPropertyIfDefined(
                            settings.publicationTtl,
                            "Number",
                            "settings.publicationTtl");
                }

                /**
                 * See [constructor description]{@link SubscriptionQos}.
                 * @name SubscriptionQos#expiryDateMs
                 * @type Number
                 */
                /**
                 * See [constructor description]{@link SubscriptionQos}.
                 * @name SubscriptionQos#publicationTtl
                 * @type Number
                 */
                Util.extend(this, defaultSettings, settings);
                if (this.publicationTtl < SubscriptionQos.MIN_PUBLICATION_TTL) {
                    throw new Error("Wrong publicationttl with value "
                        + this.publicationTtl
                        + ": it shall be higher than "
                        + SubscriptionQos.MIN_PUBLICATION_TTL);
                }

                if (this.expiryDateMs < SubscriptionQos.MIN_EXPIRY_MS) {
                    throw new Error("Wrong expiry date with value "
                        + this.expiryDateMs
                        + ": it shall be higher than "
                        + SubscriptionQos.MIN_EXPIRY_MS);
                }
            }

            /**
             * Minimal value for [publicationTtl]{@link SubscriptionQos#publicationTtl}.
             * See [constructor description]{@link SubscriptionQos}.
             *
             * @name SubscriptionQos.MIN_PUBLICATION_TTL
             * @type Number
             * @default 100
             * @static
             * @readonly
             */
            SubscriptionQos.MIN_PUBLICATION_TTL = 100;
            /**
             * Minimal value for [expiryDateMs]{@link SubscriptionQos#expiryDateMs} in milliseconds
             * (0 secs). See [constructor description]{@link SubscriptionQos}.
             *
             * @name SubscriptionQos.MIN_EXPIRY_MS
             * @type Number
             * @default 0
             * @static
             * @readonly
             */
            SubscriptionQos.MIN_EXPIRY_MS = 0;
            /**
             * Default value for [expiryDateMs]{@link SubscriptionQos#expiryDateMs} in milliseconds
             * (no expiry date). See [constructor description]{@link SubscriptionQos}.
             *
             * @name SubscriptionQos.NO_EXPIRY_DATE
             * @type Number
             * @default 0
             * @static
             * @readonly
             */
            SubscriptionQos.NO_EXPIRY_DATE = 0;
            /**
             * @name SubscriptionQos.NO_EXPIRY_DATE_TTL
             * @type Number
             * @default Util.getMaxLongValue()
             * @static
             * @readonly
             */
            SubscriptionQos.NO_EXPIRY_DATE_TTL = Util.getMaxLongValue();
            /**
             * Default value for [publicationTtl]{@link SubscriptionQos#publicationTtl} in
             * milliseconds (10 secs). See [constructor description]{@link SubscriptionQos}.
             *
             * @name SubscriptionQos.DEFAULT_PUBLICATION_TTL
             * @type Number
             * @default 10 000
             * @static
             * @readonly
             */
            SubscriptionQos.DEFAULT_PUBLICATION_TTL = 10000;

            defaultSettings = {
                expiryDateMs : SubscriptionQos.NO_EXPIRY_DATE,
                publicationTtl : SubscriptionQos.DEFAULT_PUBLICATION_TTL
            };

            return SubscriptionQos;

        });
