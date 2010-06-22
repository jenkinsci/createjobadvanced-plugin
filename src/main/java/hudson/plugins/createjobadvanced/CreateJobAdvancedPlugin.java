package hudson.plugins.createjobadvanced;

import hudson.Plugin;
import hudson.Util;
import hudson.model.Descriptor.FormException;

import java.io.IOException;
import java.util.logging.Logger;

import javax.servlet.ServletException;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.StaplerRequest;

/**
 * @plugin
 * @author Bertrand Gressier
 * 
 */

public class CreateJobAdvancedPlugin extends Plugin {

    static Logger log = Logger.getLogger(CreateJobAdvancedPlugin.class.getName());

    private boolean autoOwnerRights;
    private boolean autoPublicBrowse;
    private boolean replaceSpace;

    private boolean activeLogRotator;
    private int daysToKeep=-1;
    private int numToKeep=-1;
    private int artifactDaysToKeep=-1;
    private int artifactNumToKeep=-1;

    public CreateJobAdvancedPlugin() {
    }

    @Override
    public void start() throws Exception {
	// TODO Auto-generated method stub
	super.start();
	log.info("Create job advanced plugin started ...");
	load();
    }

    @Override
    public void configure(StaplerRequest req, JSONObject formData) throws IOException, ServletException, FormException {

	// formData.optBoolean("autoOwnerRights",autoOwnerRights);

	if (req.getParameter("cja.security") == null || req.getParameter("cja.security") == "false") {
	    autoOwnerRights = false;
	} else {
	    autoOwnerRights = true;
	}

	if (req.getParameter("cja.public") == null || req.getParameter("cja.public") == "false") {
	    autoPublicBrowse = false;
	} else {
	    autoPublicBrowse = true;
	}

	if (req.getParameter("cja.jobspacesinname") == null || req.getParameter("cja.jobspacesinname") == "false") {
	    replaceSpace = false;
	} else {
	    replaceSpace = true;
	}

	if (req.getParameter("cja.activeLogRotator") == null || req.getParameter("cja.activeLogRotator") == "false") {
	    activeLogRotator = false;
	} else {
	    activeLogRotator = true;
	}

	if (activeLogRotator) {

	    try {
		daysToKeep = Integer.valueOf(Util.fixNull(req.getParameter("cja.daysToKeep")));
	    } catch (Exception e) {
		daysToKeep = -1;
	    }
	    try {
		numToKeep = Integer.valueOf(Util.fixNull(req.getParameter("cja.numToKeep")));
	    } catch (Exception e) {
		numToKeep = -1;
	    }
	    try {
		artifactDaysToKeep = Integer.valueOf(Util.fixNull(req.getParameter("cja.artifactDaysToKeep")));
	    } catch (Exception e) {
		artifactDaysToKeep = -1;
	    }
	    try {
		artifactNumToKeep = Integer.valueOf(Util.fixNull(req.getParameter("cja.artifactNumToKeep")));
	    } catch (Exception e) {
		artifactNumToKeep = -1;
	    }
	}

	save();
    }

    public boolean isAutoOwnerRights() {
	return autoOwnerRights;
    }

    public boolean isAutoPublicBrowse() {
	return autoPublicBrowse;
    }

    public boolean isReplaceSpace() {
	return replaceSpace;
    }

    public boolean isActiveLogRotator() {
	return activeLogRotator;
    }

    public int getDaysToKeep() {
	return daysToKeep;
    }

    public int getNumToKeep() {
	return numToKeep;
    }

    public int getArtifactDaysToKeep() {
	return artifactDaysToKeep;
    }

    public int getArtifactNumToKeep() {
	return artifactNumToKeep;
    }
}
