package hudson.plugins.createjobadvanced;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import hudson.maven.MavenModuleSet;
import hudson.maven.reporters.MavenMailer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class MavenConfigurerTest {

    private JenkinsRule r;

    @BeforeEach
    void setUp(JenkinsRule rule) {
        r = rule;
    }

    @Test
    void testPreConfigureMavenJob() throws Exception {
        MavenModuleSet mavenModuleSet = r.jenkins.createProject(MavenModuleSet.class, "test");
        MavenConfigurer configurer = new MavenConfigurer();
        mavenModuleSet.getReporters().remove(MavenMailer.class);
        assertNull(mavenModuleSet.getReporters().get(MavenMailer.class));
        configurer.doCreate(mavenModuleSet);
        assertNotNull(mavenModuleSet.getReporters().get(MavenMailer.class));
    }

    @Test
    void testPreConfigureMavenJob2() throws Exception {
        MavenModuleSet mavenModuleSet = r.jenkins.createProject(MavenModuleSet.class, "test");
        MavenConfigurer configurer = new MavenConfigurer();
        assertNotNull(mavenModuleSet.getReporters().get(MavenMailer.class));
        configurer.doCreate(mavenModuleSet);
    }
}
