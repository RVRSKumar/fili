// Copyright 2016 Yahoo Inc.
// Licensed under the terms of the Apache license. Please see LICENSE file distributed with this work for terms.
package com.yahoo.bard.webservice.config;

import static com.yahoo.bard.webservice.config.ConfigMessageFormat.TOO_MANY_APPLICATION_CONFIGS;
import static com.yahoo.bard.webservice.config.ConfigMessageFormat.TOO_MANY_USER_CONFIGS;

import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.EnvironmentConfiguration;
import org.apache.commons.configuration.MapConfiguration;
import org.apache.commons.configuration.SystemConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A class to hold and fetch configuration values from the environment, the system, user, application, module and
 * library configurations.
 * <p>
 * LayeredFileSystemConfig uses a layered model with the highest priority granted to environment variables, followed
 * by system properties, then user configuration, application configuration, module configurations and then core
 * library default configuration.
 * <p>
 * LayeredFileSystemConfig also uses a Properties resource to allow runtime override of configured behavior.
 */
public class LayeredFileSystemConfig implements SystemConfig {

    private static final Logger LOG = LoggerFactory.getLogger(LayeredFileSystemConfig.class);

    /**
     * The resource path for local user override of application and default properties.
     */
    private static final String USER_CONFIG_FILE_NAME = "/userConfig.properties";

    /**
     * The resource path for configuring properties within an application.
     */
    private static final String APPLICATION_CONFIG_FILE_NAME = "/applicationConfig.properties";

    /**
     * The resource path for configuring properties within an application.
     */
    private static final String TEST_CONFIG_FILE_NAME = "/testApplicationConfig.properties";

    /**
     * The composite configuration containing the layered properties and values.
     */
    private final CompositeConfiguration masterConfiguration;

    private final Properties runtimeProperties;

    private final ConfigResourceLoader loader = new ConfigResourceLoader();

    /**
     * A layered file system config loads a single (required) default configuration, followed by any number of module
     * configurations, followed by an optional application configuration, followed by a
     */
    @SuppressWarnings(value = "unchecked")
    LayeredFileSystemConfig() {
        masterConfiguration = new CompositeConfiguration();
        masterConfiguration.setThrowExceptionOnMissing(true);
        runtimeProperties = new Properties();

        try {

            List<Configuration> userConfig = loader.loadConfigurations(USER_CONFIG_FILE_NAME);
            if (userConfig.size() > 1) {
                List<Resource> resources = loader.loadResourcesWithName(USER_CONFIG_FILE_NAME)
                        .collect(Collectors.toList());
                LOG.error(TOO_MANY_USER_CONFIGS.logFormat(resources.toString()));
                throw new SystemConfigException(TOO_MANY_USER_CONFIGS.format(resources.size()));
            }

            // This layer allows users to provide an override in their local tests.
            List<Configuration> testApplicationConfig = loader.loadConfigurationsNoJars(TEST_CONFIG_FILE_NAME);

            // Application configuration defined configuration for a given bard-implementing application
            List<Configuration> applicationConfig = loader.loadConfigurations(APPLICATION_CONFIG_FILE_NAME);

            if (applicationConfig.size() > 1) {
                List<Resource> resources = loader.loadResourcesWithName(APPLICATION_CONFIG_FILE_NAME)
                        .collect(Collectors.toList());
                LOG.error(TOO_MANY_APPLICATION_CONFIGS.logFormat(resources.toString()));
                throw new SystemConfigException(TOO_MANY_APPLICATION_CONFIGS.format(resources.size()));
            }

            // Load the rest of the config "top down" through the layers, in highest to lowest precedence
            Stream.of(
                    Stream.of(new MapConfiguration(runtimeProperties)),
                    Stream.of(new EnvironmentConfiguration()),
                    Stream.of(new SystemConfiguration()),
                    userConfig.stream(),
                    testApplicationConfig.stream(),
                    applicationConfig.stream()
            )
                    .flatMap(Function.identity())
                    .filter(Objects::nonNull)
                    .forEachOrdered(masterConfiguration::addConfiguration);


            // Use the config which has been loaded to identify module dependencies
            List<String> dependentModules = (List<String>) masterConfiguration.getList(
                    ConfigurationGraph.DEPENDENT_MODULE_KEY,
                    Collections.<String>emptyList()
            );

            // Add module dependencies to the master configuration
            new ModuleLoader(loader).getConfigurations(dependentModules).forEach(
                    masterConfiguration::addConfiguration
            );
        } catch (IOException e) {
            throw new SystemConfigException(e);
        }
    }

    @Override
    public CompositeConfiguration getMasterConfiguration() {
        return masterConfiguration;
    }

    @Override
    public Properties getRuntimeProperties() {
        return runtimeProperties;
    }
}
