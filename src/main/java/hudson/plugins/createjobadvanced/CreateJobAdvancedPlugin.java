package hudson.plugins.createjobadvanced;

import hudson.Plugin;
import hudson.model.Descriptor.FormException;
import hudson.security.Permission;
import hudson.security.PermissionGroup;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.StaplerRequest2;

/**
 * @author Bertrand Gressier
 *
 */
public class CreateJobAdvancedPlugin extends Plugin {

    private static final Logger log = Logger.getLogger(CreateJobAdvancedPlugin.class.getName());

    private boolean autoOwnerRights;
    private boolean autoPublicBrowse;
    private boolean replaceSpace;

    private boolean activeLogRotator;
    private int daysToKeep = -1;
    private int numToKeep = -1;
    private int artifactDaysToKeep = -1;
    private int artifactNumToKeep = -1;

    private boolean activeDynamicPermissions;
    private String extractPattern;

    private boolean mvnArchivingDisabled;
    private boolean mvnPerModuleEmail;

    private List<DynamicPermissionConfig> dynamicPermissionConfigs = new ArrayList<DynamicPermissionConfig>();

    /**
     * @return the dynamicPermissionConfigs
     */
    public List<DynamicPermissionConfig> getDynamicPermissionConfigs() {
        return dynamicPermissionConfigs;
    }

    @Deprecated
    public CreateJobAdvancedPlugin() {}

    @Override
    public void start() throws Exception {
        super.start();
        log.info("Create job advanced plugin started ...");
        load();
    }

    @Override
    public void configure(StaplerRequest2 req, JSONObject formData)
            throws IOException, ServletException, FormException {

        autoOwnerRights = formData.optBoolean("security", false);
        autoPublicBrowse = formData.optBoolean("public", false);
        replaceSpace = formData.optBoolean("jobspacesinname", false);

        mvnArchivingDisabled = formData.optBoolean("mvnArchivingDisabled", false);
        mvnPerModuleEmail = formData.optBoolean("mvnPerModuleEmail", false);

        final JSONObject activeLogRotatorJson = formData.optJSONObject("activeLogRotator");

        if (activeLogRotatorJson != null) {
            activeLogRotator = true;
            daysToKeep = activeLogRotatorJson.optInt("daysToKeep", -1);
            numToKeep = activeLogRotatorJson.optInt("numToKeep", -1);
            artifactDaysToKeep = activeLogRotatorJson.optInt("artifactDaysToKeep", -1);
            artifactNumToKeep = activeLogRotatorJson.optInt("artifactNumToKeep", -1);
        } else {
            activeLogRotator = false;
        }

        final JSONObject activeDynamicPermissionsJson = formData.optJSONObject("activeDynamicPermissions");

        if (activeDynamicPermissionsJson != null) {
            activeDynamicPermissions = true;
            extractPattern = activeDynamicPermissionsJson.optString("extractPattern", "");

            dynamicPermissionConfigs.clear();
            final Object cfgs = activeDynamicPermissionsJson.get("cfgs");
            if (cfgs instanceof JSONArray) {
                final JSONArray jsonArray = (JSONArray) cfgs;
                for (Object object : jsonArray) {
                    addDynamicPermission(req, (JSONObject) object);
                }
            } else {
                // there might be only one single dynamic permission
                addDynamicPermission(req, (JSONObject) cfgs);
            }

        } else {
            activeDynamicPermissions = false;
            extractPattern = null;
            dynamicPermissionConfigs.clear();
        }

        save();
    }

    /**
     * adds a dynamic permission configuration with the data extracted form the
     * jsonObject.
     *
     * @param req
     * @param jsonObject
     */
    private void addDynamicPermission(StaplerRequest2 req, JSONObject jsonObject) {
        final DynamicPermissionConfig dynPerm = req.bindJSON(DynamicPermissionConfig.class, jsonObject);

        // add the enabled permission ids
        final Map<String, List<Permission>> allPossiblePermissions = getAllPossiblePermissions();
        for (Map.Entry<String, List<Permission>> entry : allPossiblePermissions.entrySet()) {
            for (Permission permission : entry.getValue()) {
                final String enabled = jsonObject.getString(permission.getId());
                if (Boolean.valueOf(enabled)) {
                    dynPerm.addPermissionId(permission.getId());
                    log.log(Level.FINE, "enable {0}", new String[] {permission.getId()});
                }
            }
        }

        dynamicPermissionConfigs.add(dynPerm);
    }

    /**
     *
     * @return
     */
    public static Map<String, List<Permission>> getAllPossiblePermissions() {
        final Map<String, List<Permission>> enabledPerms = new TreeMap<String, List<Permission>>();

        addEnabledPermissionsForGroup(enabledPerms, hudson.model.Item.class);
        addEnabledPermissionsForGroup(enabledPerms, hudson.model.Run.class);

        return enabledPerms;
    }

    /**
     *
     * @param p
     * @return
     */
    public static String impliedByList(Permission p) {
        List<Permission> impliedBys = new ArrayList<>();
        while (p.impliedBy != null) {
            p = p.impliedBy;
            impliedBys.add(p);
        }
        return StringUtils.join(impliedBys.stream().map(Permission::getId).collect(Collectors.toList()), " ");
    }

    /**
     *
     * @param allEnabledPerms
     * @param owner
     */
    private static void addEnabledPermissionsForGroup(
            final Map<String, List<Permission>> allEnabledPerms, Class<?> owner) {
        final PermissionGroup permissionGroup = PermissionGroup.get(owner);
        if (permissionGroup != null) {
            final List<Permission> enabledPerms = new ArrayList<Permission>();
            List<Permission> permissions = permissionGroup.getPermissions();
            for (Permission permission : permissions) {
                if (permission.enabled) {
                    enabledPerms.add(permission);
                }
            }
            if (enabledPerms.size() > 0) {
                allEnabledPerms.put(permissionGroup.title.toString(), enabledPerms);
            }
        }
    }

    /**
     *
     * @return true when automatic owner right assigment  option is activated
     */
    public boolean isAutoOwnerRights() {
        return autoOwnerRights;
    }

    /**
     *
     * @return true when automatic public browse assigment option is activated
     */
    public boolean isAutoPublicBrowse() {
        return autoPublicBrowse;
    }

    /**
     *
     * @return true when replace space option is activated
     */
    public boolean isReplaceSpace() {
        return replaceSpace;
    }

    /**
     *
     * @return true when log rotator option is activated
     */
    public boolean isActiveLogRotator() {
        return activeLogRotator;
    }

    /**
     *
     * @return the days to keep builds
     */
    public int getDaysToKeep() {
        return daysToKeep;
    }

    /**
     *
     * @return the number of build to be kept
     */
    public int getNumToKeep() {
        return numToKeep;
    }

    /**
     *
     * @return the days to keep build artifacts
     */
    public int getArtifactDaysToKeep() {
        return artifactDaysToKeep;
    }

    /**
     *
     * @return the number of build to keep with artifacts
     */
    public int getArtifactNumToKeep() {
        return artifactNumToKeep;
    }

    /**
     * @return the extractPattern
     */
    public String getExtractPattern() {
        return extractPattern;
    }

    /**
     * @return the activeDynamicPermissions
     */
    public boolean isActiveDynamicPermissions() {
        return activeDynamicPermissions;
    }

    /**
     *
     * @return
     */
    public boolean isMvnArchivingDisabled() {
        return mvnArchivingDisabled;
    }

    /**
     *
     * @return
     */
    public boolean isMvnPerModuleEmail() {
        return mvnPerModuleEmail;
    }
}
