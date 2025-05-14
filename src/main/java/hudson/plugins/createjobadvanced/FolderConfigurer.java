package hudson.plugins.createjobadvanced;

import com.cloudbees.hudson.plugins.folder.AbstractFolder;
import com.cloudbees.hudson.plugins.folder.properties.AuthorizationMatrixProperty;
import hudson.model.Item;
import hudson.security.Permission;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.matrixauth.PermissionEntry;
import org.jenkinsci.plugins.matrixauth.inheritance.InheritanceStrategy;

public class FolderConfigurer extends AbstractConfigurer<AbstractFolder<?>, AuthorizationMatrixProperty> {

    protected FolderConfigurer() {}

    @Override
    protected void onCreated(Item item) {
        if ((item instanceof AbstractFolder<?>)) {
            log.finer("> " + this.getClass().getName() + ".onCreated()");
            super.onCreated(item);
            log.finer("< " + this.getClass().getName() + ".onCreated()");
        }
    }

    @Override
    protected void renameJob(Item item, String newName) throws IOException {
        if ((item instanceof AbstractFolder<?>)) {
            log.finer("> " + this.getClass().getName() + ".renameJob()");
            AbstractFolder<?> folder = (AbstractFolder<?>) item;
            folder.renameTo(newName);
            log.finer("< " + this.getClass().getName() + ".renameJob()");
        }
    }

    @Override
    protected Map<Permission, Set<PermissionEntry>> getGrantedPermissionEntries(
            AuthorizationMatrixProperty authProperty) {
        log.finer("> " + this.getClass().getName() + ".getGrantedPermissionEntries()");
        if (null == authProperty) {
            return new HashMap<Permission, Set<PermissionEntry>>();
        }
        log.finer("< " + this.getClass().getName() + ".addAuthorizationMatrixProperty()");
        return new HashMap<Permission, Set<PermissionEntry>>(authProperty.getGrantedPermissionEntries());
    }

    @Override
    protected AuthorizationMatrixProperty getAuthorizationMatrixProperty(Item item) {
        if (!(item instanceof AbstractFolder<?>)) {
            return null;
        }
        AbstractFolder<?> folder = (AbstractFolder<?>) item;
        return folder.getProperties().get(AuthorizationMatrixProperty.class);
    }

    @Override
    protected boolean removeProperty(Item item, AuthorizationMatrixProperty authProperty) {
        if (!(item instanceof AbstractFolder<?>)) {
            return false;
        }
        if (null == authProperty) {
            return false;
        }
        AbstractFolder<?> folder = (AbstractFolder<?>) item;
        List<?> folderProperties = folder.getProperties();
        return folderProperties.remove(authProperty);
    }

    private final AuthorizationMatrixProperty.DescriptorImpl getAuthorizationPropertyDescriptor() {
        AuthorizationMatrixProperty.DescriptorImpl result = (AuthorizationMatrixProperty.DescriptorImpl)
                (Jenkins.get().getDescriptor(AuthorizationMatrixProperty.class));
        if (result == null) {
            log.warning(AuthorizationMatrixProperty.DescriptorImpl.class.getName() + " is null");
        }
        return result;
    }

    @Override
    protected void addAuthorizationMatrixProperty(Item item, AuthorizationMatrixProperty authProperty)
            throws IOException {
        log.finer("> " + this.getClass().getName()
                + ".addAuthorizationMatrixProperty(Item, AuthorizationMatrixProperty)");
        if (!(item instanceof AbstractFolder<?>)) {
            log.warning("JobConfigurer.onCreated() non applicable for "
                    + item.getClass().getName());
            log.finer("< " + this.getClass().getName()
                    + ".addAuthorizationMatrixProperty(Item, AuthorizationMatrixProperty)");
            return;
        }
        AbstractFolder<?> folder = (AbstractFolder<?>) item;
        folder.addProperty(authProperty);
        log.finer("< " + this.getClass().getName()
                + ".addAuthorizationMatrixProperty(Item, AuthorizationMatrixProperty)");
    }

    @Override
    protected AuthorizationMatrixProperty createAuthorizationMatrixProperty() {
        AuthorizationMatrixProperty.DescriptorImpl propDescriptor = getAuthorizationPropertyDescriptor();
        if (propDescriptor == null) {
            log.finer("< " + this.getClass().getName()
                    + ".addAuthorizationMatrixProperty(Item, Map<Permission, Set<PermissionEntry>>)");
            return null;
        }

        return propDescriptor.create();
    }

    @Override
    protected void setInheritanceStrategy(
            AuthorizationMatrixProperty authProperty, InheritanceStrategy inheritanceStrategy) {
        authProperty.setInheritanceStrategy(inheritanceStrategy);
    }

    @Override
    protected void addPermission(AuthorizationMatrixProperty authProperty, Permission perm, PermissionEntry permEntry) {
        authProperty.add(perm, permEntry);
    }

    @Override
    protected boolean showPermission(Permission perm) {
        AuthorizationMatrixProperty.DescriptorImpl propDescriptor = getAuthorizationPropertyDescriptor();
        if (propDescriptor == null) {
            log.finer("< " + this.getClass().getName()
                    + ".addAuthorizationMatrixProperty(Item, Map<Permission, Set<PermissionEntry>>)");
            return false;
        }
        return propDescriptor.showPermission(perm);
    }
}
