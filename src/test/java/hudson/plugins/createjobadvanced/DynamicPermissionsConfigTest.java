package hudson.plugins.createjobadvanced;

import java.util.HashSet;
import org.junit.Assert;
import org.junit.Test;

public class DynamicPermissionsConfigTest {

    @Test
    public void testDynamicPermissionConfig() {
        // This is a placeholder for the actual test implementation.
        // You would typically create instances of DynamicPermissionConfig,
        // set properties, and assert expected behaviors or values.

        // Example:
        DynamicPermissionConfig config = new DynamicPermissionConfig("groupFormat", new HashSet<>());
        Assert.assertEquals("groupFormat", config.getGroupFormat());
        Assert.assertTrue(config.getCheckedPermissionIds().isEmpty());
        config.addPermissionId("hudson.model.Item.Read");
        Assert.assertTrue(config.getCheckedPermissionIds().contains("hudson.model.Item.Read"));
        Assert.assertTrue(config.isPermissionChecked(hudson.model.Item.READ));
        Assert.assertFalse(config.isPermissionChecked(hudson.model.Item.CREATE));
        Assert.assertEquals(
                "[DynamicPermissionConfig: groupFormat, permissions: [hudson.model.Item.Read]]", config.toString());
        config = new DynamicPermissionConfig(null, null);
        Assert.assertNull(config.getGroupFormat());
        Assert.assertTrue(config.getCheckedPermissionIds().isEmpty());
        Assert.assertEquals("[DynamicPermissionConfig: null, permissions: []]", config.toString());
    }
}
