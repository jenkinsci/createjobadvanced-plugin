package hudson.plugins.createjobadvanced;

import hudson.security.Permission;

import java.util.HashSet;
import java.util.Set;

import org.kohsuke.stapler.DataBoundConstructor;

public class DynamicPermissionConfig {
	private String groupFormat;

	private Set<String> checkedPermissionIds = new HashSet<String>();

	@DataBoundConstructor
	public DynamicPermissionConfig(String groupFormat, Set<String> checkedPermissionIds) {
		this.groupFormat = groupFormat;
		if (checkedPermissionIds != null) {
			this.checkedPermissionIds = checkedPermissionIds;
		}
	}

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
	 * @return the checkedPermissionIds
	 */
	public Set<String> getCheckedPermissionIds() {
		return checkedPermissionIds;
	}

	public boolean isPermissionChecked(Permission permission) {
		return checkedPermissionIds.contains(permission.getId());
	}

	@Override
	public String toString() {
		return "[DynamicPermissionConfig: " + groupFormat + ", permissions: " + checkedPermissionIds + "]";
	}
}
