package hudson.plugins.createjobadvanced;

import hudson.maven.MavenModuleSet;
import hudson.maven.reporters.MavenMailer;
import java.io.IOException;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class MavenConfigurerTest {
    @Rule
    public JenkinsRule r = new JenkinsRule();

    @Test
    public void testPreConfigureMavenJob() {
        try {
            MavenModuleSet mavenModuleSet = r.jenkins.createProject(MavenModuleSet.class, "test");
            MavenConfigurer configurer = new MavenConfigurer();
            mavenModuleSet.getReporters().remove(MavenMailer.class);
            Assert.assertNull(mavenModuleSet.getReporters().get(MavenMailer.class));
            configurer.doCreate(mavenModuleSet);
            Assert.assertNotNull(mavenModuleSet.getReporters().get(MavenMailer.class));
        } catch (IOException e) {
            Assert.assertTrue(false);
        }
    }

    @Test
    public void testPreConfigureMavenJob2() {
        try {
            MavenModuleSet mavenModuleSet = r.jenkins.createProject(MavenModuleSet.class, "test");
            MavenConfigurer configurer = new MavenConfigurer();
            Assert.assertNotNull(mavenModuleSet.getReporters().get(MavenMailer.class));
            configurer.doCreate(mavenModuleSet);
        } catch (IOException e) {
            Assert.assertTrue(false);
        }
    }
}
