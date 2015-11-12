package com.test;

import static com.test.DockerUtil.getDockerClient;

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

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Ports;

@RunWith(Arquillian.class)
public class SimpleEJBTest {

    private static final String SERVER = "jboss_1";
    private static final String TEST_DEPLOYMENT = "DEPLOYMENT";

    private static final String IMAGE_NAME = "test_wildfly";
    private static final String CONTAINER_NAME = "test_wildfly_container";

    @ArquillianResource
    private Deployer deployer;

    @Inject
    private SimpleEJB simpleEJB;

    @Deployment(name = TEST_DEPLOYMENT, managed = false)
    @TargetsContainer(SERVER)
    public static Archive<?> deployTestEar() {
        final WebArchive archive = ShrinkWrap.create(WebArchive.class, SimpleEJBTest.class.getSimpleName() + ".war")
                .addAsWebInfResource("META-INF/beans.xml").addClass(SimpleEJB.class).addClass(SimpleEJBTest.class);
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

        getDockerClient().createContainerCmd(IMAGE_NAME)
        .withCmd("/bin/sh", "-c", "$JBOSS_HOME/bin/standalone.sh -c standalone-full.xml "
                + "-b $HOSTNAME -bmanagement $HOSTNAME -Djboss.socket.binding.port-offset=500")
                .withName(CONTAINER_NAME).withExposedPorts(http, mgmtHttp).withPortBindings(portBindings).exec();
        getDockerClient().startContainerCmd(CONTAINER_NAME).exec();

        // TODO: Implement wait based on port availability or log
        try {
            Thread.sleep(10000);
        } catch (final InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    @InSequence(2)
    @RunAsClient
    public void deployPackage() {
        deployer.deploy(TEST_DEPLOYMENT);
    }

    @Test
    @InSequence(3)
    @OperateOnDeployment(TEST_DEPLOYMENT)
    public void simpleTest() {
        Assert.assertEquals("Hello world", simpleEJB.sayHello());
    }

    @Test
    @InSequence(5)
    @RunAsClient
    public void undeploy() {
        deployer.undeploy(TEST_DEPLOYMENT);
    }

    @Test
    @InSequence(5)
    @RunAsClient
    public void stopAndRemoveContainer() {
        getDockerClient().stopContainerCmd(CONTAINER_NAME).exec();
        final InspectContainerResponse inspectContainerResponse = getDockerClient().inspectContainerCmd(CONTAINER_NAME)
                .exec();
        Assert.assertFalse(inspectContainerResponse.getState().isRunning());

        getDockerClient().removeContainerCmd(CONTAINER_NAME).exec();
    }

}