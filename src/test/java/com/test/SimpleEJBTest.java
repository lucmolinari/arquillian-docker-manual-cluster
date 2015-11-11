package com.test;

import java.net.URI;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.jaxrs.DockerCmdExecFactoryImpl;

@RunWith(Arquillian.class)
public class SimpleEJBTest {

	private static final String SERVER = "jboss_1";
	private static final String TEST_DEPLOYMENT = "DEPLOYMENT";

	@ArquillianResource
	private Deployer deployer;

	@Inject
	private SimpleEJB simpleEJB;

	@Deployment(name = TEST_DEPLOYMENT, managed = false)
	@TargetsContainer(SERVER)
	public static Archive<?> deployTestEar() {
		final WebArchive archive = ShrinkWrap.create(WebArchive.class, SimpleEJBTest.class.getSimpleName() + ".war")
				.addAsWebInfResource("META-INF/beans.xml").addClass(SimpleEJBTest.class).addClass(SimpleEJB.class);
		return archive;
	}

	@Test
	@InSequence(1)
	@RunAsClient
	public void startContainer() {
		final ExposedPort http = ExposedPort.tcp(8581);
		final ExposedPort mgmt = ExposedPort.tcp(10500);
		final Ports portBindings = new Ports();
		portBindings.bind(http, Ports.Binding(8581));
		portBindings.bind(mgmt, Ports.Binding(10500));

		final DockerClientConfig config = DockerClientConfig.createDefaultConfigBuilder()
				.withUri(URI.create("unix:///var/run/docker.sock").toString()).build();

		final DockerCmdExecFactoryImpl dockerCmdExecFactory = new DockerCmdExecFactoryImpl().withReadTimeout(1000)
				.withConnectTimeout(1000).withMaxTotalConnections(100).withMaxPerRouteConnections(10);

		final DockerClient dockerClient = DockerClientBuilder.getInstance(config)
				.withDockerCmdExecFactory(dockerCmdExecFactory).build();

		final CreateContainerResponse response = dockerClient
				.createContainerCmd("test_wildfly")
				.withCmd(
						"/bin/sh",
						"-c",
						"$JBOSS_HOME/bin/standalone.sh -c standalone-full.xml "
								+ "-b $HOSTNAME -bmanagement $HOSTNAME -Djboss.socket.binding.port-offset=501")
				.withName("test_wildfly").withExposedPorts(http, mgmt).withPortBindings(portBindings).exec();
		dockerClient.startContainerCmd("test_wildfly").exec();

		// TODO: Implement wait based on port availability or log
		try {
			Thread.sleep(15000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Test
	@InSequence(2)
	public void deployPackage() {
		this.deployer.deploy(TEST_DEPLOYMENT);
	}

	// add test cases
	// stop/remove container

}