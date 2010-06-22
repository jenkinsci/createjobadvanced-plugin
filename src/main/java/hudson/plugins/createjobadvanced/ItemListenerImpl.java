package hudson.plugins.createjobadvanced;

import hudson.Extension;
import hudson.model.Hudson;
import hudson.model.Item;
import hudson.model.Job;
import hudson.model.listeners.ItemListener;
import hudson.security.AuthorizationMatrixProperty;
import hudson.security.Permission;
import hudson.security.SecurityMode;
import hudson.tasks.LogRotator;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.kohsuke.stapler.DataBoundConstructor;

@Extension
public class ItemListenerImpl extends ItemListener {

	static Logger log = Logger.getLogger(CreateJobAdvancedPlugin.class.getName());

	@DataBoundConstructor
	public ItemListenerImpl() {
		// log.info("ItemListenerImpl started");
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onRenamed(Item item, String oldName, String newName) {
		log.info("renamed " + oldName + " to " + newName);

		if (!(item instanceof Job))
			return;
		final Job job = (Job) item;

		CreateJobAdvancedPlugin cja = Hudson.getInstance().getPlugin(CreateJobAdvancedPlugin.class);
		if (cja.isReplaceSpace()) {
			renameJob(job);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onCreated(Item item) {

		if (!(item instanceof Job))
			return;
		final Job job = (Job) item;

		CreateJobAdvancedPlugin cja = Hudson.getInstance().getPlugin(CreateJobAdvancedPlugin.class);

		if (cja.isReplaceSpace()) {
			renameJob(job);
		}
		
		// hudson must activate security mode for using
		if (!Hudson.getInstance().getSecurity().equals(SecurityMode.UNSECURED)) {
		
			if (cja.isAutoOwnerRights()) {
				String sid  = Hudson.getAuthentication().getName();
				securityGrantPermissions(job, sid,  Item.CONFIGURE,Item.BUILD, Item.READ,Item.DELETE, Item.WORKSPACE );
			}
	
			if (cja.isAutoPublicBrowse()) {
				securityGrantPermissions(job, "anonymous",Item.READ,Item.WORKSPACE);
			}
		}

		if (cja.isActiveLogRotator()) {
			activateLogRotator(job, cja);
		}
	}

	@SuppressWarnings("unchecked")
	private void activateLogRotator(final Job job, final CreateJobAdvancedPlugin cja) {

		// if template, it's possible that log rotator is already defined
		if (job.getLogRotator() != null) {
			return;
		}

		LogRotator logrotator = new LogRotator(cja.getDaysToKeep(), cja.getNumToKeep(), cja.getArtifactDaysToKeep(),
				cja.getArtifactNumToKeep());

		job.setLogRotator(logrotator);
	}

	@SuppressWarnings("unchecked")
	private void renameJob(final Job job) {
		if (job.getName().indexOf(" ") != -1) {
			try {
				job.renameTo(job.getName().replaceAll(" ", "-"));
			} catch (IOException e) {
				log.log(Level.SEVERE, "error during rename", e);
			}
		}
	}

	
	
	@SuppressWarnings("unchecked")
	private void securityGrantPermissions(final Job job, String sid, Permission ... hudsonPermissions) {
		
		Map<Permission, Set<String>> permissions = initPermissions(job);

		for(Permission perm : hudsonPermissions) {
			configurePermission(permissions,perm, sid);
		}
		
		try {
			AuthorizationMatrixProperty authProperty = new AuthorizationMatrixProperty(permissions);
			job.addProperty(authProperty);
			log.info("Granding rights to [" + sid + "] for newly-created job " + job.getDisplayName());
		} catch (IOException e) {
			log.log(Level.SEVERE, "problem to add granted permissions", e);
		}
	}
	
	@SuppressWarnings("unchecked")
	private Map<Permission, Set<String>> initPermissions(final Job job){
		
		Map<Permission, Set<String>> permissions = null;

		// if you create the job with template, need to get informations
		AuthorizationMatrixProperty auth = (AuthorizationMatrixProperty) job
				.getProperty(AuthorizationMatrixProperty.class);
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