package hudson.plugins.createjobadvanced;

import com.cloudbees.hudson.plugins.folder.AbstractFolder;
import com.cloudbees.hudson.plugins.folder.properties.AuthorizationMatrixProperty;
import edu.umd.cs.findbugs.annotations.Nullable;
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

/**
 * Changes the configuration of {@link AbstractFolder<?>} items.
 *
 * @author Laurent Coltat
 */
public final class FolderConfigurer extends AbstractConfigurer<AbstractFolder<?>, AuthorizationMatrixProperty> {

    /**
     * Class contructor
     */
    protected FolderConfigurer() {}

    @Override
    protected void doCreate(@Nullable Item item) {
        if (null != item && item instanceof AbstractFolder<?>) {
            super.doCreate(item);
        }
    }

    @Override
    protected void renameJob(@Nullable Item item, @Nullable String newName) throws IOException {
        if (null != item && null != newName && item instanceof AbstractFolder<?>) {
            AbstractFolder<?> folder = (AbstractFolder<?>) item;
            folder.renameTo(newName);
        }
    }

    @Override
    protected Map<Permission, Set<PermissionEntry>> getGrantedPermissionEntries(
            @Nullable AuthorizationMatrixProperty authProperty) {
        Map<Permission, Set<PermissionEntry>> result = new HashMap<Permission, Set<PermissionEntry>>();
        if (null != authProperty) {
            result = new HashMap<Permission, Set<PermissionEntry>>(authProperty.getGrantedPermissionEntries());
        }
        return result;
    }

    @Override
    @Nullable
    protected AuthorizationMatrixProperty getAuthorizationMatrixProperty(@Nullable Item item) {
        AuthorizationMatrixProperty result = null;
        if (null != item && item instanceof AbstractFolder<?>) {
            AbstractFolder<?> folder = (AbstractFolder<?>) item;
            result = folder.getProperties().get(AuthorizationMatrixProperty.class);
        }
        return result;
    }

    @Override
    protected void removeProperty(@Nullable Item item, @Nullable AuthorizationMatrixProperty authProperty) {
        if (null != item && null != authProperty && item instanceof AbstractFolder<?>) {
            AbstractFolder<?> folder = (AbstractFolder<?>) item;
            List<?> folderProperties = folder.getProperties();
            folderProperties.remove(authProperty);
        }
    }

    @Nullable
    private final AuthorizationMatrixProperty.DescriptorImpl getAuthorizationPropertyDescriptor() {
        AuthorizationMatrixProperty.DescriptorImpl result = (AuthorizationMatrixProperty.DescriptorImpl)
                (Jenkins.get().getDescriptor(AuthorizationMatrixProperty.class));
        return result;
    }

    @Override
    protected void addAuthorizationMatrixProperty(
            @Nullable Item item, @Nullable AuthorizationMatrixProperty authProperty) throws IOException {
        if (null != item && null != authProperty && item instanceof AbstractFolder<?>) {
            AbstractFolder<?> folder = (AbstractFolder<?>) item;
            folder.addProperty(authProperty);
        }
    }

    @Override
    @Nullable
    protected AuthorizationMatrixProperty createAuthorizationMatrixProperty() {
        AuthorizationMatrixProperty result = null;

        AuthorizationMatrixProperty.DescriptorImpl propDescriptor = getAuthorizationPropertyDescriptor();
        if (null != propDescriptor) {
            result = propDescriptor.create();
        }

        return result;
    }

    @Override
    protected void setInheritanceStrategy(
            @Nullable AuthorizationMatrixProperty authProperty, @Nullable InheritanceStrategy inheritanceStrategy) {
        if (null != authProperty && null != inheritanceStrategy) {
            authProperty.setInheritanceStrategy(inheritanceStrategy);
        }
    }

    @Override
    protected void addPermission(
            @Nullable AuthorizationMatrixProperty authProperty,
            @Nullable Permission perm,
            @Nullable PermissionEntry permEntry) {
        if (null != authProperty && null != perm && null != permEntry) {
            authProperty.add(perm, permEntry);
        }
    }

    @Override
    protected boolean showPermission(@Nullable Permission perm) {
        AuthorizationMatrixProperty.DescriptorImpl propDescriptor = getAuthorizationPropertyDescriptor();
        boolean result = false;
        if (null != propDescriptor && null != perm) {
            result = propDescriptor.showPermission(perm);
        }
        return result;
    }
}
