package hudson.plugins.createjobadvanced;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.jvnet.hudson.test.recipes.LocalData;
import org.kohsuke.stapler.StaplerRequest2;

@WithJenkins
class CreateJobAdvancedPluginTest {

    private JenkinsRule r;

    @BeforeEach
    void setUp(JenkinsRule rule) {
        r = rule;
    }

    private void initUser(String username) throws IOException {
        // Initialize a user with the given username.
        HudsonPrivateSecurityRealm realm = (HudsonPrivateSecurityRealm) r.jenkins.getSecurityRealm();
        if (realm != null) {
            realm.createAccount(username, username);
        }
    }

    @Test
    void testCreateJobAdvancedPlugin() {
        // This test checks if the Create Job Advanced plugin can be instantiated.
        CreateJobAdvancedPlugin cja = r.jenkins.getPlugin(CreateJobAdvancedPlugin.class);
        assertNotNull(cja, "Create Job Advanced Plugin should be instantiated");
        assertFalse(cja.isAutoOwnerRights(), "Auto owner rights should be false by default");
        assertFalse(cja.isAutoPublicBrowse(), "Auto public browse should be false by default");
        assertFalse(cja.isReplaceSpace(), "Replace space should be false by default");
        assertFalse(cja.isMvnArchivingDisabled(), "Maven archiving disabled should be false by default");
        assertFalse(cja.isMvnPerModuleEmail(), "Maven per module email should be false by default");
        assertFalse(cja.isActiveLogRotator(), "Active log rotator should be false by default");
        assertEquals(-1, cja.getDaysToKeep(), "Days to keep should be -1 by default");
        assertEquals(-1, cja.getNumToKeep(), "Number to keep should be -1 by default");
        assertEquals(-1, cja.getArtifactDaysToKeep(), "Artifact days to keep should be -1 by default");
        assertEquals(-1, cja.getArtifactNumToKeep(), "Artifact number to keep should be -1 by default");
        assertFalse(cja.isActiveDynamicPermissions(), "Active dynamic permissions should be false by default");
        assertNull(cja.getExtractPattern(), "Extract pattern should be null by default");
        List<DynamicPermissionConfig> dynamicPermissionConfigs = cja.getDynamicPermissionConfigs();
        assertNotNull(dynamicPermissionConfigs, "Dynamic permission configs should not be null");
        assertTrue(dynamicPermissionConfigs.isEmpty(), "Dynamic permission configs should be empty by default");
        Map<String, List<Permission>> allPossiblePermissions = CreateJobAdvancedPlugin.getAllPossiblePermissions();
        assertNotNull(allPossiblePermissions, "All possible permissions should not be null");
        assertFalse(allPossiblePermissions.isEmpty(), "All possible permissions should not be empty");
        // Iterate through all possible permissions to ensure they are correctly defined.
        for (String group : allPossiblePermissions.keySet()) {
            assertNotNull(group, "Group name should not be null");
            assertFalse(group.isEmpty(), "Group name should not be empty");
            List<Permission> permissions = allPossiblePermissions.get(group);
            assertNotNull(permissions, "Permissions list for group " + group + " should not be null");
            assertFalse(permissions.isEmpty(), "Permissions list for group " + group + " should not be empty");
            for (Permission permission : permissions) {
                assertNotNull(permission, "Permission should not be null");
                assertFalse(permission.name.isEmpty(), "Permission name should not be empty");
                assertFalse(permission.getId().isEmpty(), "Permission ID should not be empty");
            }
        }
    }

    @Test
    @LocalData("createJobAdvancedFullConfig")
    void fullConfigTest() throws Exception {
        // This test checks if the Create Job Advanced plugin can load a fully configured job from local data.
        // The actual implementation of this test will depend on the specific configurations you have set up in your
        // local data.
        assertInstanceOf(HudsonPrivateSecurityRealm.class, r.jenkins.getSecurityRealm());
        ((HudsonPrivateSecurityRealm) r.jenkins.getSecurityRealm()).createAccount("alice", "alice");
        assertInstanceOf(ProjectMatrixAuthorizationStrategy.class, r.jenkins.getAuthorizationStrategy());

        ProjectMatrixAuthorizationStrategy authorizationStrategy =
                (ProjectMatrixAuthorizationStrategy) r.jenkins.getAuthorizationStrategy();
        assertTrue(authorizationStrategy.hasExplicitPermission(PermissionEntry.user("alice"), Item.CREATE));
        assertTrue(authorizationStrategy.hasExplicitPermission(PermissionEntry.user("alice"), Jenkins.READ));

        CreateJobAdvancedPlugin cja = r.jenkins.getPlugin(CreateJobAdvancedPlugin.class);
        assertNotNull(cja, "Create Job Advanced Plugin should be instantiated");
        assertTrue(cja.isAutoOwnerRights());
        assertTrue(cja.isAutoPublicBrowse());
        assertTrue(cja.isReplaceSpace());
        assertTrue(cja.isMvnArchivingDisabled());
        assertTrue(cja.isMvnPerModuleEmail());
        assertTrue(cja.isActiveLogRotator());
        assertEquals(1, cja.getDaysToKeep());
        assertEquals(3, cja.getNumToKeep());
        assertEquals(3, cja.getArtifactDaysToKeep());
        assertEquals(7, cja.getArtifactNumToKeep());
        assertTrue(cja.isActiveDynamicPermissions());
        assertEquals(".*", cja.getExtractPattern());
        List<DynamicPermissionConfig> dynamicPermissionConfigs = cja.getDynamicPermissionConfigs();
        assertNotNull(dynamicPermissionConfigs);
        assertEquals(1, dynamicPermissionConfigs.size());
        DynamicPermissionConfig config = dynamicPermissionConfigs.get(0);
        assertEquals("authenticated", config.getGroupFormat());
        Map<String, List<Permission>> allPossiblePermissions = CreateJobAdvancedPlugin.getAllPossiblePermissions();
        assertNotNull(allPossiblePermissions);
        assertEquals(2, allPossiblePermissions.size());
        // Iterate through all possible permissions to ensure they are correctly defined.
        for (String group : allPossiblePermissions.keySet()) {
            assertNotNull(group);
            assertFalse(group.isEmpty());
            List<Permission> permissions = allPossiblePermissions.get(group);
            assertNotNull(permissions);
            assertFalse(permissions.isEmpty());
            for (Permission permission : permissions) {
                assertNotNull(permission);
                assertFalse(permission.name.isEmpty());
                assertFalse(permission.getId().isEmpty());
                assertTrue(config.isPermissionChecked(permission));
            }
        }
    }

    @Test
    @LocalData("createJobAdvancedFullConfig")
    void freestyleTest() throws Exception {
        initUser("alice");
        FreeStyleProject project = createProject(FreeStyleProject.class, "Free Style Project", "alice");
        assertEquals(project.getName().replaceAll(" ", "-"), project.getName());
        testOwnerRights(project, "alice");
        project.renameTo(project.getName() + " Renamed");
        assertEquals(project.getName().replaceAll(" ", "-"), project.getName());
    }

    @Test
    @LocalData("createJobAdvancedFullConfig")
    void mavenTest() throws Exception {
        initUser("alice");
        MavenModuleSet project = createProject(MavenModuleSet.class, "Maven Module Set", "alice");
        assertEquals(project.getName().replaceAll(" ", "-"), project.getName());
        testOwnerRights(project, "alice");
        assertTrue(project.isArchivingDisabled());
        MavenMailer m = project.getReporters().get(MavenMailer.class);
        assertNotNull(m);
        assertTrue(m.perModuleEmail);
        project.renameTo(project.getName() + " Renamed");
        assertEquals(project.getName().replaceAll(" ", "-"), project.getName());
    }

    @Test
    @LocalData("createJobAdvancedFullConfig")
    void folderTest() throws Exception {
        initUser("alice");
        Folder project = createProject(Folder.class, "User Folder", "alice");
        assertEquals(project.getName().replaceAll(" ", "-"), project.getName());
        testOwnerRights(project, "alice");
        project.renameTo(project.getName() + " Renamed");
        assertEquals(project.getName().replaceAll(" ", "-"), project.getName());
    }

    @Test
    @LocalData("createJobAdvancedFullConfig")
    void pipelineTest() throws Exception {
        initUser("alice");
        WorkflowJob project = createProject(WorkflowJob.class, "Workflow Job", "alice");
        assertEquals(project.getName().replaceAll(" ", "-"), project.getName());
        testOwnerRights(project, "alice");
        project.renameTo(project.getName() + " Renamed");
        assertEquals(project.getName().replaceAll(" ", "-"), project.getName());
    }

    @Test
    @LocalData("createJobAdvancedFullConfig")
    void multibranchTest() throws Exception {
        initUser("alice");
        WorkflowMultiBranchProject project =
                createProject(WorkflowMultiBranchProject.class, "Workflow Multi Branch Project", "alice");
        assertEquals(project.getName().replaceAll(" ", "-"), project.getName());
        testOwnerRights(project, "alice");
        project.renameTo(project.getName() + " Renamed");
        assertEquals(project.getName().replaceAll(" ", "-"), project.getName());
    }

    @Test
    @LocalData("createJobAdvancedFullConfig")
    void organizationFolderTest() throws Exception {
        initUser("alice");
        OrganizationFolder project = createProject(OrganizationFolder.class, "Organization Folder", "alice");
        assertEquals(project.getName().replaceAll(" ", "-"), project.getName());
        testOwnerRights(project, "alice");
        project.renameTo(project.getName() + " Renamed");
        assertEquals(project.getName().replaceAll(" ", "-"), project.getName());
    }

    private <T extends TopLevelItem> T createProject(Class<T> jobClass, String name, String creator) throws Exception {
        // This method creates a project of the specified type with the given name.
        T project;
        try (ACLContext ignored = ACL.as(User.get(creator, false, Collections.emptyMap()))) {
            project = r.createProject(jobClass, name);
        }
        return project;
    }

    private void testOwnerRights(AbstractItem item, String owner) {
        // This method checks if the owner rights are correctly set for the given job.
        for (Permission permission :
                new Permission[] {Item.CONFIGURE, Item.BUILD, Item.READ, Item.DELETE, Item.WORKSPACE}) {
            assertTrue(item.getACL()
                    .hasPermission2(
                            Objects.requireNonNull(User.get(owner, false, Collections.emptyMap()))
                                    .impersonate2(),
                            permission));
        }
    }

    @Test
    void testConfigureWithDefaultValues() throws Exception {
        // Simulate a request with default values
        JSONObject formData = new JSONObject();
        StaplerRequest2 req = mock(StaplerRequest2.class);

        CreateJobAdvancedPlugin plugin = r.jenkins.getPlugin(CreateJobAdvancedPlugin.class);
        assertNotNull(plugin);

        plugin.configure(req, formData);

        assertFalse(plugin.isAutoOwnerRights(), "Auto owner rights should be false by default");
        assertFalse(plugin.isAutoPublicBrowse(), "Auto public browse should be false by default");
        assertFalse(plugin.isReplaceSpace(), "Replace space should be false by default");
        assertFalse(plugin.isMvnArchivingDisabled(), "Maven archiving disabled should be false by default");
        assertFalse(plugin.isMvnPerModuleEmail(), "Maven per module email should be false by default");
        assertFalse(plugin.isActiveLogRotator(), "Active log rotator should be false by default");
        assertEquals(-1, plugin.getDaysToKeep(), "Days to keep should be -1 by default");
        assertEquals(-1, plugin.getNumToKeep(), "Number to keep should be -1 by default");
        assertEquals(-1, plugin.getArtifactDaysToKeep(), "Artifact days to keep should be -1 by default");
        assertEquals(-1, plugin.getArtifactNumToKeep(), "Artifact number to keep should be -1 by default");
        assertFalse(plugin.isActiveDynamicPermissions(), "Active dynamic permissions should be false by default");
        assertNull(plugin.getExtractPattern(), "Extract pattern should be null by default");
        assertTrue(
                plugin.getDynamicPermissionConfigs().isEmpty(),
                "Dynamic permission configs should be empty by default");
    }

    @Test
    void testConfigureWithLogRotator() throws Exception {
        // Simulate a request with log rotator configuration
        JSONObject formData = new JSONObject();
        JSONObject logRotatorConfig = new JSONObject();
        logRotatorConfig.put("daysToKeep", 5);
        logRotatorConfig.put("numToKeep", 10);
        logRotatorConfig.put("artifactDaysToKeep", 15);
        logRotatorConfig.put("artifactNumToKeep", 20);
        formData.put("activeLogRotator", logRotatorConfig);

        StaplerRequest2 req = mock(StaplerRequest2.class);

        CreateJobAdvancedPlugin plugin = r.jenkins.getPlugin(CreateJobAdvancedPlugin.class);
        assertNotNull(plugin);

        plugin.configure(req, formData);

        assertTrue(plugin.isActiveLogRotator(), "Active log rotator should be true");
        assertEquals(5, plugin.getDaysToKeep(), "Days to keep should be 5");
        assertEquals(10, plugin.getNumToKeep(), "Number to keep should be 10");
        assertEquals(15, plugin.getArtifactDaysToKeep(), "Artifact days to keep should be 15");
        assertEquals(20, plugin.getArtifactNumToKeep(), "Artifact number to keep should be 20");
    }

    @Test
    void testConfigureWithDynamicPermissionsSingle() throws Exception {
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

        StaplerRequest2 req = mock(StaplerRequest2.class);
        when(req.bindJSON(eq(DynamicPermissionConfig.class), any(JSONObject.class)))
                .thenReturn(new DynamicPermissionConfig("authenticated", (new HashSet<>())));

        CreateJobAdvancedPlugin plugin = r.jenkins.getPlugin(CreateJobAdvancedPlugin.class);
        assertNotNull(plugin);

        plugin.configure(req, formData);

        assertTrue(plugin.isActiveDynamicPermissions(), "Active dynamic permissions should be true");
        assertEquals(".*", plugin.getExtractPattern(), "Extract pattern should be .*");
        assertEquals(
                1, plugin.getDynamicPermissionConfigs().size(), "Dynamic permission configs should have one entry");

        DynamicPermissionConfig configuredPermCfg =
                plugin.getDynamicPermissionConfigs().get(0);
        assertEquals("authenticated", configuredPermCfg.getGroupFormat());

        assertEquals(
                checkedPermissions.size(),
                configuredPermCfg.getCheckedPermissionIds().size());

        // Check expected checked permissions match checked permissions
        for (Permission perm : checkedPermissions) {
            assertTrue(configuredPermCfg.isPermissionChecked(perm));
        }

        // Check that checked permissions match only expected checked permissions
        for (String permId : configuredPermCfg.getCheckedPermissionIds()) {
            assertTrue(checkedPermissions.contains(Permission.fromId(permId)));
        }
    }

    @Test
    void testConfigureWithDynamicPermissionsArray() throws Exception {
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

        StaplerRequest2 req = mock(StaplerRequest2.class);
        when(req.bindJSON(eq(DynamicPermissionConfig.class), any(JSONObject.class)))
                .thenReturn(new DynamicPermissionConfig("authenticated", (new HashSet<>())));

        CreateJobAdvancedPlugin plugin = r.jenkins.getPlugin(CreateJobAdvancedPlugin.class);
        assertNotNull(plugin);

        plugin.configure(req, formData);

        assertTrue(plugin.isActiveDynamicPermissions(), "Active dynamic permissions should be true");
        assertEquals(".*", plugin.getExtractPattern(), "Extract pattern should be .*");
        assertEquals(
                1, plugin.getDynamicPermissionConfigs().size(), "Dynamic permission configs should have one entry");

        DynamicPermissionConfig configuredPermCfg =
                plugin.getDynamicPermissionConfigs().get(0);
        assertEquals("authenticated", configuredPermCfg.getGroupFormat());

        assertEquals(
                checkedPermissions.size(),
                configuredPermCfg.getCheckedPermissionIds().size());

        // Check expected checked permissions match checked permissions
        for (Permission perm : checkedPermissions) {
            assertTrue(configuredPermCfg.isPermissionChecked(perm));
        }

        // Check that checked permissions match only expected checked permissions
        for (String permId : configuredPermCfg.getCheckedPermissionIds()) {
            assertTrue(checkedPermissions.contains(Permission.fromId(permId)));
        }
    }

    @Test
    void testImpliedByList() {
        String implies = CreateJobAdvancedPlugin.impliedByList(hudson.model.Item.DISCOVER);
        assertTrue(implies.contains("hudson.model.Item.Read"));
    }

    @Test
    void testImpliedByList2() {
        String implies = CreateJobAdvancedPlugin.impliedByList(null);
        assertTrue(implies.isEmpty());
    }
}
