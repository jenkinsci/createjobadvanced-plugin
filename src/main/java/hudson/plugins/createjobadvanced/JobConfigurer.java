package hudson.plugins.createjobadvanced;

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

public class JobConfigurer extends AbstractConfigurer<Job<?, ?>, AuthorizationMatrixProperty> {

    protected JobConfigurer() {}

    @Override
    protected void renameJob(Item item, String newName) throws IOException {
        if (!(item instanceof Job<?, ?>)) {
            log.finer("> " + this.getClass().getName() + ".renameJob()");
            Job<?, ?> job = (Job<?, ?>) item;
            job.renameTo(newName);
            log.finer("< " + this.getClass().getName() + ".renameJob()");
        }
    }

    @Override
    protected void onCreated(Item item) {
        if ((item instanceof Job<?, ?>)) {
            final CreateJobAdvancedPlugin cja = getPlugin();
            if (null == cja) {
                return;
            }
            log.finer("> " + this.getClass().getName() + ".onCreated()");
            super.onCreated(item);
            Job<?, ?> job = (Job<?, ?>) item;
            if (cja.isActiveLogRotator()) {
                activateLogRotator(job, cja);
            }
            log.finer("< " + this.getClass().getName() + ".onCreated()");
        }
    }

    @Override
    protected AuthorizationMatrixProperty getAuthorizationMatrixProperty(Item item) {
        if (!(item instanceof Job<?, ?>)) {
            return null;
        }
        Job<?, ?> job = (Job<?, ?>) item;
        return job.getProperty(AuthorizationMatrixProperty.class);
    }

    @Override
    protected boolean removeProperty(Item item, AuthorizationMatrixProperty authProperty) {
        if (!(item instanceof Job<?, ?>)) {
            return false;
        }
        if (null == authProperty) {
            return false;
        }
        AuthorizationMatrixProperty result = null;
        Job<?, ?> job = (Job<?, ?>) item;
        try {
            result = job.removeProperty(AuthorizationMatrixProperty.class);
        } catch (IOException e) {
            log.log(Level.SEVERE, "problem to remove granted permissions (template or copy job)", e);
        }
        return null != result;
    }

    @Override
    protected Map<Permission, Set<PermissionEntry>> getGrantedPermissionEntries(
            AuthorizationMatrixProperty authProperty) {
        if (null == authProperty) {
            return new HashMap<Permission, Set<PermissionEntry>>();
        }
        return new HashMap<Permission, Set<PermissionEntry>>(authProperty.getGrantedPermissionEntries());
    }

    private void activateLogRotator(final Item item, final CreateJobAdvancedPlugin cja) {

        if (!(item instanceof Job<?, ?>)) {
            return;
        }
        Job<?, ?> job = (Job<?, ?>) item;
        // if template, it's possible that log rotator is already defined
        if (null != job.getBuildDiscarder()) {
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
        if (!(item instanceof Job<?, ?>)) {
            log.warning("JobConfigurer.onCreated() non applicable for "
                    + item.getClass().getName());
            log.finer("< " + this.getClass().getName() + ".addAuthorizationMatrixProperty()");
            return;
        }
        Job<?, ?> job = (Job<?, ?>) item;
        job.addProperty(authProperty);
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
