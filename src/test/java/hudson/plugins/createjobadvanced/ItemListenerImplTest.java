package hudson.plugins.createjobadvanced;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class ItemListenerImplTest {

    private JenkinsRule r;

    @BeforeEach
    void setUp(JenkinsRule rule) {
        r = rule;
    }

    @Test
    void testConstructorWithMavenPlugin() {
        // Verify that MavenConfigurer is added
        List<ItemListenerImpl> extensions = r.jenkins.getExtensionList(ItemListenerImpl.class);
        ItemListenerImpl itemListener = extensions.get(0);
        List<AbstractConfigurer<?, ?>> configurers = itemListener.getConfigurers();
        boolean hasMavenConfigurer = configurers.stream().anyMatch(configurer -> configurer instanceof MavenConfigurer);
        assertTrue(
                hasMavenConfigurer,
                "MavenConfigurer should be present in configurers list when maven-plugin is installed");
        boolean hasJobConfigurer = configurers.stream().anyMatch(configurer -> configurer instanceof JobConfigurer);
        assertTrue(hasJobConfigurer, "JobConfigurer should be present in configurers list");
        boolean hasFolderConfigurer =
                configurers.stream().anyMatch(configurer -> configurer instanceof FolderConfigurer);
        assertTrue(
                hasFolderConfigurer,
                "FolderConfigurer should be present in configurers list when folder-plugin is installed");
    }
}
