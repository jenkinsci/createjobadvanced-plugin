package hudson.plugins.createjobadvanced;

import hudson.Extension;
import hudson.model.Item;
import hudson.model.listeners.ItemListener;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerResponse2;

/**
 * Item listener in charge to apply plugin configuration on created or renamed items.
 */
@Extension
public class ItemListenerImpl extends ItemListener {
    /**
     * Plugin logger
     */
    private static final Logger log = Logger.getLogger(CreateJobAdvancedPlugin.class.getName());

    /**
     * Available Item configurers list
     */
    private final List<AbstractConfigurer<?, ?>> configurers = new ArrayList<>();

    /**
     * Class constructor in charge to initialize item configurers according to available plugins.
     */
    public ItemListenerImpl() {
        Jenkins instance = Jenkins.getInstanceOrNull();
        if (null != instance) {
            if (null != instance.getPlugin("maven-plugin")) {
                MavenConfigurer mavenConfigurer = new MavenConfigurer();
                log.finer(mavenConfigurer.toString());
                configurers.add(mavenConfigurer);
            } else {
                JobConfigurer jobConfigurer = new JobConfigurer();
                log.finer(jobConfigurer.toString());
                configurers.add(jobConfigurer);
            }
            if (null != instance.getPlugin("cloudbees-folder")) {
                FolderConfigurer folderConfigurer = new FolderConfigurer();
                log.finer(folderConfigurer.toString());
                configurers.add(folderConfigurer);
            }
        }
    }

    @Override
    public void onRenamed(Item item, String oldName, String newName) {
        final Object[] params = {oldName, newName};
        log.entering(getClass().getSimpleName(), "onRenamed", params);
        for (AbstractConfigurer<?, ?> configurer : configurers) {
            boolean isRespCommitted = false;
            try {
                String name = configurer.doRename(item);
                if (newName.compareTo(name) != 0) {
                    StaplerResponse2 rsp = Stapler.getCurrentResponse2();
                    if (null != rsp) {
                        isRespCommitted = rsp.isCommitted();
                        rsp.sendRedirect2("../" + name);
                    }
                }
            } catch (java.lang.IllegalStateException e) {
                log.log(Level.FINEST, "resp.isCommitted()={0}", isRespCommitted);
                if (false == isRespCommitted) {
                    log.log(Level.SEVERE, "error during sendRedirect2", e);
                }
            } catch (java.io.IOException e) {
                log.log(Level.SEVERE, "error during sendRedirect2", e);
            }
        }
        log.exiting(getClass().getSimpleName(), "onRenamed");
    }

    @Override
    public void onCreated(Item item) {
        for (AbstractConfigurer<?, ?> configurer : configurers) {
            configurer.doCreate(item);
        }
    }

    protected List<AbstractConfigurer<?, ?>> getConfigurers() {
        return configurers;
    }
}
