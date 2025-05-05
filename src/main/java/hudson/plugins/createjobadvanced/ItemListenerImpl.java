package hudson.plugins.createjobadvanced;

import hudson.Extension;
import hudson.model.Item;
import hudson.model.listeners.ItemListener;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;

@Extension
public class ItemListenerImpl extends ItemListener {

    private static final Logger log = Logger.getLogger(CreateJobAdvancedPlugin.class.getName());

    private List<AbstractConfigurer<?, ?>> configurers = new ArrayList<AbstractConfigurer<?, ?>>();

    @DataBoundConstructor
    public ItemListenerImpl() {
        log.finer("> ItemListenerImpl()");
        Jenkins instance = Jenkins.getInstanceOrNull();
        if (null != instance) {
            if (null != instance.getPlugin("maven-plugin")) {
                log.finer("> ItemListenerImpl() registering MavenConfigurer");
                MavenConfigurer mavenConfigurer = new MavenConfigurer();
                log.finer(mavenConfigurer.toString());
                configurers.add(mavenConfigurer);
                log.finer("< ItemListenerImpl() registered MavenConfigurer");
            } else {
                log.finer("> ItemListenerImpl() registering JobConfigurer");
                JobConfigurer jobConfigurer = new JobConfigurer();
                log.finer(jobConfigurer.toString());
                configurers.add(jobConfigurer);
                log.finer("< ItemListenerImpl() registered JobConfigurer");
            }
            if (null != instance.getPlugin("cloudbees-folder")) {
                log.finer("> ItemListenerImpl() registering FolderConfigurer");
                FolderConfigurer folderConfigurer = new FolderConfigurer();
                log.finer(folderConfigurer.toString());
                configurers.add(folderConfigurer);
                log.finer("< ItemListenerImpl() registered FolderConfigurer");
            }
        }
        log.finer("< ItemListenerImpl()");
    }

    @Override
    public void onRenamed(Item item, String oldName, String newName) {
        log.finer("> ItemListenerImpl.onRenamed()");

        for (AbstractConfigurer<?,?> configurer : configurers) {
            log.finer("> " + configurer.getClass().getName() + ".onRenamed()");
            configurer.onRenamed(item);
            log.finer("< " + configurer.getClass().getName() + ".onRenamed()");
        }

        log.finer("< ItemListenerImpl.onRenamed()");
    }

    @Override
    public void onCreated(Item item) {
        log.finer("> ItemListenerImpl.onCreated()");

        for (AbstractConfigurer<?,?> configurer : configurers) {
            log.finer("> " + configurer.getClass().getName() + ".onCreated()");
            configurer.onCreated(item);
            log.finer("< " + configurer.getClass().getName() + ".onCreated()");
        }

        log.finer("< ItemListenerImpl.onCreated()");
    }
}
