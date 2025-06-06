package hudson.plugins.createjobadvanced;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import org.junit.jupiter.api.Test;

class DynamicPermissionsConfigTest {

    @Test
    void testDynamicPermissionConfig() {
        // This is a placeholder for the actual test implementation.
        // You would typically create instances of DynamicPermissionConfig,
        // set properties, and assert expected behaviors or values.

        // Example:
        DynamicPermissionConfig config = new DynamicPermissionConfig("groupFormat", new HashSet<>());
        assertEquals("groupFormat", config.getGroupFormat());
        assertTrue(config.getCheckedPermissionIds().isEmpty());
        config.addPermissionId("hudson.model.Item.Read");
        assertTrue(config.getCheckedPermissionIds().contains("hudson.model.Item.Read"));
        assertTrue(config.isPermissionChecked(hudson.model.Item.READ));
        assertFalse(config.isPermissionChecked(hudson.model.Item.CREATE));
        assertEquals(
                "[DynamicPermissionConfig: groupFormat, permissions: [hudson.model.Item.Read]]", config.toString());
        config = new DynamicPermissionConfig(null, null);
        assertNull(config.getGroupFormat());
        assertTrue(config.getCheckedPermissionIds().isEmpty());
        assertEquals("[DynamicPermissionConfig: null, permissions: []]", config.toString());
    }
}
