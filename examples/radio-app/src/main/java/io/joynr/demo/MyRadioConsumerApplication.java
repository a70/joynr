package io.joynr.demo;

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

import io.joynr.arbitration.ArbitrationStrategy;
import io.joynr.arbitration.DiscoveryQos;
import io.joynr.exceptions.JoynrArbitrationException;
import io.joynr.exceptions.JoynrCommunicationException;
import io.joynr.messaging.MessagingPropertyKeys;
import io.joynr.messaging.MessagingQos;
import io.joynr.proxy.ProxyBuilder;
import io.joynr.pubsub.subscription.AttributeSubscriptionAdapter;
import io.joynr.runtime.AbstractJoynrApplication;
import io.joynr.runtime.JoynrApplication;
import io.joynr.runtime.JoynrApplicationModule;
import io.joynr.runtime.JoynrInjectorFactory;
import io.joynr.proxy.Future;
import io.joynr.proxy.Callback;
import io.joynr.exceptions.JoynrRuntimeException;

import java.io.IOException;
import java.util.Properties;

import jline.console.ConsoleReader;
import joynr.OnChangeSubscriptionQos;
import joynr.OnChangeWithKeepAliveSubscriptionQos;
import joynr.vehicle.Country;
import joynr.vehicle.GeoPosition;
import joynr.vehicle.RadioBroadcastInterface;
import joynr.vehicle.RadioBroadcastInterface.NewStationDiscoveredBroadcastFilterParameters;
import joynr.vehicle.RadioBroadcastInterface.WeakSignalBroadcastAdapter;
import joynr.vehicle.RadioProxy;
import joynr.vehicle.RadioStation;
import joynr.vehicle.RadioSync.GetLocationOfCurrentStationReturned;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;

public class MyRadioConsumerApplication extends AbstractJoynrApplication {
    private static final String PRINT_BORDER = "\n####################\n";
    private static final Logger LOG = LoggerFactory.getLogger(MyRadioConsumerApplication.class);
    public static final String APP_CONFIG_PROVIDER_DOMAIN = "javademoapp.provider.domain";
    private static final String STATIC_PERSISTENCE_FILE = "consumer-joynr.properties";

    @Inject
    @Named(APP_CONFIG_PROVIDER_DOMAIN)
    private String providerDomain;
    private String subscriptionIdCurrentStation;
    private RadioProxy radioProxy;
    @Inject
    private ObjectMapper objectMapper;

    /**
     * Main method. This method is responsible for: 1. Instantiating the consumer application. 2. Injecting the instance
     * with Guice bindings 3. Starting the application. 4. Ending the application so that the necessary clean up calls
     * are made.
     * 
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        // run application from cmd line using Maven:
        // mvn exec:java -Dexec.mainClass="io.joynr.demo.MyRadioConsumerApplication" -Dexec.args="<provider-domain>"
        if (args.length != 1) {
            LOG.error("USAGE: java {} <provider-domain>", MyRadioConsumerApplication.class.getName());
            return;
        }
        String providerDomain = args[0];
        LOG.debug("Searching for providers on domain \"{}\"", providerDomain);

        // joynr config properties are used to set joynr configuration at compile time. They are set on the
        // JoynInjectorFactory.
        Properties joynrConfig = new Properties();
        // Set a custom static persistence file (default is joynr.properties in the working dir) to store
        // joynr configuration. It allows for changing the joynr configuration at runtime. Custom persistence
        // files support running the consumer and provider applications from within the same directory.
        joynrConfig.setProperty(MessagingPropertyKeys.PERSISTENCE_FILE, STATIC_PERSISTENCE_FILE);

        // How to use custom infrastructure elements:

        // 1) Set them programmatically at compile time using the joynr configuration properties at the
        // JoynInjectorFactory. E.g. uncomment the following lines to set a certain joynr server
        // instance.
        // joynrConfig.setProperty(MessagingPropertyKeys.BOUNCE_PROXY_URL, "http://localhost:8080/bounceproxy/");
        // joynrConfig.setProperty(MessagingPropertyKeys.CAPABILITIESDIRECTORYURL, "http://localhost:8080/discovery/channels/discoverydirectory_channelid/");
        // joynrConfig.setProperty(MessagingPropertyKeys.CHANNELURLDIRECTORYURL, "http://localhost:8080/discovery/channels/discoverydirectory_channelid/");
        joynrConfig.setProperty(PROPERTY_JOYNR_DOMAIN_LOCAL, "radioapp_consumer_local_domain");

        // 2) Or set them in the static persistence file (default: joynr.properties in working dir) at
        // runtime. If not available in the working dir, it will be created during the first launch
        // of the application. Copy the following lines to the custom persistence file to set a
        // certain joynr server instance.
        // NOTE: This application uses a custom static persistence file consumer-joynr.properties.
        // Copy the following lines to the custom persistence file to set a certain joynr server
        // instance.
        // joynr.messaging.bounceproxyurl=http://localhost:8080/bounceproxy/
        // joynr.messaging.capabilitiesdirectoryurl=http://localhost:8080/discovery/channels/discoverydirectory_channelid/
        // joynr.messaging.channelurldirectoryurl=http://localhost:8080/discovery/channels/discoverydirectory_channelid/

        // 3) Or set them in Java System properties.
        // -Djoynr.messaging.bounceProxyUrl=http://localhost:8080/bounceproxy/
        // -Djoynr.messaging.capabilitiesDirectoryUrl=http://localhost:8080/discovery/channels/discoverydirectory_channelid/
        // -Djoynr.messaging.channelUrlDirectoryUrl=http://localhost:8080/discovery/channels/discoverydirectory_channelid/

        // NOTE:
        // Programmatically set configuration properties override properties set in the static persistence file.
        // Java system properties override both

        // Application-specific configuration properties are injected to the application by setting
        // them on the JoynApplicationModule.
        Properties appConfig = new Properties();
        appConfig.setProperty(APP_CONFIG_PROVIDER_DOMAIN, providerDomain);

        JoynrApplication myRadioConsumerApp = new JoynrInjectorFactory(joynrConfig).createApplication(new JoynrApplicationModule(MyRadioConsumerApplication.class,
                                                                                                                                 appConfig));
        myRadioConsumerApp.run();

        myRadioConsumerApp.shutdown();
    }

    @Override
    @SuppressWarnings(value = "DM_EXIT", justification = "WORKAROUND to be removed")
    public void shutdown() {
        if (radioProxy != null) {
            if (subscriptionIdCurrentStation != null) {
                radioProxy.unsubscribeFromCurrentStation(subscriptionIdCurrentStation);
            }
        }

        // Add any clean up code here for your application.
        runtime.shutdown(true);

        // TODO currently there is a bug preventing all threads being stopped
        // WORKAROUND
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            // do nothing; exiting application
        }
        System.exit(0);
    }

    @SuppressWarnings("checkstyle:methodlength")
    @Override
    public void run() {
        DiscoveryQos discoveryQos = new DiscoveryQos();
        // As soon as the arbitration QoS is set on the proxy builder, discovery of suitable providers
        // is triggered. If the discovery process does not find matching providers within the
        // arbitration timeout duration it will be terminated and you will get an arbitration exception.
        discoveryQos.setDiscoveryTimeout(10000);
        // Provider entries in the global capabilities directory are cached locally. Discovery will
        // consider entries in this cache valid if they are younger as the max age of cached
        // providers as defined in the QoS. All valid entries will be processed by the arbitrator when searching
        // for and arbitrating the "best" matching provider.
        // NOTE: Valid cache entries might prevent triggering a lookup in the global capabilities
        // directory. Therefore, not all providers registered with the global capabilities
        // directory might be taken into account during arbitration.
        discoveryQos.setCacheMaxAge(Long.MAX_VALUE);
        // The discovery process outputs a list of matching providers. The arbitration strategy then
        // chooses one or more of them to be used by the proxy.
        discoveryQos.setArbitrationStrategy(ArbitrationStrategy.HighestPriority);

        // The provider will maintain at least a minimum interval idle time in milliseconds between
        // successive notifications, even if on-change notifications are enabled and the value changes more
        // often. This prevents the consumer from being flooded by updated values. The filtering happens on
        // the provider's side, thus also preventing excessive network traffic.
        int minInterval_ms = 0;
        // The provider will send notifications every maximum interval in milliseconds, even if the value didn't
        // change. It will send notifications more often if on-change notifications are enabled,
        // the value changes more often, and the minimum interval QoS does not prevent it. The maximum interval
        // can thus be seen as a sort of heart beat.
        int maxInterval_ms = 10000;

        // The provider will send notifications until the end date is reached. The consumer will not receive any
        // notifications (neither value notifications nor missed publication notifications) after
        // this date.
        long expiryDate_ms = System.currentTimeMillis() + 60000;
        // If no notification was received within the last alert interval, a missed publication
        // notification will be raised.
        int alertAfterInterval_ms = 20000;
        // Notification messages will be sent with this time-to-live. If a notification message can not be
        // delivered within its TTL, it will be deleted from the system.
        // NOTE: If a notification message is not delivered due to an expired TTL, it might raise a
        // missed publication notification (depending on the value of the alert interval QoS).
        int publicationTtl_ms = 5000;

        OnChangeWithKeepAliveSubscriptionQos subscriptionQos = new OnChangeWithKeepAliveSubscriptionQos(minInterval_ms,
                                                                                                        maxInterval_ms,
                                                                                                        expiryDate_ms,
                                                                                                        alertAfterInterval_ms,
                                                                                                        publicationTtl_ms);

        ProxyBuilder<RadioProxy> proxyBuilder = runtime.getProxyBuilder(providerDomain, RadioProxy.class);

        try {
            // getting an attribute
            radioProxy = proxyBuilder.setMessagingQos(new MessagingQos()).setDiscoveryQos(discoveryQos).build();
            RadioStation currentStation = radioProxy.getCurrentStation();
            LOG.info(PRINT_BORDER + "ATTRIBUTE GET: current station: " + currentStation + PRINT_BORDER);

            // subscribe to an attribute
            subscriptionIdCurrentStation = radioProxy.subscribeToCurrentStation(new AttributeSubscriptionAdapter<RadioStation>() {

                                                                                    @Override
                                                                                    public void onReceive(RadioStation value) {
                                                                                        LOG.info(PRINT_BORDER
                                                                                                + "ATTRIBUTE SUBSCRIPTION: current station: "
                                                                                                + value + PRINT_BORDER);
                                                                                    }

                                                                                    @Override
                                                                                    public void onError() {
                                                                                        LOG.info(PRINT_BORDER
                                                                                                + "ATTRIBUTE SUBSCRIPTION: publication missed "
                                                                                                + PRINT_BORDER);
                                                                                    }
                                                                                },
                                                                                subscriptionQos);

            // broadcast subscription

            // The provider will send a notification whenever the value changes. The number of sent
            // notifications may be limited by the min interval QoS.
            // NOTE: The provider must support on-change notifications in order to use this feature by
            // calling the <broadcast>EventOccurred method of the <interface>Provider class whenever
            // the <broadcast> should be triggered.
            OnChangeSubscriptionQos weakSignalBroadcastSubscriptionQos;
            // The provider will maintain at least a minimum interval idle time in milliseconds between
            // successive notifications, even if on-change notifications are enabled and the value changes
            // more often. This prevents the consumer from being flooded by updated values. The filtering
            // happens on the provider's side, thus also preventing excessive network traffic.
            int wsbMinInterval = 1 * 1000;
            // The provider will send notifications until the end date is reached. The consumer will not receive any
            // notifications (neither value notifications nor missed publication notifications) after
            // this date.
            long wsbExpiryDate = System.currentTimeMillis() + 60 * 1000;
            // Notification messages will be sent with this time-to-live. If a notification message can not be
            // delivered within its TTL, it will be deleted from the system.
            // NOTE: If a notification message is not delivered due to an expired TTL, it might raise a
            // missed publication notification (depending on the value of the alert interval QoS).
            int wsbPublicationTtl = 5 * 1000;
            weakSignalBroadcastSubscriptionQos = new OnChangeSubscriptionQos(wsbMinInterval,
                                                                             wsbExpiryDate,
                                                                             wsbPublicationTtl);
            radioProxy.subscribeToWeakSignalBroadcast(new WeakSignalBroadcastAdapter() {
                @Override
                public void onReceive(RadioStation weakSignalStation) {
                    LOG.info(PRINT_BORDER + "BROADCAST SUBSCRIPTION: weak signal: " + weakSignalStation + PRINT_BORDER);
                }
            },
                                                      weakSignalBroadcastSubscriptionQos);

            // selective broadcast subscription

            OnChangeSubscriptionQos newStationDiscoveredBroadcastSubscriptionQos;
            int nsdbMinInterval = 2 * 1000;
            long nsdbExpiryDate = System.currentTimeMillis() + 180 * 1000;
            int nsdbPublicationTtl = 5 * 1000;
            newStationDiscoveredBroadcastSubscriptionQos = new OnChangeSubscriptionQos(nsdbMinInterval,
                                                                                       nsdbExpiryDate,
                                                                                       nsdbPublicationTtl);
            NewStationDiscoveredBroadcastFilterParameters newStationDiscoveredBroadcastFilterParams = new NewStationDiscoveredBroadcastFilterParameters();
            newStationDiscoveredBroadcastFilterParams.setHasTrafficService("true");
            GeoPosition positionOfInterest = new GeoPosition(48.1351250, 11.5819810); // Munich
            String positionOfInterestJson = null;
            try {
                positionOfInterestJson = objectMapper.writeValueAsString(positionOfInterest);
            } catch (JsonProcessingException e1) {
                LOG.error("Unable to write position of interest filter parameter to JSON", e1);
            }
            newStationDiscoveredBroadcastFilterParams.setPositionOfInterest(positionOfInterestJson);
            newStationDiscoveredBroadcastFilterParams.setRadiusOfInterestArea("200000"); // 200 km
            radioProxy.subscribeToNewStationDiscoveredBroadcast(new RadioBroadcastInterface.NewStationDiscoveredBroadcastAdapter() {
                                                                    @Override
                                                                    public void onReceive(RadioStation discoveredStation,
                                                                                          GeoPosition geoPosition) {
                                                                        LOG.info(PRINT_BORDER
                                                                                + "BROADCAST SUBSCRIPTION: new station discovered: "
                                                                                + discoveredStation + " at "
                                                                                + geoPosition + PRINT_BORDER);
                                                                    }
                                                                },
                                                                newStationDiscoveredBroadcastSubscriptionQos,
                                                                newStationDiscoveredBroadcastFilterParams);

            boolean success;

            // add favorite radio station
            RadioStation favouriteStation = new RadioStation("99.3 The Fox Rocks", false, Country.CANADA);
            success = radioProxy.addFavouriteStation(favouriteStation);
            LOG.info(PRINT_BORDER + "METHOD: added favourite station: " + favouriteStation + ": " + success
                    + PRINT_BORDER);

            // shuffle the stations
            radioProxy.shuffleStations();
            currentStation = radioProxy.getCurrentStation();
            LOG.info(PRINT_BORDER + "The current radio station after shuffling is: " + currentStation + PRINT_BORDER);

            // add favourite radio station async
            RadioStation radioStation = new RadioStation("99.4 AFN", false, Country.GERMANY);
            Callback<Boolean> callback = new Callback<Boolean>() {
                @Override
                public void onSuccess(Boolean result) {
                    LOG.info(PRINT_BORDER + "ASYNC METHOD: added favourite station: callback onSuccess" + PRINT_BORDER);
                }

                @Override
                public void onFailure(JoynrRuntimeException error) {
                    LOG.info(PRINT_BORDER + "ASYNC METHOD: added favourite station: callback onFailure" + PRINT_BORDER);
                }
            };
            Future<Boolean> future = radioProxy.addFavouriteStation(callback, radioStation);
            try {
                long timeoutInMilliseconds = 8000;
                Boolean reply = future.getReply(timeoutInMilliseconds);
                LOG.info(PRINT_BORDER + "ASYNC METHOD: added favourite station: " + radioStation + ": " + reply
                        + PRINT_BORDER);
            } catch (InterruptedException|JoynrRuntimeException e) {
                LOG.info(PRINT_BORDER + "ASYNC METHOD: added favourite station: " + radioStation
                        + ": " + e.getClass().getSimpleName() + "!");
            }

            ConsoleReader console;
            try {
                console = new ConsoleReader();
                int key;
                while ((key = console.readCharacter()) != 'q') {
                    switch (key) {
                    case 's':
                        radioProxy.shuffleStations();
                        LOG.info("called shuffleStations");
                        break;
                    case 'm':
                        GetLocationOfCurrentStationReturned locationOfCurrentStation = radioProxy.getLocationOfCurrentStation();
                        LOG.info("called getLocationOfCurrentStation. country: " + locationOfCurrentStation.country + ", location: " + locationOfCurrentStation.location);
                    default:
                        LOG.info("\n\nUSAGE press\n" + " q\tto quit\n" + " s\tto shuffle stations\n");
                        break;
                    }
                }
            } catch (IOException e) {
                LOG.error("error reading input from console", e);
            }

        } catch (JoynrArbitrationException e) {
            LOG.error("No provider found", e);
        } catch (JoynrCommunicationException e) {
            LOG.error("The message was not sent: ", e);
        }
    }
}
