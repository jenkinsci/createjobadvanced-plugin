package hudson.plugins.createjobadvanced;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
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
import org.jenkinsci.plugins.matrixauth.PermissionEntry;
import org.jenkinsci.plugins.matrixauth.inheritance.InheritParentStrategy;
import org.jenkinsci.plugins.matrixauth.inheritance.InheritanceStrategy;

/**
 * Partial default implementation of item configurers.
 * <P>
 * Used to apply plugin configuration to items
 *
 * @author Laurent Coltat
 */
public abstract class AbstractConfigurer<T extends AbstractItem, A> {

    /**
     * Plugin logger
     */
    protected static final Logger log = Logger.getLogger(CreateJobAdvancedPlugin.class.getName());

    /**
     * Update given Item according to plugin configuration.
     *
     * @param item item to be updated
     */
    protected void doCreate(Item item) {
        final CreateJobAdvancedPlugin cja = getPlugin();
        if (null == cja) {
            return;
        }
        log.finest("> AbstractConfigurer.onCreated()");

        doRename(item);

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
            securityGrantPermissions(item, PermissionEntry.user(sid), new Permission[] {
                Item.CONFIGURE, Item.BUILD, Item.READ, Item.DELETE, Item.WORKSPACE
            });
        }

        if (cja.isAutoPublicBrowse()) {
            securityGrantPermissions(
                    item, PermissionEntry.user("anonymous"), new Permission[] {Item.READ, Item.WORKSPACE});
        }

        if (cja.isActiveDynamicPermissions()) {
            securityGrantDynamicPermissions(item, cja);
        }
        log.finest("< AbstractConfigurer.onCreated()");
    }

    /**
     * Rename given item if plugin configuration request it.
     *
     * @param item to be renamed
     */
    protected final String doRename(Item item) {
        String resulString = item.getName();
        final CreateJobAdvancedPlugin cja = getPlugin();
        if (null != cja && cja.isReplaceSpace()) {
            if (item.getName().contains(" ")) {
                try {
                    resulString = item.getName().replaceAll(" ", "-");
                    renameJob(item, resulString);
                } catch (IOException e) {
                    log.log(Level.SEVERE, "error during rename", e);
                }
            }
        }
        return resulString;
    }

    /**
     * @return plugin configuration instance.identity/
     */
    protected final CreateJobAdvancedPlugin getPlugin() {
        CreateJobAdvancedPlugin result = null;
        Jenkins instance = Jenkins.getInstanceOrNull();
        if (null != instance) {
            result = instance.getPlugin(CreateJobAdvancedPlugin.class);
        }
        return result;
    }

    /**
     * Grant dynamic group permissions to given item according to plugin configuration.
     *
     * @param item item to be granted
     * @param cja plugin configuration
     */
    private void securityGrantDynamicPermissions(final Item item, CreateJobAdvancedPlugin cja) {
        String patternStr = cja.getExtractPattern(); // com.([A-Z]{3}).(.*)

        List<String> groupsList = new ArrayList<>();

        if (patternStr != null) {
            Pattern pattern = Pattern.compile(patternStr);
            Matcher matcher = pattern.matcher(item.getName());
            boolean matchFound = matcher.find();

            if (matchFound) {
                // Get all groups for this match
                for (int i = 0; i <= matcher.groupCount(); i++) {
                    String groupStr = matcher.group(i);
                    log.log(Level.FINE, "groupStr: {0}", groupStr);
                    groupsList.add(groupStr);
                }
            }
        }

        for (DynamicPermissionConfig dpc : cja.getDynamicPermissionConfigs()) {
            MessageFormat format = new MessageFormat(dpc.getGroupFormat());
            final String newName = format.format(groupsList.toArray(String[]::new));
            log.log(Level.FINEST, "add perms for group: {0}", newName);

            final Set<String> permissions = dpc.getCheckedPermissionIds();
            List<Permission> permissionList = new ArrayList<>();
            for (String id : permissions) {
                final Permission permForId = Permission.fromId(id);
                permissionList.add(permForId);
            }

            securityGrantPermissions(
                    item, PermissionEntry.group(newName), (Permission[]) permissionList.toArray(Permission[]::new));
        }
    }

    /**
     * Grant given Jenkins permissions to given item for given sid of given type.
     *
     * @param item item to be granted
     * @param permEnt Permission entry
     * @param jenkinsPermissions permissions to grant
     */
    protected final void securityGrantPermissions(
            final Item item, PermissionEntry permEnt, Permission[] jenkinsPermissions) {

        Map<Permission, Set<PermissionEntry>> permissions = initPermissions(item);
        for (Permission perm : jenkinsPermissions) {
            configurePermission(permissions, perm, permEnt);
        }
        try {
            A authProperty = setupAuthorizationMatrixProperty(permissions);
            addAuthorizationMatrixProperty(item, authProperty);
        } catch (IOException e) {
            log.log(Level.SEVERE, "problem to add granted permissions", e);
        }
    }

    /**
     * Retrive existing Jenkins permissions map granted to given item.
     *
     * @param item item to be parsed
     * @return Jenkins permissions map granted to given item
     */
    protected final @NonNull Map<Permission, Set<PermissionEntry>> initPermissions(@Nullable Item item) {
        // if you create the job with template, need to get informations
        A auth = getAuthorizationMatrixProperty(item);
        Map<Permission, Set<PermissionEntry>> permissions = getGrantedPermissionEntries(auth);
        removeProperty(item, auth);

        return permissions;
    }

    /**
     * Associates given permission entry to given Jenkins permission, and add it to given permissions map.
     *
     * @param permissions map of Jenkins permissions linked to their assigned permission entries
     * @param permission permission to be configured
     * @param permissionEntry permission entry
     */
    protected final void configurePermission(
            Map<Permission, Set<PermissionEntry>> permissions, Permission permission, PermissionEntry permissionEntry) {

        Set<PermissionEntry> sidPermissionSet = permissions.get(permission);
        if (sidPermissionSet == null) {
            Set<PermissionEntry> sidSet = new HashSet<>();
            sidSet.add(permissionEntry);
            permissions.put(permission, sidSet);
        } else {
            if (!sidPermissionSet.contains(permissionEntry)) {
                sidPermissionSet.add(permissionEntry);
            }
        }
    }

    /**
     * Creates authorization matrix property from given permission map.
     *
     * @param permissions map of permissions linked to their assigned permissions entries
     * @throws IOException
     */
    protected final A setupAuthorizationMatrixProperty(Map<Permission, Set<PermissionEntry>> permissions)
            throws IOException {

        A authProperty = createAuthorizationMatrixProperty();
        setInheritanceStrategy(authProperty, new InheritParentStrategy());
        for (Map.Entry<Permission, Set<PermissionEntry>> entry : permissions.entrySet()) {
            Permission perm = entry.getKey();
            for (PermissionEntry permEntry : entry.getValue()) {
                if (null != perm && null != permEntry && showPermission(perm)) {
                    addPermission(authProperty, perm, permEntry);
                } else {
                    if (null != perm) {
                        log.log(Level.FINER, ": {0}skip hidden permissions {1}", new Object[] {
                            this.getClass().getName(), perm.name
                        });
                    } else {
                        log.log(
                                Level.FINER,
                                ": {0}skip null permission",
                                this.getClass().getName());
                    }
                    if (null == permEntry) {
                        log.log(
                                Level.FINER,
                                ": {0}skip null permission entry",
                                this.getClass().getName());
                    }
                }
            }
        }

        return authProperty;
    }

    /**
     * Assign given inheritance strategy to given authorization matrix property.
     *
     * @param authProperty authorization matrix property to be updated
     * @param inheritanceStrategy inheritance strategy to be assigned
     */
    protected abstract void setInheritanceStrategy(
            @Nullable A authProperty, @Nullable InheritanceStrategy inheritanceStrategy);

    /**
     * Associate given Jenkins permission to given permission entry and assign it to given authorization property.
     *
     * @param authProperty authorization matrix property to be updated
     * @param perm Jenkins permission to be assigned
     * @param permEntry permission entry to be associated
     */
    protected abstract void addPermission(
            @Nullable A authProperty, @Nullable Permission perm, @Nullable PermissionEntry permEntry);

    /**
     * Check if given Jenkins permission is handled by this object.
     *
     * @param perm Jenkins permission
     * @return true if parameter is available
     */
    protected abstract boolean showPermission(@Nullable Permission perm);

    /**
     * Assigne given authorization matrix property to given item.
     *
     * @param item Item to be updated
     * @param authProperty authorization matrix property to be assigned
     * @throws IOException
     */
    protected abstract void addAuthorizationMatrixProperty(@Nullable Item item, @Nullable A authProperty)
            throws IOException;

    /**
     * Rename given item with given name.
     *
     * @param item Item to be updated
     * @param newName New name to be assigned
     * @throws IOException
     */
    protected abstract void renameJob(@Nullable Item item, @Nullable String newName) throws IOException;

    /**
     * Create a fresh new authorization matrix property.
     *
     * @return created authorization matrix property
     */
    protected abstract @Nullable A createAuthorizationMatrixProperty();

    /**
     * Fetch autorization matrix property from given item.
     *
     * @param item owner of the autorization matrix property to be fetched
     * @return autorization matrix property of given item if any, null otherwize
     */
    protected abstract @Nullable A getAuthorizationMatrixProperty(@Nullable Item item);

    /**
     * Remove given authorization matrix from given item.
     *
     * @param item item to be updated
     * @param authProperty authorization matrix to be removed
     */
    protected abstract void removeProperty(@Nullable Item item, @Nullable A authProperty);

    /**
     * Fetch association of Jenkins permissions to permission entries from given authorization matrix.auth.
     *
     * @param authProperty authorization matrix to be updated.
     * @return a Map of Jenkins permissions linked to their assigned permissions entries.
     */
    protected abstract @NonNull Map<Permission, Set<PermissionEntry>> getGrantedPermissionEntries(
            @Nullable A authProperty);
}
