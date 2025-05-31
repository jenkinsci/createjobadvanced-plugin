package hudson.plugins.createjobadvanced;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import hudson.model.Item;
import hudson.model.Job;
import hudson.security.AuthorizationMatrixProperty;
import hudson.security.Permission;
import hudson.tasks.LogRotator;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.matrixauth.PermissionEntry;
import org.jenkinsci.plugins.matrixauth.inheritance.InheritanceStrategy;

/**
 * Changes the configuration of {@link Job<?,?>} items.
 *
 * @author Laurent Coltat
 */
public class JobConfigurer extends AbstractConfigurer<Job<?, ?>, AuthorizationMatrixProperty> {

    protected JobConfigurer() {}

    @Override
    protected final void renameJob(@Nullable Item item, @NonNull String newName) throws IOException {
        if (null != newName && null != item && item instanceof Job<?, ?>) {
            Job<?, ?> job = (Job<?, ?>) item;
            job.renameTo(newName);
        }
    }

    @Override
    protected void doCreate(Item item) {
        if (null != item && item instanceof Job<?, ?>) {
            final CreateJobAdvancedPlugin cja = getPlugin();
            if (null != cja) {
                super.doCreate(item);
                Job<?, ?> job = (Job<?, ?>) item;
                if (cja.isActiveLogRotator()) {
                    activateLogRotator(job, cja);
                }
            }
        }
    }

    @Override
    protected final @Nullable AuthorizationMatrixProperty getAuthorizationMatrixProperty(@Nullable Item item) {
        AuthorizationMatrixProperty result = null;
        if (null != item && item instanceof Job<?, ?>) {
            Job<?, ?> job = (Job<?, ?>) item;
            result = job.getProperty(AuthorizationMatrixProperty.class);
        }
        return result;
    }

    @Override
    protected final void removeProperty(@Nullable Item item, @Nullable AuthorizationMatrixProperty authProperty) {
        if (null != item && null != authProperty && item instanceof Job<?, ?>) {
            Job<?, ?> job = (Job<?, ?>) item;
            try {
                job.removeProperty(AuthorizationMatrixProperty.class);
            } catch (IOException e) {
                log.log(Level.SEVERE, "problem to remove granted permissions (template or copy job)", e);
            }
        }
    }

    @Override
    protected final Map<Permission, Set<PermissionEntry>> getGrantedPermissionEntries(
            @Nullable AuthorizationMatrixProperty authProperty) {
        HashMap<Permission, Set<PermissionEntry>> result = new HashMap<Permission, Set<PermissionEntry>>();
        if (null != authProperty) {
            result = new HashMap<Permission, Set<PermissionEntry>>(authProperty.getGrantedPermissionEntries());
        }
        return result;
    }

    /**
     *
     * @param item
     * @param cja
     */
    private final void activateLogRotator(final Item item, final CreateJobAdvancedPlugin cja) {
        if (null != item && null != cja && item instanceof Job<?, ?>) {
            Job<?, ?> job = (Job<?, ?>) item;
            // if template, it's possible that log rotator is already defined
            if (null == job.getBuildDiscarder()) {
                LogRotator logrotator = new LogRotator(
                        cja.getDaysToKeep(),
                        cja.getNumToKeep(),
                        cja.getArtifactDaysToKeep(),
                        cja.getArtifactNumToKeep());
                try {
                    // with 1.503, the signature changed and might now throw an IOException
                    job.setBuildDiscarder(logrotator);
                } catch (Exception e) {
                    log.log(Level.SEVERE, "error setting Logrotater", e);
                }
            }
        }
    }

    /**
     *
     * @return
     */
    private final @Nullable AuthorizationMatrixProperty.DescriptorImpl getAuthorizationPropertyDescriptor() {
        return (AuthorizationMatrixProperty.DescriptorImpl)
                (Jenkins.get().getDescriptor(AuthorizationMatrixProperty.class));
    }

    @Override
    protected final void addAuthorizationMatrixProperty(
            @Nullable Item item, @Nullable AuthorizationMatrixProperty authProperty) throws IOException {
        if (null != authProperty && null != item && item instanceof Job<?, ?>) {
            log.finer("> " + this.getClass().getName()
                    + ".addAuthorizationMatrixProperty(Item, AuthorizationMatrixProperty)");
            Job<?, ?> job = (Job<?, ?>) item;
            job.addProperty(authProperty);
            log.finer("< " + this.getClass().getName()
                    + ".addAuthorizationMatrixProperty(Item, AuthorizationMatrixProperty)");
        }
    }

    @Override
    protected final @Nullable AuthorizationMatrixProperty createAuthorizationMatrixProperty() {
        AuthorizationMatrixProperty result = null;
        AuthorizationMatrixProperty.DescriptorImpl propDescriptor = getAuthorizationPropertyDescriptor();
        if (propDescriptor != null) {
            result = propDescriptor.create();
        }
        return result;
    }

    @Override
    protected final void setInheritanceStrategy(
            @Nullable AuthorizationMatrixProperty authProperty, @Nullable InheritanceStrategy inheritanceStrategy) {
        if (null == inheritanceStrategy) {
            log.warning("inheritanceStrategy parameter is null");
            return;
        }
        if (null == authProperty) {
            log.warning("authProperty parameter is null");
            return;
        }
        authProperty.setInheritanceStrategy(inheritanceStrategy);
    }

    @Override
    protected final void addPermission(
            @Nullable AuthorizationMatrixProperty authProperty,
            @Nullable Permission perm,
            @Nullable PermissionEntry permEntry) {
        if (permEntry != null && perm != null && null != authProperty) {
            authProperty.add(perm, permEntry);
        }
    }

    @Override
    protected final boolean showPermission(@Nullable Permission perm) {
        boolean result = false;
        if (perm != null) {
            AuthorizationMatrixProperty.DescriptorImpl propDescriptor = getAuthorizationPropertyDescriptor();
            if (null != propDescriptor) {
                result = propDescriptor.showPermission(perm);
            }
        }
        return result;
    }
}
