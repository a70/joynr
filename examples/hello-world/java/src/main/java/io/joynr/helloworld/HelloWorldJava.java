package io.joynr.helloworld;

import java.util.concurrent.Semaphore;

import io.joynr.arbitration.DiscoveryQos;
import io.joynr.arbitration.DiscoveryScope;
import io.joynr.provider.Promise;
import io.joynr.runtime.AbstractJoynrApplication;
import io.joynr.runtime.CCInProcessRuntimeModule;
import io.joynr.runtime.JoynrApplication;
import io.joynr.runtime.JoynrInjectorFactory;
import joynr.io.joynr.HelloWorldServiceAbstractProvider;
import joynr.io.joynr.HelloWorldServiceProxy;
import joynr.types.ProviderQos;
import joynr.types.ProviderScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The simplest possible example of setting up a provider and a consumer which talk to eachother
 * via joynr. This example runs entirely within one runtime and the joynr participants communicate
 * via in-process messaging. For an example of joynr participants communicating via external transports
 * see the radio-app example.
 */
public class HelloWorldJava {

    private static final Logger logger = LoggerFactory.getLogger(HelloWorldJava.class);

    private static final String LOCAL_DOMAIN = "hello.world.domain";

    private static Semaphore executionSemaphore = new Semaphore(0);

    public static final void main(String ...args) throws InterruptedException {
        JoynrInjectorFactory joynrInjectorFactory = new JoynrInjectorFactory(new CCInProcessRuntimeModule());
        JoynrApplication helloWorldApplication = joynrInjectorFactory.createApplication(HelloWorldApplication.class);
        helloWorldApplication.run();

        // Wait for the application to finish, shutdown and exit
        executionSemaphore.acquire();
        helloWorldApplication.shutdown();
        logger.info("Done.");
    }

    static class HelloWorldServiceProvider extends HelloWorldServiceAbstractProvider {
        @Override
        public Promise<SayHelloDeferred> sayHello(String toName) {
            SayHelloDeferred deferred = new SayHelloDeferred();
            deferred.resolve("Hello " + toName);
            return new Promise<>(deferred);
        }
    }

    static class HelloWorldApplication extends AbstractJoynrApplication {
        private HelloWorldServiceProvider provider = new HelloWorldServiceProvider();
        @Override
        public void run() {

            // Register the provider
            ProviderQos providerQos = new ProviderQos();
            providerQos.setScope(ProviderScope.LOCAL);
            this.runtime.registerProvider(LOCAL_DOMAIN, provider, providerQos);

            // Create a consumer proxy for the provider
            DiscoveryQos discoveryQos = new DiscoveryQos();
            discoveryQos.setDiscoveryScope(DiscoveryScope.LOCAL_ONLY);
            HelloWorldServiceProxy proxy = runtime.getProxyBuilder(LOCAL_DOMAIN, HelloWorldServiceProxy.class).setDiscoveryQos(discoveryQos).build();

            // Print the result and exit
            String result = proxy.sayHello("world");
            logger.info("RESULT: " + result);
            executionSemaphore.release();
        }
    }
}
