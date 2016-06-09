/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wso2.carbon.container.options;

import org.ops4j.lang.NullArgumentException;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.options.MavenUrlReference;

import java.nio.file.Path;

public class CarbonDistributionConfigurationOption implements Option {

    private Path distributionDirectoryPath;
    private Path distributionZipPath;
    private MavenUrlReference distributionMavenURL;
    private String name;
    private Path unpackDirectory;

    public CarbonDistributionConfigurationOption() {
        distributionDirectoryPath = null;
        distributionMavenURL = null;
        name = null;
    }

    /**
     * Sets the path of the distribution as a zip file.
     *
     * @param distributionZipPath distribution path
     * @return this
     */
    public CarbonDistributionConfigurationOption distributionZipURL(Path distributionZipPath) {
        NullArgumentException.validateNotNull(distributionZipPath, "Distribution Zip Path");
        this.distributionZipPath = distributionZipPath;
        return this;
    }

    /**
     * Sets the path of the distribution as a directory.
     *
     * @param distributionDirectoryPath distribution directory path
     * @return this
     */
    public CarbonDistributionConfigurationOption distributionDirectoryURL(Path distributionDirectoryPath) {
        NullArgumentException.validateNotNull(distributionDirectoryPath, "Distribution Directory Path");
        this.distributionDirectoryPath = distributionDirectoryPath;
        return this;
    }

    /**
     * Sets the URL of the distribution as a maven reference.
     *
     * @param distributionMavenURL distribution maven URL
     * @return this
     */
    public CarbonDistributionConfigurationOption distributionMavenURL(MavenUrlReference distributionMavenURL) {
        NullArgumentException.validateNotNull(distributionMavenURL, "Distribution Maven URL");
        this.distributionMavenURL = distributionMavenURL;
        return this;
    }

    /**
     * Sets the name of the Distribution. This is only used for logging.
     *
     * @param name distribution name
     * @return this
     */
    public CarbonDistributionConfigurationOption name(String name) {
        this.name = name;
        return this;
    }

    /**
     * Define the unpack directory for the carbon distribution. In this directory a UUID named
     * directory will be created for each environment.
     *
     * @param unpackDirectory unpack directory
     * @return this for fluent syntax
     */
    public CarbonDistributionConfigurationOption unpackDirectory(Path unpackDirectory) {
        this.unpackDirectory = unpackDirectory;
        return this;
    }

    public MavenUrlReference getDistributionMavenURL() {
        return distributionMavenURL;
    }

    public Path getDistributionDirectoryPath() {
        return distributionDirectoryPath;
    }

    public Path getDistributionZipPath() {
        return distributionZipPath;
    }

    public String getName() {
        return name;
    }

    public Path getUnpackDirectory() {
        return unpackDirectory;
    }

}
