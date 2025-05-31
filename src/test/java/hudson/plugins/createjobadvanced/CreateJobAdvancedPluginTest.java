package hudson.plugins.createjobadvanced;

import com.cloudbees.hudson.plugins.folder.Folder;
import hudson.maven.MavenModuleSet;
import hudson.model.Item;
import hudson.model.Job;
import hudson.model.User;
import hudson.security.ACL;
import hudson.security.ACLContext;
import hudson.security.AuthorizationMatrixProperty;
import hudson.security.HudsonPrivateSecurityRealm;
import hudson.security.Permission;
import hudson.security.ProjectMatrixAuthorizationStrategy;
import java.util.Collections;
import java.util.Objects;
import jenkins.branch.OrganizationFolder;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.matrixauth.PermissionEntry;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.recipes.LocalData;

public class CreateJobAdvancedPluginTest {

    @Rule
    public JenkinsRule r = new JenkinsRule();

    @Test
    @LocalData
    public void loadFullyConfiguredCreateJobAdvanced() throws Exception {
        // This test checks if the Create Job Advanced plugin can load a fully configured job from local data.
        // The actual implementation of this test will depend on the specific configurations you have set up in your
        // local data.
        Assert.assertTrue(r.jenkins.getSecurityRealm() instanceof HudsonPrivateSecurityRealm);
        ((HudsonPrivateSecurityRealm) r.jenkins.getSecurityRealm()).createAccount("alice", "alice");
        Assert.assertTrue(r.jenkins.getAuthorizationStrategy() instanceof ProjectMatrixAuthorizationStrategy);

        ProjectMatrixAuthorizationStrategy authorizationStrategy =
                (ProjectMatrixAuthorizationStrategy) r.jenkins.getAuthorizationStrategy();
        Assert.assertTrue(authorizationStrategy.hasExplicitPermission(PermissionEntry.user("alice"), Item.CREATE));
        Assert.assertTrue(authorizationStrategy.hasExplicitPermission(PermissionEntry.user("alice"), Jenkins.READ));

        CreateJobAdvancedPlugin cja = r.jenkins.getPlugin(CreateJobAdvancedPlugin.class);
        Assert.assertNotNull(cja);
        Assert.assertTrue(cja.isAutoOwnerRights());
        Assert.assertTrue(cja.isAutoPublicBrowse());
        Assert.assertTrue(cja.isReplaceSpace());
        Assert.assertTrue(cja.isMvnArchivingDisabled());
        Assert.assertTrue(cja.isMvnPerModuleEmail());
        Assert.assertTrue(cja.isActiveLogRotator());
        Assert.assertTrue(cja.isActiveDynamicPermissions());

        Job<?, ?> job;
        try (ACLContext ignored = ACL.as(User.get("alice", false, Collections.emptyMap()))) {
            job = r.createFreeStyleProject("test freestyle");
        }
        Assert.assertNotNull(job.getProperty(AuthorizationMatrixProperty.class));
        Assert.assertEquals("test-freestyle", job.getName());

        for (Permission permission :
                new Permission[] {Item.CONFIGURE, Item.BUILD, Item.READ, Item.DELETE, Item.WORKSPACE}) {
            Assert.assertTrue(job.getACL()
                    .hasPermission2(
                            Objects.requireNonNull(User.get("alice", false, Collections.emptyMap()))
                                    .impersonate2(),
                            permission));
        }

        // Maven job
        try (ACLContext ignored = ACL.as(User.get("alice", false, Collections.emptyMap()))) {
            job = r.createProject(MavenModuleSet.class, "test maven");
        }
        Assert.assertNotNull(job.getProperty(AuthorizationMatrixProperty.class));
        Assert.assertEquals("test-maven", job.getName());

        for (Permission permission :
                new Permission[] {Item.CONFIGURE, Item.BUILD, Item.READ, Item.DELETE, Item.WORKSPACE}) {
            Assert.assertTrue(job.getACL()
                    .hasPermission2(
                            Objects.requireNonNull(User.get("alice", false, Collections.emptyMap()))
                                    .impersonate2(),
                            permission));
        }

        // Folder job
        Folder folder;
        try (ACLContext ignored = ACL.as(User.get("alice", false, Collections.emptyMap()))) {
            folder = r.createProject(Folder.class, "test-folder");
        }
        Assert.assertEquals("test-folder", folder.getName());

        for (Permission permission :
                new Permission[] {Item.CONFIGURE, Item.BUILD, Item.READ, Item.DELETE, Item.WORKSPACE}) {
            Assert.assertTrue(folder.getACL()
                    .hasPermission2(
                            Objects.requireNonNull(User.get("alice", false, Collections.emptyMap()))
                                    .impersonate2(),
                            permission));
        }

        // Pipeline job
        try (ACLContext ignored = ACL.as(User.get("alice", false, Collections.emptyMap()))) {
            job = r.createProject(WorkflowJob.class, "test pipeline");
        }
        Assert.assertNotNull(job.getProperty(AuthorizationMatrixProperty.class));
        Assert.assertEquals("test-pipeline", job.getName());

        for (Permission permission :
                new Permission[] {Item.CONFIGURE, Item.BUILD, Item.READ, Item.DELETE, Item.WORKSPACE}) {
            Assert.assertTrue(job.getACL()
                    .hasPermission2(
                            Objects.requireNonNull(User.get("alice", false, Collections.emptyMap()))
                                    .impersonate2(),
                            permission));
        }

        WorkflowMultiBranchProject mbp;
        try (ACLContext ignored = ACL.as(User.get("alice", false, Collections.emptyMap()))) {
            mbp = r.createProject(WorkflowMultiBranchProject.class, "test multibranch");
        }
        // Assert.assertNotNull(mbp.getProperty(AuthorizationMatrixProperty.class));
        Assert.assertEquals("test-multibranch", mbp.getName());

        for (Permission permission :
                new Permission[] {Item.CONFIGURE, Item.BUILD, Item.READ, Item.DELETE, Item.WORKSPACE}) {
            Assert.assertTrue(mbp.getACL()
                    .hasPermission2(
                            Objects.requireNonNull(User.get("alice", false, Collections.emptyMap()))
                                    .impersonate2(),
                            permission));
        }

        OrganizationFolder orgFolder;
        try (ACLContext ignored = ACL.as(User.get("alice", false, Collections.emptyMap()))) {
            orgFolder = r.createProject(OrganizationFolder.class, "test org folder");
        }
        Assert.assertEquals("test-org-folder", orgFolder.getName());
        for (Permission permission :
                new Permission[] {Item.CONFIGURE, Item.BUILD, Item.READ, Item.DELETE, Item.WORKSPACE}) {
            Assert.assertTrue(orgFolder
                    .getACL()
                    .hasPermission2(
                            Objects.requireNonNull(User.get("alice", false, Collections.emptyMap()))
                                    .impersonate2(),
                            permission));
        }
    }
}
