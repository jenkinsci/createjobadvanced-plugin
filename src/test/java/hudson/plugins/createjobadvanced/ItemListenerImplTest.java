package hudson.plugins.createjobadvanced;

import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class ItemListenerImplTest {

    @Rule
    public JenkinsRule r = new JenkinsRule();

    @Test
    public void testConstructorWithMavenPlugin() {
        // Verify that MavenConfigurer is added
        List<ItemListenerImpl> extensions = r.jenkins.getExtensionList(ItemListenerImpl.class);
        ItemListenerImpl itemListener = extensions.get(0);
        List<AbstractConfigurer<?, ?>> configurers = itemListener.getConfigurers();
        boolean hasMavenConfigurer = configurers.stream().anyMatch(configurer -> configurer instanceof MavenConfigurer);
        assert hasMavenConfigurer
                : "MavenConfigurer should be present in configurers list when maven-plugin is installed";
        boolean hasJobConfigurer = configurers.stream().anyMatch(configurer -> configurer instanceof JobConfigurer);
        assert hasJobConfigurer : "JobConfigurer should be present in configurers list";
        boolean hasFolderConfigurer =
                configurers.stream().anyMatch(configurer -> configurer instanceof FolderConfigurer);
        assert hasFolderConfigurer
                : "FolderConfigurer should be present in configurers list when folder-plugin is installed";
    }
}
