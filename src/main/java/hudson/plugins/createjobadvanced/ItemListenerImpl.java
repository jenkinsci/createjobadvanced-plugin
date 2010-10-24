package hudson.plugins.createjobadvanced;

import hudson.Extension;
import hudson.model.Item;
import hudson.model.Hudson;
import hudson.model.Job;
import hudson.model.listeners.ItemListener;
import hudson.security.Permission;
import hudson.security.SecurityMode;
import hudson.security.AuthorizationMatrixProperty;
import hudson.tasks.LogRotator;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.kohsuke.stapler.DataBoundConstructor;

@Extension
public class ItemListenerImpl extends ItemListener {

	static Logger log = Logger.getLogger(CreateJobAdvancedPlugin.class.getName());

	@DataBoundConstructor
	public ItemListenerImpl() {
		// log.info("ItemListenerImpl started");
	}

	@Override
	public void onRenamed(Item item, String oldName, String newName) {
		log.info("renamed " + oldName + " to " + newName);

		if (!(item instanceof Job))
			return;
		final Job<?, ?> job = (Job<?, ?>) item;

		CreateJobAdvancedPlugin cja = Hudson.getInstance().getPlugin(CreateJobAdvancedPlugin.class);
		if (cja.isReplaceSpace()) {
			renameJob(job);
		}
	}

	@Override
	public void onCreated(Item item) {
		if (!(item instanceof Job))
			return;
		final Job<?, ?> job = (Job<?, ?>) item;

		CreateJobAdvancedPlugin cja = Hudson.getInstance().getPlugin(CreateJobAdvancedPlugin.class);

		if (cja.isReplaceSpace()) {
			renameJob(job);
		}

		// hudson must activate security mode for using
		if (!Hudson.getInstance().getSecurity().equals(SecurityMode.UNSECURED)) {

			if (cja.isAutoOwnerRights()) {
				String sid = Hudson.getAuthentication().getName();
				securityGrantPermissions(job, sid, new Permission[] { Item.CONFIGURE, Item.BUILD, Item.READ, Item.DELETE, Item.WORKSPACE });
			}

			if (cja.isAutoPublicBrowse()) {
				securityGrantPermissions(job, "anonymous", new Permission[] { Item.READ, Item.WORKSPACE });
			}

			if (cja.isActiveDynamicPermissions()) {
				securityGrantDynamicPermissions(job, cja);
			}
		}

		if (cja.isActiveLogRotator()) {
			activateLogRotator(job, cja);
		}
	}

	private void securityGrantDynamicPermissions(final Job<?, ?> job, CreateJobAdvancedPlugin cja) {
		String patternStr = cja.getExtractPattern();// com.([A-Z]{3}).(.*)

		List<String> groupsList = new ArrayList<String>();

		if (patternStr != null) {
			Pattern pattern = Pattern.compile(patternStr);
			Matcher matcher = pattern.matcher(job.getName());
			boolean matchFound = matcher.find();

			if (matchFound) {
				// Get all groups for this match
				for (int i = 0; i <= matcher.groupCount(); i++) {
					String groupStr = matcher.group(i);
					log.log(Level.FINE, "groupStr: " + groupStr);
					groupsList.add(groupStr);
				}
			}
		}

		for (DynamicPermissionConfig dpc : cja.getDynamicPermissionConfigs()) {
			MessageFormat format = new MessageFormat(dpc.getGroupFormat());
			final String newName = format.format(groupsList.toArray(new String[0]));
			log.log(Level.FINE, "add perms for group: " + newName);

			final Set<String> permissions = dpc.getCheckedPermissionIds();
			List<Permission> permissionList = new ArrayList<Permission>();
			for (String id : permissions) {
				final Permission permForId = Permission.fromId(id);
				permissionList.add(permForId);
			}

			securityGrantPermissions(job, newName, (Permission[]) permissionList.toArray(new Permission[permissionList.size()]));
		}
	}

	private void activateLogRotator(final Job<?, ?> job, final CreateJobAdvancedPlugin cja) {

		// if template, it's possible that log rotator is already defined
		if (job.getLogRotator() != null) {
			return;
		}

		LogRotator logrotator = new LogRotator(cja.getDaysToKeep(), cja.getNumToKeep(), cja.getArtifactDaysToKeep(), cja.getArtifactNumToKeep());

		job.setLogRotator(logrotator);
	}

	private void renameJob(final Job<?, ?> job) {
		if (job.getName().indexOf(" ") != -1) {
			try {
				job.renameTo(job.getName().replaceAll(" ", "-"));
			} catch (IOException e) {
				log.log(Level.SEVERE, "error during rename", e);
			}
		}
	}

	private void securityGrantPermissions(final Job<?, ?> job, String sid, Permission[] hudsonPermissions) {

		Map<Permission, Set<String>> permissions = initPermissions(job);

		for (Permission perm : hudsonPermissions) {
			configurePermission(permissions, perm, sid);
		}

		try {
			AuthorizationMatrixProperty authProperty = new AuthorizationMatrixProperty(permissions);
			job.addProperty(authProperty);
			log.info("Granding rights to [" + sid + "] for newly-created job " + job.getDisplayName());
		} catch (IOException e) {
			log.log(Level.SEVERE, "problem to add granted permissions", e);
		}
	}

	private Map<Permission, Set<String>> initPermissions(final Job<?, ?> job) {

		Map<Permission, Set<String>> permissions = null;

		// if you create the job with template, need to get informations
		AuthorizationMatrixProperty auth = (AuthorizationMatrixProperty) job.getProperty(AuthorizationMatrixProperty.class);
		if (auth != null) {
			permissions = new HashMap<Permission, Set<String>>(auth.getGrantedPermissions());
			try {
				job.removeProperty(AuthorizationMatrixProperty.class);
			} catch (IOException e) {
				log.log(Level.SEVERE, "problem to remove granted permissions (template or copy job)", e);
			}
		} else {
			permissions = new HashMap<Permission, Set<String>>();
		}

		return permissions;
	}

	private void configurePermission(Map<Permission, Set<String>> permissions, Permission permission, String sid) {

		Set<String> sidPermission = permissions.get(permission);
		if (sidPermission == null) {
			Set<String> sidSet = new HashSet<String>();
			sidSet.add(sid);
			permissions.put(permission, sidSet);
		} else {
			if (!sidPermission.contains(sid)) {
				sidPermission.add(sid);
			}
		}
	}
}