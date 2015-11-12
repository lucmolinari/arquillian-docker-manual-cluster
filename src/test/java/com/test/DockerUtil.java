package com.test;

import java.net.URI;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.jaxrs.DockerCmdExecFactoryImpl;

public class DockerUtil {

    private static DockerClient dockerClient;

    public static DockerClient getDockerClient() {
        if (dockerClient == null) {
            final DockerClientConfig config = DockerClientConfig.createDefaultConfigBuilder()
                    .withUri(URI.create("unix:///var/run/docker.sock").toString()).build();

            final DockerCmdExecFactoryImpl dockerCmdExecFactory = new DockerCmdExecFactoryImpl().withReadTimeout(1000).withConnectTimeout(1000)
                    .withMaxTotalConnections(100).withMaxPerRouteConnections(10);

            dockerClient = DockerClientBuilder.getInstance(config).withDockerCmdExecFactory(dockerCmdExecFactory).build();
        }

        return dockerClient;
    }

}