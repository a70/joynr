/**
 *
 */
package test.io.joynr.jeeintegration;

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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import javax.ejb.Stateless;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.util.AnnotationLiteral;

import joynr.jeeintegration.servicelocator.MyServiceProvider;
import joynr.jeeintegration.servicelocator.MyServiceSync;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import test.io.joynr.jeeintegration.servicelocator.MyInvalidServiceSync;
import io.joynr.exceptions.JoynrRuntimeException;
import io.joynr.jeeintegration.ServiceProviderDiscovery;
import io.joynr.jeeintegration.api.ServiceProvider;

/**
 * Unit tests for {@link ServiceProviderDiscovery}.
 */
@RunWith(MockitoJUnitRunner.class)
public class ServiceProviderDiscoveryTest {

    @ServiceProvider(serviceInterface = MyServiceSync.class)
    @Stateless
    private class DummyBeanOne implements MyServiceSync {

        @Override
        public String callMe(String parameterOne) throws JoynrRuntimeException {
            return "DummyBeanOne";
        }
    }

    @Stateless
    private class DummyBeanTwo implements MyServiceSync {

        @Override
        public String callMe(String parameterOne) throws JoynrRuntimeException {
            return "DummyBeanTwo";
        }
    }

    @ServiceProvider(serviceInterface = MyInvalidServiceSync.class)
    @Stateless
    private class DummyBeanThree implements MyInvalidServiceSync {

        @Override
        public void test() {
            //do nothing
        }
    }

    @SuppressWarnings({ "unchecked", "serial" })
    @Test
    public void testFindServiceProviderBeans() {
        BeanManager mockBeanManager = mock(BeanManager.class);

        Bean<DummyBeanOne> mockBeanOne = mock(Bean.class);
        Mockito.doReturn(DummyBeanOne.class).when(mockBeanOne).getBeanClass();
        Bean<DummyBeanTwo> mockBeanTwo = mock(Bean.class);
        Mockito.doReturn(DummyBeanTwo.class).when(mockBeanTwo).getBeanClass();
        Bean<DummyBeanThree> mockBeanThree = mock(Bean.class);
        Mockito.doReturn(DummyBeanThree.class).when(mockBeanThree).getBeanClass();

        Set<Bean<?>> beans = new HashSet<>();
        beans.add(mockBeanOne);
        beans.add(mockBeanTwo);
        beans.add(mockBeanThree);
        Mockito.when(mockBeanManager.getBeans(Object.class, new AnnotationLiteral<Any>() {
        })).thenReturn(beans);

        ServiceProviderDiscovery subject = new ServiceProviderDiscovery(mockBeanManager);

        Set<Bean<?>> result = subject.findServiceProviderBeans();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.iterator().next().getBeanClass().equals(DummyBeanOne.class));
    }

    @Test
    public void testFindProviderForCorrectInterface() {
        ServiceProviderDiscovery subject = new ServiceProviderDiscovery(mock(BeanManager.class));
        Class<?> result = subject.getProviderInterfaceFor(MyServiceSync.class);
        assertNotNull(result);
        assertEquals(MyServiceProvider.class, result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFindProviderForInterfaceWithoutServiceProviderAnnotation() {
        ServiceProviderDiscovery subject = new ServiceProviderDiscovery(mock(BeanManager.class));
        subject.getProviderInterfaceFor(Serializable.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFindProviderForInterfaceWithCorruptServiceProviderAnnotation() {
        ServiceProviderDiscovery subject = new ServiceProviderDiscovery(mock(BeanManager.class));
        subject.getProviderInterfaceFor(MyInvalidServiceSync.class);
    }

}