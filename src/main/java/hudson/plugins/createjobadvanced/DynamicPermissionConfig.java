package hudson.plugins.createjobadvanced;

import hudson.security.Permission;
import java.util.HashSet;
import java.util.Set;
import org.kohsuke.stapler.DataBoundConstructor;

public class DynamicPermissionConfig {
    private String groupFormat = null;

    private Set<String> checkedPermissionIds = new HashSet<>();

    @DataBoundConstructor
    public DynamicPermissionConfig(String groupFormat, Set<String> checkedPermissionIds) {
        this.groupFormat = groupFormat;
        if (checkedPermissionIds != null) {
            this.checkedPermissionIds = checkedPermissionIds;
        }
    }

    /**
     * Add given permission ID to checked permission set.
     *
     * @param permissionId permission ID to be added to checked permission IDs set.
     */
    public void addPermissionId(String permissionId) {
        checkedPermissionIds.add(permissionId);
    }

    /**
     * @return the groupFormat
     */
    public String getGroupFormat() {
        return groupFormat;
    }

    /**
     * @return the checked permission IDs set
     */
    public Set<String> getCheckedPermissionIds() {
        return checkedPermissionIds;
    }

    /**
     * Check given permission state.
     *
     * @param permission
     * @return true if given permission is checked, false otherwise
     */
    public boolean isPermissionChecked(Permission permission) {
        return checkedPermissionIds.contains(permission.getId());
    }

    @Override
    public String toString() {
        return "[DynamicPermissionConfig: " + groupFormat + ", permissions: " + checkedPermissionIds + "]";
    }
}
