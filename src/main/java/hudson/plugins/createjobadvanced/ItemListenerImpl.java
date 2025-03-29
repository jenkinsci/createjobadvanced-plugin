package hudson.plugins.createjobadvanced;

import com.cloudbees.hudson.plugins.folder.AbstractFolder;
import hudson.Extension;
import hudson.model.AbstractItem;
import hudson.model.Hudson;
import hudson.model.Item;
import hudson.model.Job;
import hudson.model.listeners.ItemListener;
import hudson.security.AuthorizationMatrixProperty;
import hudson.security.Permission;
import hudson.security.SecurityMode;
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
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.matrixauth.AuthorizationType;
import org.jenkinsci.plugins.matrixauth.PermissionEntry;
import org.jenkinsci.plugins.matrixauth.inheritance.InheritParentStrategy;
import org.kohsuke.stapler.DataBoundConstructor;

@Extension
public class ItemListenerImpl extends ItemListener {

    private static final Logger log = Logger.getLogger(CreateJobAdvancedPlugin.class.getName());

    private MavenConfigurer mavenConfigurer = null;

    @DataBoundConstructor
    public ItemListenerImpl() {
        Jenkins instance = Jenkins.getInstanceOrNull();
        if (instance != null && instance.getPlugin("maven-plugin") != null) {
                mavenConfigurer = new MavenConfigurer();
        }
    }

    @Override
    public void onRenamed(Item item, String oldName, String newName) {
        log.info("renamed " + oldName + " to " + newName);

        if (!(item instanceof Job || item instanceof AbstractFolder)) return;
        final AbstractItem abstractItem = (AbstractItem) item;

        CreateJobAdvancedPlugin cja = getPlugin();
        if (cja.isReplaceSpace()) {
            renameJob(abstractItem);
        }
    }

    private CreateJobAdvancedPlugin getPlugin() {
        CreateJobAdvancedPlugin result = null;
        Jenkins instance = Jenkins.getInstanceOrNull();
        if(instance != null) {
            result = instance.getPlugin(CreateJobAdvancedPlugin.class);
        }
        return result;
    }

    @Override
    public void onCreated(Item item) {
        log.finer("> ItemListenerImpl.onCreated()");
        if (!(item instanceof Job || item instanceof AbstractFolder)) {
            return;
        }
        final AbstractItem abstractItem = (AbstractItem) item;

        CreateJobAdvancedPlugin cja = getPlugin();

        if (cja.isReplaceSpace()) {
            renameJob(abstractItem);
        }

        // hudson must activate security mode for using
        Jenkins instance = Jenkins.getInstanceOrNull();
        if(instance != null) {
            SecurityMode security = instance.getSecurity();
            if(security != null && !security.equals(SecurityMode.UNSECURED)) {
                if (cja.isAutoOwnerRights()) {
                    String sid = Hudson.getAuthentication2().getName();
                    securityGrantPermissions(
                            abstractItem,
                            sid,
                            new Permission[] {Item.CONFIGURE, Item.BUILD, Item.READ, Item.DELETE, Item.WORKSPACE},
                            AuthorizationType.USER);
                }
        
                if (cja.isAutoPublicBrowse()) {
                    securityGrantPermissions(
                            abstractItem,
                            "anonymous",
                            new Permission[] {Item.READ, Item.WORKSPACE},
                            AuthorizationType.USER);
                }
        
                if (cja.isActiveDynamicPermissions()) {
                    securityGrantDynamicPermissions(abstractItem, cja);
                }
            }

        }

        if (cja.isActiveLogRotator() && item instanceof Job) {
            activateLogRotator((Job<?, ?>) abstractItem, cja);
        }

        if (mavenConfigurer != null && item instanceof Job) {
            mavenConfigurer.onCreated((Job<?, ?>) abstractItem);
        }

        log.finer("< ItemListenerImpl.onCreated()");
    }

    private void securityGrantDynamicPermissions(final AbstractItem abstractItem, CreateJobAdvancedPlugin cja) {
        String patternStr = cja.getExtractPattern(); // com.([A-Z]{3}).(.*)

        List<String> groupsList = new ArrayList<String>();

        if (patternStr != null) {
            Pattern pattern = Pattern.compile(patternStr);
            Matcher matcher = pattern.matcher(abstractItem.getName());
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

            securityGrantPermissions(
                    abstractItem,
                    newName,
                    (Permission[]) permissionList.toArray(new Permission[permissionList.size()]),
                    AuthorizationType.GROUP);
        }
    }

    private void activateLogRotator(final Job<?, ?> job, final CreateJobAdvancedPlugin cja) {

        // if template, it's possible that log rotator is already defined
        if (job.getBuildDiscarder() != null) {
            return;
        }

        LogRotator logrotator = new LogRotator(
                cja.getDaysToKeep(), cja.getNumToKeep(), cja.getArtifactDaysToKeep(), cja.getArtifactNumToKeep());

        try {
            // with 1.503, the signature changed and might now throw an IOException
            job.setBuildDiscarder(logrotator);
        } catch (Exception e) {
            log.log(Level.SEVERE, "error setting Logrotater", e);
        }
    }

    private void renameJob(final AbstractItem abstractItem) {
        try {
            if (abstractItem.getName().indexOf(" ") != -1) {
                if (abstractItem instanceof Job) {
                    renameJob((Job<?, ?>) abstractItem);
                } else {
                    renameJob((AbstractFolder<?>) abstractItem);
                }
            }
        } catch (IOException e) {
            log.log(Level.SEVERE, "error during rename", e);
        }
    }

    private void renameJob(final Job<?, ?> job) throws IOException {
        job.renameTo(job.getName().replaceAll(" ", "-"));
    }

    private void renameJob(final AbstractFolder<?> folder) throws IOException {
        folder.renameTo(folder.getName().replaceAll(" ", "-"));
    }

    private void securityGrantPermissions(
            final AbstractItem abstractItem, String sid, Permission[] hudsonPermissions, AuthorizationType type) {
        PermissionEntry permEnt = new PermissionEntry(AuthorizationType.EITHER, sid);
        switch (type) {
            case USER:
                permEnt = PermissionEntry.user(sid);
                break;
            case GROUP:
                permEnt = PermissionEntry.group(sid);
                break;
            case EITHER:
                break;
        }

        Map<Permission, Set<PermissionEntry>> permissions = initPermissions(abstractItem);
        for (Permission perm : hudsonPermissions) {
            configurePermission(permissions, perm, permEnt);
        }
        try {
            if (abstractItem instanceof Job) {
                addAuthorizationMatrixProperty((Job<?, ?>) abstractItem, permissions);
                log.info("Granting rights to [" + sid + "] for newly-created job " + abstractItem.getDisplayName());
            } else {
                addAuthorizationMatrixProperty((AbstractFolder<?>) abstractItem, permissions);
                log.info("Granting rights to [" + sid + "] for newly-created folder " + abstractItem.getDisplayName());
            }
        } catch (IOException e) {
            log.log(Level.SEVERE, "problem to add granted permissions", e);
        }
    }

    private void addAuthorizationMatrixProperty(Job<?, ?> job, Map<Permission, Set<PermissionEntry>> permissions)
            throws IOException {
        AuthorizationMatrixProperty authProperty =
                new AuthorizationMatrixProperty(permissions, new InheritParentStrategy());
        job.addProperty(authProperty);
    }

    private void addAuthorizationMatrixProperty(
            AbstractFolder<?> folder, Map<Permission, Set<PermissionEntry>> permissions) throws IOException {
        Jenkins instance = Jenkins.getInstanceOrNull();
        if(instance != null) {
            com.cloudbees.hudson.plugins.folder.properties.AuthorizationMatrixProperty.DescriptorImpl propDescriptor =
                (com.cloudbees.hudson.plugins.folder.properties.AuthorizationMatrixProperty.DescriptorImpl)                        
                    instance.getDescriptor(
                   com.cloudbees.hudson.plugins.folder.properties.AuthorizationMatrixProperty.class
                    );
            if(propDescriptor != null && folder != null) {
                com.cloudbees.hudson.plugins.folder.properties.AuthorizationMatrixProperty authProperty =
                    propDescriptor.create();
                if(authProperty != null) {
                    for(Map.Entry<Permission, Set<PermissionEntry>> permission : permissions.entrySet()) {
                        for (PermissionEntry permEntry : permission.getValue()) {
                            authProperty.add(permission.getKey(), permEntry);
                        }
                    }
                    folder.addProperty(authProperty);
                }
            }
        }
    }

    private Map<Permission, Set<PermissionEntry>> initPermissions(final AbstractItem abstractItem) {

        Map<Permission, Set<PermissionEntry>> permissions = null;
        if (abstractItem instanceof Job) {
            permissions = initPermissions((Job<?, ?>) abstractItem);
        } else {
            permissions = initPermissions((AbstractFolder<?>) abstractItem);
        }

        return permissions;
    }

    private Map<Permission, Set<PermissionEntry>> initPermissions(final Job<?, ?> job) {

        Map<Permission, Set<PermissionEntry>> permissions = null;

        // if you create the job with template, need to get informations
        AuthorizationMatrixProperty auth =
                (AuthorizationMatrixProperty) job.getProperty(AuthorizationMatrixProperty.class);
        if (auth != null) {
            permissions = new HashMap<Permission, Set<PermissionEntry>>(auth.getGrantedPermissionEntries());
            try {
                job.removeProperty(AuthorizationMatrixProperty.class);
            } catch (IOException e) {
                log.log(Level.SEVERE, "problem to remove granted permissions (template or copy job)", e);
            }
        } else {
            permissions = new HashMap<Permission, Set<PermissionEntry>>();
        }

        return permissions;
    }

    private Map<Permission, Set<PermissionEntry>> initPermissions(final AbstractFolder<?> folder) {

        Map<Permission, Set<PermissionEntry>> permissions = null;

        com.cloudbees.hudson.plugins.folder.properties.AuthorizationMatrixProperty authProperty = folder.getProperties()
                .get(com.cloudbees.hudson.plugins.folder.properties.AuthorizationMatrixProperty.class);
        if (authProperty != null) {
            permissions = new HashMap<Permission, Set<PermissionEntry>>(authProperty.getGrantedPermissionEntries());
            List<?> folderProperties = folder.getProperties();
            folderProperties.remove(authProperty);
        } else {
            permissions = new HashMap<Permission, Set<PermissionEntry>>();
        }

        return permissions;
    }

    private void configurePermission(
            Map<Permission, Set<PermissionEntry>> permissions, Permission permission, PermissionEntry sid) {

        Set<PermissionEntry> sidPermission = permissions.get(permission);
        if (sidPermission == null) {
            Set<PermissionEntry> sidSet = new HashSet<PermissionEntry>();

            sidSet.add(sid);
            permissions.put(permission, sidSet);
        } else {
            if (!sidPermission.contains(sid)) {
                sidPermission.add(sid);
            }
        }
    }
}
