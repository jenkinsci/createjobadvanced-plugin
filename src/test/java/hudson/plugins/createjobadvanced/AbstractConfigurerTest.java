package hudson.plugins.createjobadvanced;

import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class AbstractConfigurerTest {

    @Test
    void nullPluginTest() {
        MavenConfigurer configurer = new MavenConfigurer();
        assertNull(configurer.getPlugin());
    }
}
