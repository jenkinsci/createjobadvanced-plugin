package hudson.plugins.createjobadvanced;

import org.junit.Assert;
import org.junit.Test;

public class AbstractConfigurerTest {
    @Test
    public void nullPluginTest() {
        MavenConfigurer configurer = new MavenConfigurer();
        Assert.assertNull(configurer.getPlugin());
    }
}
