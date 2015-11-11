package com.test;

import java.io.IOException;
import java.net.URI;

import javax.ejb.EJB;
import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.DockerCmdExecFactory;
import com.github.dockerjava.api.model.Event;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.command.EventsResultCallback;
import com.github.dockerjava.jaxrs.DockerCmdExecFactoryImpl;

@RunWith(Arquillian.class)
public class SimpleEJBTest {

	private static final String SERVER = "jboss_1";
	private static final String TEST_DEPLOYMENT = "DEPLOYMENT";

	@ArquillianResource
	private Deployer deployer;

	@EJB
	private SimpleEJB simpleEJB;

	@Deployment(name = TEST_DEPLOYMENT, managed = false)
	@TargetsContainer(SERVER)
	public static Archive<?> deployTestEar() {
		final WebArchive archive = ShrinkWrap.create(WebArchive.class, SimpleEJBTest.class.getSimpleName() + ".war")
				.addClass(SimpleEJB.class).addClass(DockerCmdExecFactory.class);
		//.addAsWebInfResource("META-INF/beans.xml")
		return archive;
	}

	@Test
	@InSequence(1)
	@RunAsClient
	public void startContainer() throws Exception {
		final ExposedPort http = ExposedPort.tcp(8580);
		final ExposedPort mgmtHttp = ExposedPort.tcp(10490);
		final Ports portBindings = new Ports();
		portBindings.bind(http, Ports.Binding(8580));
		portBindings.bind(mgmtHttp, Ports.Binding(10490));

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
								+ "-b 0.0.0.0 -bmanagement 0.0.0.0 -Djboss.socket.binding.port-offset=500")
				.withName("test_wildfly").withExposedPorts(http, mgmtHttp).withPortBindings(portBindings).exec();
		dockerClient.startContainerCmd("test_wildfly").exec();
		
		

		// TODO: Implement wait based on port availability or log
		try {
			Thread.sleep(7000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Test
	@InSequence(2)
	public void deployPackage() {
		this.deployer.deploy(TEST_DEPLOYMENT);
	}
	
	@Test
	@InSequence(3)
	@OperateOnDeployment(TEST_DEPLOYMENT)
	public void simpleTest() {
		Assert.assertEquals("Hello world", simpleEJB.sayHello());
	}

	// add test cases
	// stop/remove container

}