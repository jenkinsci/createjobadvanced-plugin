package hudson.plugins.createjobadvanced;

import com.cloudbees.hudson.plugins.folder.Folder;
import hudson.maven.MavenModuleSet;
import hudson.maven.reporters.MavenMailer;
import hudson.model.AbstractItem;
import hudson.model.FreeStyleProject;
import hudson.model.Item;
import hudson.model.TopLevelItem;
import hudson.model.User;
import hudson.security.ACL;
import hudson.security.ACLContext;
import hudson.security.HudsonPrivateSecurityRealm;
import hudson.security.Permission;
import hudson.security.ProjectMatrixAuthorizationStrategy;
import java.io.IOException;
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

    private void initUser(String username) throws IOException {
        // Initialize a user with the given username.
        HudsonPrivateSecurityRealm realm = (HudsonPrivateSecurityRealm) r.jenkins.getSecurityRealm();
        if (realm != null) {
            realm.createAccount(username, username);
        }
    }

    @Test
    @LocalData("createJobAdvancedFullConfig")
    public void fullConfigTest() throws Exception {
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
    }

    @Test
    @LocalData("createJobAdvancedFullConfig")
    public void freestyleTest() throws Exception {
        initUser("alice");
        FreeStyleProject project = createProject(FreeStyleProject.class, "Free Style Project", "alice");
        Assert.assertEquals(project.getName().replaceAll(" ", "-"), project.getName());
        testOwnerRights(project, "alice");
    }

    @Test
    @LocalData("createJobAdvancedFullConfig")
    public void mavenTest() throws Exception {
        initUser("alice");
        MavenModuleSet project = createProject(MavenModuleSet.class, "Maven Module Set", "alice");
        Assert.assertEquals(project.getName().replaceAll(" ", "-"), project.getName());
        testOwnerRights(project, "alice");
        Assert.assertTrue(project.isArchivingDisabled());
        MavenMailer m = project.getReporters().get(MavenMailer.class);
        Assert.assertNotNull(m);
        Assert.assertTrue(m.perModuleEmail);
    }

    @Test
    @LocalData("createJobAdvancedFullConfig")
    public void folderTest() throws Exception {
        initUser("alice");
        Folder project = createProject(Folder.class, "User Folder", "alice");
        Assert.assertEquals(project.getName().replaceAll(" ", "-"), project.getName());
        testOwnerRights(project, "alice");
    }

    @Test
    @LocalData("createJobAdvancedFullConfig")
    public void pipelineTest() throws Exception {
        initUser("alice");
        WorkflowJob project = createProject(WorkflowJob.class, "Workflow Job", "alice");
        Assert.assertEquals(project.getName().replaceAll(" ", "-"), project.getName());
        testOwnerRights(project, "alice");
    }

    @Test
    @LocalData("createJobAdvancedFullConfig")
    public void multibranchTest() throws Exception {
        initUser("alice");
        WorkflowMultiBranchProject project =
                createProject(WorkflowMultiBranchProject.class, "Workflow Multi Branch Project", "alice");
        Assert.assertEquals(project.getName().replaceAll(" ", "-"), project.getName());
        testOwnerRights(project, "alice");
    }

    @Test
    @LocalData("createJobAdvancedFullConfig")
    public void organizationFolderTest() throws Exception {
        initUser("alice");
        OrganizationFolder project = createProject(OrganizationFolder.class, "Organization Folder", "alice");
        Assert.assertEquals(project.getName().replaceAll(" ", "-"), project.getName());
        testOwnerRights(project, "alice");
    }

    private <T extends TopLevelItem> T createProject(Class<T> jobClass, String name, String creator) throws Exception {
        // This method creates a project of the specified type with the given name.
        T project;
        try (ACLContext ignored = ACL.as(User.get("alice", false, Collections.emptyMap()))) {
            project = r.createProject(jobClass, name);
        }
        return project;
    }

    private void testOwnerRights(AbstractItem item, String owner) throws Exception {
        // This method checks if the owner rights are correctly set for the given job.
        for (Permission permission :
                new Permission[] {Item.CONFIGURE, Item.BUILD, Item.READ, Item.DELETE, Item.WORKSPACE}) {
            Assert.assertTrue(item.getACL()
                    .hasPermission2(
                            Objects.requireNonNull(User.get(owner, false, Collections.emptyMap()))
                                    .impersonate2(),
                            permission));
        }
    }
}
