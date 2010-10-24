package hudson.plugins.createjobadvanced;

import hudson.Plugin;
import hudson.Util;
import hudson.model.Descriptor.FormException;
import hudson.security.Permission;
import hudson.security.PermissionGroup;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.kohsuke.stapler.StaplerRequest;

/**
 * @plugin
 * @author Bertrand Gressier
 * 
 */

public class CreateJobAdvancedPlugin extends Plugin {

	static Logger log = Logger.getLogger(CreateJobAdvancedPlugin.class.getName());

	private boolean autoOwnerRights;
	private boolean autoPublicBrowse;
	private boolean replaceSpace;

	private boolean activeLogRotator;
	private int daysToKeep = -1;
	private int numToKeep = -1;
	private int artifactDaysToKeep = -1;
	private int artifactNumToKeep = -1;

	private boolean activeDynamicPermissions;
	private String extractPattern;

	private List<DynamicPermissionConfig> dynamicPermissionConfigs = new ArrayList<DynamicPermissionConfig>();

	/**
	 * @return the dynamicPermissionConfigs
	 */
	public List<DynamicPermissionConfig> getDynamicPermissionConfigs() {
		return dynamicPermissionConfigs;
	}

	public CreateJobAdvancedPlugin() {
	}

	@Override
	public void start() throws Exception {
		super.start();
		log.info("Create job advanced plugin started ...");
		load();
	}

	@Override
	public void configure(StaplerRequest req, JSONObject formData) throws IOException, ServletException, FormException {

		// formData.optBoolean("autoOwnerRights",autoOwnerRights);

		if (req.getParameter("cja.security") == null || req.getParameter("cja.security") == "false") {
			autoOwnerRights = false;
		} else {
			autoOwnerRights = true;
		}

		if (req.getParameter("cja.public") == null || req.getParameter("cja.public") == "false") {
			autoPublicBrowse = false;
		} else {
			autoPublicBrowse = true;
		}

		if (req.getParameter("cja.jobspacesinname") == null || req.getParameter("cja.jobspacesinname") == "false") {
			replaceSpace = false;
		} else {
			replaceSpace = true;
		}

		if (req.getParameter("cja.activeLogRotator") == null || req.getParameter("cja.activeLogRotator") == "false") {
			activeLogRotator = false;
		} else {
			activeLogRotator = true;
		}

		if (activeLogRotator) {

			try {
				daysToKeep = Integer.valueOf(Util.fixNull(req.getParameter("cja.daysToKeep")));
			} catch (Exception e) {
				daysToKeep = -1;
			}
			try {
				numToKeep = Integer.valueOf(Util.fixNull(req.getParameter("cja.numToKeep")));
			} catch (Exception e) {
				numToKeep = -1;
			}
			try {
				artifactDaysToKeep = Integer.valueOf(Util.fixNull(req.getParameter("cja.artifactDaysToKeep")));
			} catch (Exception e) {
				artifactDaysToKeep = -1;
			}
			try {
				artifactNumToKeep = Integer.valueOf(Util.fixNull(req.getParameter("cja.artifactNumToKeep")));
			} catch (Exception e) {
				artifactNumToKeep = -1;
			}
		}

		if (req.getParameter("cja.activeDynamicPermissions") == null || req.getParameter("cja.activeDynamicPermissions") == "false") {
			activeDynamicPermissions = false;
		} else {
			activeDynamicPermissions = true;
		}

		if (activeDynamicPermissions) {
			if (req.getParameter("cja.extractPattern") != null) {
				extractPattern = req.getParameter("cja.extractPattern");
			}
			for (Object o : JSONArray.fromObject(formData.get("activeDynamicPermissions"))) {
				JSONObject jo = (JSONObject) o;

				dynamicPermissionConfigs.clear();
				final Object cfgs = jo.get("cfgs");
				if (cfgs instanceof JSONArray) {
					final JSONArray jsonArray = (JSONArray) cfgs;
					for (Object object : jsonArray) {
						addDynamicPermission(req, (JSONObject) object);
					}
				} else {
					// there might be only one single dynamic permission
					addDynamicPermission(req, (JSONObject) cfgs);
				}

			}

		}

		save();
	}

	/**
	 * adds a dynamic permission configuration with the data extracted form the
	 * jsonObject.
	 * 
	 */
	private void addDynamicPermission(StaplerRequest req, JSONObject jsonObject) {
		final DynamicPermissionConfig dynPerm = req.bindJSON(DynamicPermissionConfig.class, jsonObject);

		// add the enabled permission ids
		final List<Permission> allPossiblePermissions = getAllPossiblePermissions();
		for (Permission permission : allPossiblePermissions) {
			final String enabled = jsonObject.getString(permission.getId());
			if (Boolean.valueOf(enabled)) {
				dynPerm.addPermissionId(permission.getId());
				log.log(Level.FINE, "enable {0}", new String[] { permission.getId() });
			}
		}

		dynamicPermissionConfigs.add(dynPerm);
	}

	public static List<Permission> getAllPossiblePermissions() {
		final List<Permission> enabledPerms = new ArrayList<Permission>();

		addEnabledPermissionsForGroup(enabledPerms, hudson.model.Item.class);
		addEnabledPermissionsForGroup(enabledPerms, hudson.model.Run.class);

		return enabledPerms;
	}

	private static void addEnabledPermissionsForGroup(final List<Permission> enabledPerms, Class owner) {
		final PermissionGroup permissionGroup = PermissionGroup.get(owner);
		final List<Permission> permissions = permissionGroup.getPermissions();
		for (Permission permission : permissions) {
			if (permission.enabled) {
				enabledPerms.add(permission);
			}
		}
	}

	public boolean isAutoOwnerRights() {
		return autoOwnerRights;
	}

	public boolean isAutoPublicBrowse() {
		return autoPublicBrowse;
	}

	public boolean isReplaceSpace() {
		return replaceSpace;
	}

	public boolean isActiveLogRotator() {
		return activeLogRotator;
	}

	public int getDaysToKeep() {
		return daysToKeep;
	}

	public int getNumToKeep() {
		return numToKeep;
	}

	public int getArtifactDaysToKeep() {
		return artifactDaysToKeep;
	}

	public int getArtifactNumToKeep() {
		return artifactNumToKeep;
	}

	/**
	 * @return the extractPattern
	 */
	public String getExtractPattern() {
		return extractPattern;
	}

	/**
	 * @return the activeDynamicPermissions
	 */
	public boolean isActiveDynamicPermissions() {
		return activeDynamicPermissions;
	}

}
