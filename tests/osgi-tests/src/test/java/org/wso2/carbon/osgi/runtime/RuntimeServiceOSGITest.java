/*
* Copyright 2015 WSO2, Inc. http://www.wso2.org
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.wso2.carbon.osgi.runtime;

import org.ops4j.pax.exam.ExamFactory;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.ops4j.pax.exam.testng.listener.PaxExam;
import org.osgi.framework.BundleContext;
import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.wso2.carbon.kernel.runtime.RuntimeService;
import org.wso2.carbon.kernel.utils.CarbonServerInfo;
import org.wso2.carbon.osgi.test.util.container.CarbonContainerFactory;

import javax.inject.Inject;

/**
 * RuntimeServiceOSGITest class is to test the availability and the functionality of the Runtime Service.
 *
 * @since 5.0.0
 */
@Listeners(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
@ExamFactory(CarbonContainerFactory.class)
public class RuntimeServiceOSGITest {

    @Inject
    private BundleContext bundleContext;

    @Inject
    private RuntimeService runtimeService;

    @Inject
    private CarbonServerInfo carbonServerInfo;

    @Test
    public void testRuntimeService() {
        // TODO Remvoe this test.
        Assert.assertNotNull(runtimeService, "Pluggable Runtime Service is null");
    }

}
