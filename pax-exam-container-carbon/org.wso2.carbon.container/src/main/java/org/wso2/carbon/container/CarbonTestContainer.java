package org.wso2.carbon.container;

import org.apache.commons.io.FileUtils;
import org.ops4j.net.FreePort;
import org.ops4j.pax.exam.ExamSystem;
import org.ops4j.pax.exam.RelativeTimeout;
import org.ops4j.pax.exam.TestAddress;
import org.ops4j.pax.exam.TestContainer;
import org.ops4j.pax.exam.TestContainerException;
import org.ops4j.pax.exam.container.remote.RBCRemoteTarget;
import org.ops4j.pax.exam.options.SystemPropertyOption;
import org.ops4j.pax.exam.options.ValueOption;
import org.ops4j.pax.exam.options.extra.RepositoryOption;
import org.ops4j.pax.exam.options.extra.VMOption;
import org.ops4j.pax.exam.rbc.client.RemoteBundleContextClient;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.container.options.CarbonDistributionConfigurationFileCopyOption;
import org.wso2.carbon.container.options.CarbonDistributionConfigurationOption;
import org.wso2.carbon.container.options.CarbonDistributionExternalBundleOption;
import org.wso2.carbon.container.options.EnvironmentPropertyOption;
import org.wso2.carbon.container.options.KeepRuntimeDirectory;
import org.wso2.carbon.container.runner.CarbonRunner;
import org.wso2.carbon.container.runner.Runner;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.rmi.NoSuchObjectException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.rbc.Constants.RMI_HOST_PROPERTY;
import static org.ops4j.pax.exam.rbc.Constants.RMI_NAME_PROPERTY;
import static org.ops4j.pax.exam.rbc.Constants.RMI_PORT_PROPERTY;

public class CarbonTestContainer implements TestContainer {

    private static final Logger logger = LoggerFactory.getLogger(CarbonTestContainer.class);

    private static final String CARBON_TEST_CONTAINER = "CarbonTestContainer";
    private static final String EXAM_INJECT_PROPERTY = "pax.exam.inject";

    private final Runner runner;
    private final ExamSystem system;
    private CarbonDistributionConfigurationOption carbonDistributionConfigurationOption;
    private boolean started;
    private RBCRemoteTarget target;
    private Path targetDirectory;
    private Registry registry;

    public CarbonTestContainer(ExamSystem system,
            CarbonDistributionConfigurationOption carbonDistributionConfigurationOption) {
        this.carbonDistributionConfigurationOption = carbonDistributionConfigurationOption;
        this.system = system;
        this.runner = new CarbonRunner();
    }

    public synchronized TestContainer start() {

        if (carbonDistributionConfigurationOption.getDistributionDirectoryPath() == null
                && carbonDistributionConfigurationOption.getDistributionMavenURL() == null &&
                carbonDistributionConfigurationOption.getDistributionZipPath() == null) {
            throw new IllegalStateException("Either distributionURL or distributionUrlReference need to be set.");
        }

        try {
            String name = system.createID(CARBON_TEST_CONTAINER);
            FreePort freePort = new FreePort(21000, 21099);
            int port = freePort.getPort();
            logger.info("using RMI registry at port {}" + name, port);
            registry = LocateRegistry.createRegistry(port);
            String host = InetAddress.getLocalHost().getHostName();

            //setting RMI related properties
            ExamSystem subsystem = system.fork(options(systemProperty(RMI_HOST_PROPERTY).value(host),
                    systemProperty(RMI_PORT_PROPERTY).value(Integer.toString(port)),
                    systemProperty(RMI_NAME_PROPERTY).value(name), systemProperty(EXAM_INJECT_PROPERTY).value("true")));

            target = new RBCRemoteTarget(name, port, subsystem.getTimeout());
            System.setProperty("java.protocol.handler.pkgs", "org.ops4j.pax.url");
            addRepositories();
            targetDirectory = retrieveFinalTargetDirectory();

            if (carbonDistributionConfigurationOption.getDistributionMavenURL() != null) {
                URL sourceDistribution = new URL(
                        carbonDistributionConfigurationOption.getDistributionMavenURL().getURL());
                ArchiveExtractor.extract(sourceDistribution, targetDirectory.toFile());
            } else if (carbonDistributionConfigurationOption.getDistributionZipPath() != null) {
                Path sourceDistribution = carbonDistributionConfigurationOption.getDistributionZipPath();
                ArchiveExtractor.extract(sourceDistribution, targetDirectory.toFile());
            } else if (carbonDistributionConfigurationOption.getDistributionDirectoryPath() != null) {
                Path sourceDirectory = carbonDistributionConfigurationOption.getDistributionDirectoryPath();
                FileUtils.copyDirectory(sourceDirectory.toFile(), targetDirectory.toFile());
            }

            copyReferencedBundles(targetDirectory, subsystem);
            copyConfigurationFiles(targetDirectory);
            startCarbon(subsystem, targetDirectory);
            started = true;
        } catch (IOException e) {
            throw new RuntimeException("Problem starting container", e);
        }
        return this;
    }

    private void addRepositories() {
        RepositoryOption[] repositories = system.getOptions(RepositoryOption.class);
        if (repositories.length != 0) {
            System.setProperty("org.ops4j.pax.url.mvn.repositories", buildString(repositories));
        }
    }

    private File createUnique(String url, File deploy) {
        String prefix = UUID.randomUUID().toString();
        String fileName = new File(url).getName();
        return new File(deploy, prefix + "_" + fileName + ".jar");
    }

    private void copyReferencedArtifactsToDeployDirectory(String url, Path targetDirectory) {
        File target = createUnique(url, targetDirectory.toFile());
        try {
            FileUtils.copyURLToFile(new URL(url), target);
        } catch (IOException e) {
            logger.error("Error while copying Artifacts", e);
        }
    }

    /**
     * Copy dependencies specified as ProvisionOption in system to the dropins Directory
     */
    private void copyReferencedBundles(Path carbonHome, ExamSystem system) {
        Path targetDirectory = carbonHome.resolve("osgi").resolve("dropins");

        Arrays.asList(system.getOptions(CarbonDistributionExternalBundleOption.class)).forEach(
                option -> copyReferencedArtifactsToDeployDirectory(option.getMavenArtifactUrlReference().getURL(),
                        targetDirectory));
    }

    private void copyConfigurationFiles(Path carbonHome) {
        Arrays.asList(system.getOptions(CarbonDistributionConfigurationFileCopyOption.class)).forEach(option -> {
            try {
                Files.copy(option.getSourcePath(), carbonHome.resolve(option.getDestinationPath()),
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                logger.error("Error while copying configuration files", e);
            }
        });
    }

    private void startCarbon(ExamSystem subsystem, Path carbonHome) throws IOException {
        long startedAt = System.currentTimeMillis();
        Path carbonBin = carbonHome.resolve("bin");
        makeScriptsInBinExec(carbonBin.toFile());
        ArrayList<String> javaOpts = new ArrayList<>();
        String[] environment = setupEnvironmentProperties(subsystem);
        setupSystemProperties(javaOpts, subsystem);
        runner.exec(environment, carbonHome, javaOpts);
        logger.info("Wait for test container to finish its initialization " + subsystem.getTimeout());
        waitForState(0, Bundle.ACTIVE, subsystem.getTimeout());
        logger.info("Test Container started in " + (System.currentTimeMillis() - startedAt) + " millis");
    }

    private String[] setupEnvironmentProperties(ExamSystem subsystem) {
        EnvironmentPropertyOption[] options = subsystem.getOptions(EnvironmentPropertyOption.class);
        return Arrays.asList(options).stream().map(EnvironmentPropertyOption::getOption).collect(Collectors.toList())
                .toArray(new String[options.length]);
    }

    private void setupSystemProperties(List<String> javaOpts, ExamSystem subsystem) throws IOException {
        Arrays.asList(subsystem.getOptions(SystemPropertyOption.class)).forEach(systemPropertyOption -> {
            String property = String.format("-D%s=%s", systemPropertyOption.getKey(), systemPropertyOption.getValue());
            javaOpts.add(property);
        });

        Arrays.asList(subsystem.getOptions(VMOption.class)).forEach(vmOption -> javaOpts.add(vmOption.getOption()));
    }

    private void makeScriptsInBinExec(File carbonBin) {
        if (!carbonBin.exists()) {
            return;
        }
        File[] files = carbonBin.listFiles();
        if (files != null) {
            Arrays.asList(files).forEach(file -> file.setExecutable(true));
        }
    }

    private Path retrieveFinalTargetDirectory() {
        Path unpackDirectory = carbonDistributionConfigurationOption.getUnpackDirectory();
        if (unpackDirectory == null) {
            unpackDirectory = Paths.get("target", UUID.randomUUID().toString());
        }
        unpackDirectory.toFile().mkdir();
        return unpackDirectory;
    }

    private boolean shouldDeleteRuntime() {
        boolean deleteRuntime = true;
        KeepRuntimeDirectory[] keepRuntimeDirectory = system.getOptions(KeepRuntimeDirectory.class);
        if (keepRuntimeDirectory != null && keepRuntimeDirectory.length != 0) {
            deleteRuntime = false;
        }
        return deleteRuntime;
    }

    private String buildString(ValueOption<?>[] options) {
        return buildString(new String[0], options, new String[0]);
    }

    private String buildString(String[] prepend, ValueOption<?>[] options, String[] append) {
        StringBuilder builder = new StringBuilder();
        for (String a : prepend) {
            builder.append(a);
            builder.append(",");
        }
        for (ValueOption<?> option : options) {
            builder.append(option.getValue());
            builder.append(",");
        }
        for (String a : append) {
            builder.append(a);
            builder.append(",");
        }
        if (builder.length() > 0) {
            return builder.substring(0, builder.length() - 1);
        } else {
            return "";
        }
    }

    @Override
    public synchronized TestContainer stop() {
        logger.debug("Shutting down the test container.");
        try {
            if (started) {
                target.stop();
                RemoteBundleContextClient remoteBundleContextClient = target.getClientRBC();
                if (remoteBundleContextClient != null) {
                    remoteBundleContextClient.stop();

                }
                runner.shutdown();
                try {
                    UnicastRemoteObject.unexportObject(registry, true);
                } catch (NoSuchObjectException exc) {
                    throw new TestContainerException(exc);
                }

            } else {
                throw new RuntimeException("Container never started.");
            }
        } finally {
            started = false;
            target = null;
            if (shouldDeleteRuntime()) {
                system.clear();
                try {
                    FileUtils.forceDelete(targetDirectory.toFile());
                } catch (IOException e) {
                    forceCleanup();
                }
            }
        }
        return this;
    }

    private void forceCleanup() {
        try {
            FileUtils.forceDeleteOnExit(targetDirectory.toFile());
        } catch (IOException e) {
            logger.error("Error occured when deleting the Directory.", e);
        }
    }

    private void waitForState(final long bundleId, final int state, final RelativeTimeout timeout) {
        target.getClientRBC().waitForState(bundleId, state, timeout);
    }

    @Override
    public synchronized void call(TestAddress address) {
        target.call(address);
    }

    @Override
    public synchronized long install(InputStream stream) {
        return install("local", stream);
    }

    @Override
    public synchronized long install(String location, InputStream stream) {
        return target.install(location, stream);
    }

    @Override
    public long installProbe(InputStream stream) {
        return target.installProbe(stream);
    }

    @Override
    public void uninstallProbe() {
        target.uninstallProbe();
    }

    @Override
    public String toString() {
        return "CarbonTestContainer";
    }
}
