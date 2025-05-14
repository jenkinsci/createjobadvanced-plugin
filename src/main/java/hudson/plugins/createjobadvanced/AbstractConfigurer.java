package hudson.plugins.createjobadvanced;

import hudson.model.AbstractItem;
import hudson.model.Hudson;
import hudson.model.Item;
import hudson.security.Permission;
import hudson.security.SecurityMode;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
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
import org.jenkinsci.plugins.matrixauth.inheritance.InheritanceStrategy;

public abstract class AbstractConfigurer<T extends AbstractItem, A> {
    protected static final Logger log = Logger.getLogger(CreateJobAdvancedPlugin.class.getName());

    protected void onCreated(Item item) {
        final CreateJobAdvancedPlugin cja = getPlugin();
        if (null == cja) {
            return;
        }
        log.finest("> AbstractConfigurer.onCreated()");

        onRenamed(item);

        // hudson must activate security mode for using
        Jenkins jenkinsInstance = Hudson.getInstanceOrNull();
        if (jenkinsInstance == null
                || jenkinsInstance.getSecurity() == null
                || jenkinsInstance.getSecurity().equals(SecurityMode.UNSECURED)) {
            log.warning("Jenkins security mode disabled.");
            log.finest("< AbstractConfigurer.onCreated()");
            return;
        }

        if (cja.isAutoOwnerRights()) {
            String sid = Hudson.getAuthentication2().getName();
            securityGrantPermissions(
                    item,
                    sid,
                    new Permission[] {Item.CONFIGURE, Item.BUILD, Item.READ, Item.DELETE, Item.WORKSPACE},
                    AuthorizationType.USER);
        }

        if (cja.isAutoPublicBrowse()) {
            securityGrantPermissions(
                    item, "anonymous", new Permission[] {Item.READ, Item.WORKSPACE}, AuthorizationType.USER);
        }

        if (cja.isActiveDynamicPermissions()) {
            securityGrantDynamicPermissions(item, cja);
        }
        log.finest("< AbstractConfigurer.onCreated()");
    }

    protected final void onRenamed(Item item) {
        final CreateJobAdvancedPlugin cja = getPlugin();
        if (null == cja) {
            return;
        }
        log.finest("> AbstractConfigurer.onRenamed()");
        if (cja.isReplaceSpace()) {
            if (item.getName().indexOf(" ") != -1) {
                try {
                    renameJob(item, item.getName().replaceAll(" ", "-"));
                } catch (IOException e) {
                    log.log(Level.SEVERE, "error during rename", e);
                }
            }
        }
        log.finest("< AbstractConfigurer.onRenamed()");
    }

    protected final CreateJobAdvancedPlugin getPlugin() {
        Jenkins instance = Jenkins.getInstanceOrNull();
        if (null == instance) {
            log.warning("Jenkins instance is null");
            return null;
        }
        CreateJobAdvancedPlugin result = instance.getPlugin(CreateJobAdvancedPlugin.class);
        if (null == result) {
            log.warning("CreateJobAdvancedPlugin is null");
        }
        return result;
    }

    private final void securityGrantDynamicPermissions(final Item abstractItem, CreateJobAdvancedPlugin cja) {
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
                    log.fine("groupStr: " + groupStr);
                    groupsList.add(groupStr);
                }
            }
        }

        for (DynamicPermissionConfig dpc : cja.getDynamicPermissionConfigs()) {
            MessageFormat format = new MessageFormat(dpc.getGroupFormat());
            final String newName = format.format(groupsList.toArray(new String[0]));
            log.finest("add perms for group: " + newName);

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

    protected final void securityGrantPermissions(
            final Item item, String sid, Permission[] hudsonPermissions, AuthorizationType type) {
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

        Map<Permission, Set<PermissionEntry>> permissions = initPermissions(item);
        for (Permission perm : hudsonPermissions) {
            configurePermission(permissions, perm, permEnt);
        }
        try {
            addAuthorizationMatrixProperty(item, permissions);
        } catch (IOException e) {
            log.log(Level.SEVERE, "problem to add granted permissions", e);
        }
    }

    protected final Map<Permission, Set<PermissionEntry>> initPermissions(Item item) {
        // if you create the job with template, need to get informations
        A auth = getAuthorizationMatrixProperty(item);
        Map<Permission, Set<PermissionEntry>> permissions = getGrantedPermissionEntries(auth);
        removeProperty(item, auth);

        return permissions;
    }

    protected final void configurePermission(
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

    protected final void addAuthorizationMatrixProperty(Item item, Map<Permission, Set<PermissionEntry>> permissions)
            throws IOException {
        log.finer("> " + this.getClass().getName()
                + ".addAuthorizationMatrixProperty(Item, Map<Permission, Set<PermissionEntry>>)");

        A authProperty = createAuthorizationMatrixProperty();
        setInheritanceStrategy(authProperty, new InheritParentStrategy());
        for (Map.Entry<Permission, Set<PermissionEntry>> entry : permissions.entrySet()) {
            Permission perm = entry.getKey();
            for (PermissionEntry permEntry : entry.getValue()) {
                if (null != perm && null != permEntry && showPermission(perm)) {
                    addPermission(authProperty, perm, permEntry);
                } else {
                    if (null != perm) {
                        log.finer(": " + this.getClass().getName() + "skip hidden permissions " + perm.name);
                    } else {
                        log.finer(": " + this.getClass().getName() + "skip null permission");
                    }
                    if (null == permEntry) {
                        log.finer(": " + this.getClass().getName() + "skip null permission entry");
                    }
                }
            }
        }
        addAuthorizationMatrixProperty(item, authProperty);
        log.finer("< " + this.getClass().getName()
                + ".addAuthorizationMatrixProperty(Item, Map<Permission, Set<PermissionEntry>>)");
    }

    protected abstract void setInheritanceStrategy(A authProperty, InheritanceStrategy inheritanceStrategy);

    protected abstract void addPermission(A authProperty, Permission perm, PermissionEntry permEntry);

    protected abstract boolean showPermission(Permission perm);

    protected abstract void addAuthorizationMatrixProperty(Item item, A authProperty) throws IOException;

    protected abstract void renameJob(Item item, String newName) throws IOException;

    /*protected abstract void addAuthorizationMatrixProperty(Item item, Map<Permission, Set<PermissionEntry>> permissions)
    throws IOException;*/
    protected abstract A createAuthorizationMatrixProperty();

    protected abstract A getAuthorizationMatrixProperty(Item item);

    protected abstract boolean removeProperty(Item item, A authProperty);

    protected abstract Map<Permission, Set<PermissionEntry>> getGrantedPermissionEntries(A authProperty);
}
