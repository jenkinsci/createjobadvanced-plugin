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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import jenkins.branch.OrganizationFolder;
import jenkins.model.Jenkins;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.matrixauth.PermissionEntry;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.recipes.LocalData;
import org.kohsuke.stapler.StaplerRequest2;
import org.mockito.Mockito;

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
    public void testCreateJobAdvancedPlugin() throws Exception {
        // This test checks if the Create Job Advanced plugin can be instantiated.
        CreateJobAdvancedPlugin cja = r.jenkins.getPlugin(CreateJobAdvancedPlugin.class);
        Assert.assertNotNull("Create Job Advanced Plugin should be instantiated", cja);
        Assert.assertFalse("Auto owner rights should be false by default", cja.isAutoOwnerRights());
        Assert.assertFalse("Auto public browse should be false by default", cja.isAutoPublicBrowse());
        Assert.assertFalse("Replace space should be false by default", cja.isReplaceSpace());
        Assert.assertFalse("Maven archiving disabled should be false by default", cja.isMvnArchivingDisabled());
        Assert.assertFalse("Maven per module email should be false by default", cja.isMvnPerModuleEmail());
        Assert.assertFalse("Active log rotator should be false by default", cja.isActiveLogRotator());
        Assert.assertEquals("Days to keep should be -1 by default", -1, cja.getDaysToKeep());
        Assert.assertEquals("Number to keep should be -1 by default", -1, cja.getNumToKeep());
        Assert.assertEquals("Artifact days to keep should be -1 by default", -1, cja.getArtifactDaysToKeep());
        Assert.assertEquals("Artifact number to keep should be -1 by default", -1, cja.getArtifactNumToKeep());
        Assert.assertFalse("Active dynamic permissions should be false by default", cja.isActiveDynamicPermissions());
        Assert.assertNull("Extract pattern should be null by default", cja.getExtractPattern());
        List<DynamicPermissionConfig> dynamicPermissionConfigs = cja.getDynamicPermissionConfigs();
        Assert.assertNotNull("Dynamic permission configs should not be null", dynamicPermissionConfigs);
        Assert.assertTrue("Dynamic permission configs should be empty by default", dynamicPermissionConfigs.isEmpty());
        Map<String, List<Permission>> allPossiblePermissions = CreateJobAdvancedPlugin.getAllPossiblePermissions();
        Assert.assertNotNull("All possible permissions should not be null", allPossiblePermissions);
        Assert.assertTrue("All possible permissions should not be empty", !allPossiblePermissions.isEmpty());
        // Iterate through all possible permissions to ensure they are correctly defined.
        for (String group : allPossiblePermissions.keySet()) {
            Assert.assertNotNull("Group name should not be null", group);
            Assert.assertFalse("Group name should not be empty", group.isEmpty());
            List<Permission> permissions = allPossiblePermissions.get(group);
            Assert.assertNotNull("Permissions list for group " + group + " should not be null", permissions);
            Assert.assertFalse("Permissions list for group " + group + " should not be empty", permissions.isEmpty());
            for (Permission permission : permissions) {
                Assert.assertNotNull("Permission should not be null", permission);
                Assert.assertFalse("Permission name should not be empty", permission.name.isEmpty());
                Assert.assertFalse(
                        "Permission ID should not be empty", permission.getId().isEmpty());
            }
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
        Assert.assertNotNull("Create Job Advanced Plugin should be instantiated", cja);
        Assert.assertTrue(cja.isAutoOwnerRights());
        Assert.assertTrue(cja.isAutoPublicBrowse());
        Assert.assertTrue(cja.isReplaceSpace());
        Assert.assertTrue(cja.isMvnArchivingDisabled());
        Assert.assertTrue(cja.isMvnPerModuleEmail());
        Assert.assertTrue(cja.isActiveLogRotator());
        Assert.assertEquals(1, cja.getDaysToKeep());
        Assert.assertEquals(3, cja.getNumToKeep());
        Assert.assertEquals(3, cja.getArtifactDaysToKeep());
        Assert.assertEquals(7, cja.getArtifactNumToKeep());
        Assert.assertTrue(cja.isActiveDynamicPermissions());
        Assert.assertEquals(".*", cja.getExtractPattern());
        List<DynamicPermissionConfig> dynamicPermissionConfigs = cja.getDynamicPermissionConfigs();
        Assert.assertNotNull(dynamicPermissionConfigs);
        Assert.assertEquals(1, dynamicPermissionConfigs.size());
        DynamicPermissionConfig config = dynamicPermissionConfigs.get(0);
        Assert.assertEquals("authenticated", config.getGroupFormat());
        Map<String, List<Permission>> allPossiblePermissions = CreateJobAdvancedPlugin.getAllPossiblePermissions();
        Assert.assertNotNull(allPossiblePermissions);
        Assert.assertEquals(2, allPossiblePermissions.size());
        // Iterate through all possible permissions to ensure they are correctly defined.
        for (String group : allPossiblePermissions.keySet()) {
            Assert.assertNotNull(group);
            Assert.assertFalse(group.isEmpty());
            List<Permission> permissions = allPossiblePermissions.get(group);
            Assert.assertNotNull(permissions);
            Assert.assertFalse(permissions.isEmpty());
            for (Permission permission : permissions) {
                Assert.assertNotNull(permission);
                Assert.assertFalse(permission.name.isEmpty());
                Assert.assertFalse(permission.getId().isEmpty());
                Assert.assertTrue(config.isPermissionChecked(permission));
            }
        }
    }

    @Test
    @LocalData("createJobAdvancedFullConfig")
    public void freestyleTest() throws Exception {
        initUser("alice");
        FreeStyleProject project = createProject(FreeStyleProject.class, "Free Style Project", "alice");
        Assert.assertEquals(project.getName().replaceAll(" ", "-"), project.getName());
        testOwnerRights(project, "alice");
        project.renameTo(project.getName() + " Renamed");
        Assert.assertEquals(project.getName().replaceAll(" ", "-"), project.getName());
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
        project.renameTo(project.getName() + " Renamed");
        Assert.assertEquals(project.getName().replaceAll(" ", "-"), project.getName());
    }

    @Test
    @LocalData("createJobAdvancedFullConfig")
    public void folderTest() throws Exception {
        initUser("alice");
        Folder project = createProject(Folder.class, "User Folder", "alice");
        Assert.assertEquals(project.getName().replaceAll(" ", "-"), project.getName());
        testOwnerRights(project, "alice");
        project.renameTo(project.getName() + " Renamed");
        Assert.assertEquals(project.getName().replaceAll(" ", "-"), project.getName());
    }

    @Test
    @LocalData("createJobAdvancedFullConfig")
    public void pipelineTest() throws Exception {
        initUser("alice");
        WorkflowJob project = createProject(WorkflowJob.class, "Workflow Job", "alice");
        Assert.assertEquals(project.getName().replaceAll(" ", "-"), project.getName());
        testOwnerRights(project, "alice");
        project.renameTo(project.getName() + " Renamed");
        Assert.assertEquals(project.getName().replaceAll(" ", "-"), project.getName());
    }

    @Test
    @LocalData("createJobAdvancedFullConfig")
    public void multibranchTest() throws Exception {
        initUser("alice");
        WorkflowMultiBranchProject project =
                createProject(WorkflowMultiBranchProject.class, "Workflow Multi Branch Project", "alice");
        Assert.assertEquals(project.getName().replaceAll(" ", "-"), project.getName());
        testOwnerRights(project, "alice");
        project.renameTo(project.getName() + " Renamed");
        Assert.assertEquals(project.getName().replaceAll(" ", "-"), project.getName());
    }

    @Test
    @LocalData("createJobAdvancedFullConfig")
    public void organizationFolderTest() throws Exception {
        initUser("alice");
        OrganizationFolder project = createProject(OrganizationFolder.class, "Organization Folder", "alice");
        Assert.assertEquals(project.getName().replaceAll(" ", "-"), project.getName());
        testOwnerRights(project, "alice");
        project.renameTo(project.getName() + " Renamed");
        Assert.assertEquals(project.getName().replaceAll(" ", "-"), project.getName());
    }

    private <T extends TopLevelItem> T createProject(Class<T> jobClass, String name, String creator) throws Exception {
        // This method creates a project of the specified type with the given name.
        T project;
        try (ACLContext ignored = ACL.as(User.get(creator, false, Collections.emptyMap()))) {
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

    @Test
    public void testConfigureWithDefaultValues() throws Exception {
        // Simulate a request with default values
        JSONObject formData = new JSONObject();
        StaplerRequest2 req = Mockito.mock(StaplerRequest2.class);

        CreateJobAdvancedPlugin plugin = r.jenkins.getPlugin(CreateJobAdvancedPlugin.class);
        Assert.assertNotNull(plugin);

        plugin.configure(req, formData);

        Assert.assertFalse("Auto owner rights should be false by default", plugin.isAutoOwnerRights());
        Assert.assertFalse("Auto public browse should be false by default", plugin.isAutoPublicBrowse());
        Assert.assertFalse("Replace space should be false by default", plugin.isReplaceSpace());
        Assert.assertFalse("Maven archiving disabled should be false by default", plugin.isMvnArchivingDisabled());
        Assert.assertFalse("Maven per module email should be false by default", plugin.isMvnPerModuleEmail());
        Assert.assertFalse("Active log rotator should be false by default", plugin.isActiveLogRotator());
        Assert.assertEquals("Days to keep should be -1 by default", -1, plugin.getDaysToKeep());
        Assert.assertEquals("Number to keep should be -1 by default", -1, plugin.getNumToKeep());
        Assert.assertEquals("Artifact days to keep should be -1 by default", -1, plugin.getArtifactDaysToKeep());
        Assert.assertEquals("Artifact number to keep should be -1 by default", -1, plugin.getArtifactNumToKeep());
        Assert.assertFalse(
                "Active dynamic permissions should be false by default", plugin.isActiveDynamicPermissions());
        Assert.assertNull("Extract pattern should be null by default", plugin.getExtractPattern());
        Assert.assertTrue(
                "Dynamic permission configs should be empty by default",
                plugin.getDynamicPermissionConfigs().isEmpty());
    }

    @Test
    public void testConfigureWithLogRotator() throws Exception {
        // Simulate a request with log rotator configuration
        JSONObject formData = new JSONObject();
        JSONObject logRotatorConfig = new JSONObject();
        logRotatorConfig.put("daysToKeep", 5);
        logRotatorConfig.put("numToKeep", 10);
        logRotatorConfig.put("artifactDaysToKeep", 15);
        logRotatorConfig.put("artifactNumToKeep", 20);
        formData.put("activeLogRotator", logRotatorConfig);

        StaplerRequest2 req = Mockito.mock(StaplerRequest2.class);

        CreateJobAdvancedPlugin plugin = r.jenkins.getPlugin(CreateJobAdvancedPlugin.class);
        Assert.assertNotNull(plugin);

        plugin.configure(req, formData);

        Assert.assertTrue("Active log rotator should be true", plugin.isActiveLogRotator());
        Assert.assertEquals("Days to keep should be 5", 5, plugin.getDaysToKeep());
        Assert.assertEquals("Number to keep should be 10", 10, plugin.getNumToKeep());
        Assert.assertEquals("Artifact days to keep should be 15", 15, plugin.getArtifactDaysToKeep());
        Assert.assertEquals("Artifact number to keep should be 20", 20, plugin.getArtifactNumToKeep());
    }

    @Test
    public void testConfigureWithDynamicPermissionsSingle() throws Exception {
        // Simulate a request with dynamic permissions configuration
        JSONObject formData = new JSONObject();
        JSONObject dynamicPermissionsConfig = new JSONObject();
        dynamicPermissionsConfig.put("extractPattern", ".*");
        JSONObject cfgs = new JSONObject();
        cfgs.put("groupFormat", "authenticated");
        // DynamicPermissionConfig dynPerm = ;
        Set<Permission> checkedPermissions = new HashSet<>();
        checkedPermissions.add(hudson.model.Item.CONFIGURE);

        final Map<String, List<Permission>> allPossiblePermissions =
                CreateJobAdvancedPlugin.getAllPossiblePermissions();
        for (Map.Entry<String, List<Permission>> entry : allPossiblePermissions.entrySet()) {
            for (Permission permission : entry.getValue()) {
                cfgs.put(permission.getId(), checkedPermissions.contains(permission));
            }
        }
        dynamicPermissionsConfig.put("cfgs", cfgs);
        formData.put("activeDynamicPermissions", dynamicPermissionsConfig);

        StaplerRequest2 req = Mockito.mock(StaplerRequest2.class);
        Mockito.when(req.bindJSON(Mockito.eq(DynamicPermissionConfig.class), Mockito.any(JSONObject.class)))
                .thenReturn(new DynamicPermissionConfig("authenticated", (new HashSet<>())));

        CreateJobAdvancedPlugin plugin = r.jenkins.getPlugin(CreateJobAdvancedPlugin.class);
        Assert.assertNotNull(plugin);

        plugin.configure(req, formData);

        Assert.assertTrue("Active dynamic permissions should be true", plugin.isActiveDynamicPermissions());
        Assert.assertEquals("Extract pattern should be .*", ".*", plugin.getExtractPattern());
        Assert.assertEquals(
                "Dynamic permission configs should have one entry",
                1,
                plugin.getDynamicPermissionConfigs().size());

        DynamicPermissionConfig configuredPermCfg =
                plugin.getDynamicPermissionConfigs().get(0);
        Assert.assertEquals("authenticated", configuredPermCfg.getGroupFormat());

        Assert.assertEquals(
                checkedPermissions.size(),
                configuredPermCfg.getCheckedPermissionIds().size());

        // Check expected checked permissions match checked permissions
        for (Permission perm : checkedPermissions) {
            Assert.assertTrue(configuredPermCfg.isPermissionChecked(perm));
        }

        // Check that checked permissions match only expected checked permissions
        for (String permId : configuredPermCfg.getCheckedPermissionIds()) {
            Assert.assertTrue(checkedPermissions.contains(Permission.fromId(permId)));
        }
    }

    @Test
    public void testConfigureWithDynamicPermissionsArray() throws Exception {
        // Simulate a request with dynamic permissions configuration
        JSONObject formData = new JSONObject();
        JSONObject dynamicPermissionsConfig = new JSONObject();
        dynamicPermissionsConfig.put("extractPattern", ".*");
        JSONArray cfgs = new JSONArray();
        JSONObject groupFormat = new JSONObject();
        groupFormat.put("groupFormat", "authenticated");
        // DynamicPermissionConfig dynPerm = ;
        Set<Permission> checkedPermissions = new HashSet<>();
        checkedPermissions.add(hudson.model.Item.CONFIGURE);

        final Map<String, List<Permission>> allPossiblePermissions =
                CreateJobAdvancedPlugin.getAllPossiblePermissions();
        for (Map.Entry<String, List<Permission>> entry : allPossiblePermissions.entrySet()) {
            for (Permission permission : entry.getValue()) {
                groupFormat.put(permission.getId(), checkedPermissions.contains(permission));
            }
        }
        cfgs.add(groupFormat);
        dynamicPermissionsConfig.put("cfgs", cfgs);
        formData.put("activeDynamicPermissions", dynamicPermissionsConfig);

        StaplerRequest2 req = Mockito.mock(StaplerRequest2.class);
        Mockito.when(req.bindJSON(Mockito.eq(DynamicPermissionConfig.class), Mockito.any(JSONObject.class)))
                .thenReturn(new DynamicPermissionConfig("authenticated", (new HashSet<>())));

        CreateJobAdvancedPlugin plugin = r.jenkins.getPlugin(CreateJobAdvancedPlugin.class);
        Assert.assertNotNull(plugin);

        plugin.configure(req, formData);

        Assert.assertTrue("Active dynamic permissions should be true", plugin.isActiveDynamicPermissions());
        Assert.assertEquals("Extract pattern should be .*", ".*", plugin.getExtractPattern());
        Assert.assertEquals(
                "Dynamic permission configs should have one entry",
                1,
                plugin.getDynamicPermissionConfigs().size());

        DynamicPermissionConfig configuredPermCfg =
                plugin.getDynamicPermissionConfigs().get(0);
        Assert.assertEquals("authenticated", configuredPermCfg.getGroupFormat());

        Assert.assertEquals(
                checkedPermissions.size(),
                configuredPermCfg.getCheckedPermissionIds().size());

        // Check expected checked permissions match checked permissions
        for (Permission perm : checkedPermissions) {
            Assert.assertTrue(configuredPermCfg.isPermissionChecked(perm));
        }

        // Check that checked permissions match only expected checked permissions
        for (String permId : configuredPermCfg.getCheckedPermissionIds()) {
            Assert.assertTrue(checkedPermissions.contains(Permission.fromId(permId)));
        }
    }

    @Test
    public void testImmpliedByList() {
        String implies = CreateJobAdvancedPlugin.impliedByList(hudson.model.Item.DISCOVER);
        Assert.assertTrue(implies.contains("hudson.model.Item.Read"));
    }

    @Test
    public void testImmpliedByList2() {
        String implies = CreateJobAdvancedPlugin.impliedByList(null);
        Assert.assertTrue(implies.isEmpty());
    }
}
