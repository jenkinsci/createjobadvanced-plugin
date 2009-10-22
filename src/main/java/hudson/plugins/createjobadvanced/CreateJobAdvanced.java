package hudson.plugins.createjobadvanced;

import hudson.Extension;
import hudson.model.Hudson;
import hudson.model.Item;
import hudson.model.Job;
import hudson.model.listeners.ItemListener;
import hudson.security.AuthorizationMatrixProperty;
import hudson.security.Permission;
import hudson.security.SecurityMode;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.kohsuke.stapler.DataBoundConstructor;

/**
 * @plugin
 * @author Bertrand Gressier
 *
 */
@Extension
public class CreateJobAdvanced extends ItemListener {
	
	static Logger log = Logger.getLogger(CreateJobAdvanced.class.getName());
	
	@DataBoundConstructor
	public CreateJobAdvanced() {
		log.info("Create job advanced started");
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void onCreated(Item item) {
		
		//test if the item is a job
		if (! (item instanceof Job))
			return;
		
		//hudson must activate security mode for using
		 if (Hudson.getInstance().getSecurity().equals(SecurityMode.UNSECURED))
			return;
		
		Job job = (Job)item;
		
		//if you create the job with template, need to get informations
		AuthorizationMatrixProperty auth = (AuthorizationMatrixProperty)job.getProperty(AuthorizationMatrixProperty.class);
		if (auth == null){
			auth = new AuthorizationMatrixProperty();
		}
		
		auth.setUseProjectSecurity(true);
		String sid = Hudson.getAuthentication().getName();
		
		try {
			/* Problem !! I need to add configuration in Authorization matrix property and it's unpossible
			 * because the method is protected. 
			*/
			Method add = auth.getClass().getDeclaredMethod("add", Permission.class,String.class);
			add.setAccessible(true);
			add.invoke(auth, Item.CONFIGURE,sid);
			add.invoke(auth, Item.READ,sid);
			add.invoke(auth, Item.BUILD,sid);
			add.invoke(auth, Item.WORKSPACE,sid);
			add.invoke(auth, Item.DELETE,sid);
			job.addProperty(auth);
			
			log.info("Create Job " + item.getDisplayName() +" with right on " + sid);
						
		} catch (IOException e) {
			log.log(Level.SEVERE,"problem to set right to owner",e);
		} catch (NoSuchMethodException e) {
			log.log(Level.SEVERE,"can't modify add method from protected to public",e);
		} catch (IllegalArgumentException e) {
			log.log(Level.SEVERE,"can't modify add method from protected to public",e);
		} catch (IllegalAccessException e) {
			log.log(Level.SEVERE,"can't modify add method from protected to public",e);
		} catch (InvocationTargetException e) {
			log.log(Level.SEVERE,"can't modify add method from protected to public",e);
		}
	}
}
