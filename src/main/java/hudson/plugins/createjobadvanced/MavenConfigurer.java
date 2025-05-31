/*
 * The MIT License
 *
 * Copyright (c) 2012, Dominik Bartholdi
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.plugins.createjobadvanced;

import edu.umd.cs.findbugs.annotations.Nullable;
import hudson.maven.MavenModuleSet;
import hudson.maven.reporters.MavenMailer;
import hudson.model.Item;

/**
 * Changes the configuration of {@link MavenModuleSet} items.
 *
 * @author Dominik Bartholdi (imod)
 * @author Laurent Coltat
 */
public final class MavenConfigurer extends JobConfigurer {

    /**
     * Class constructor
     */
    protected MavenConfigurer() {}

    @Override
    public final void doCreate(Item item) {
        log.finer("> " + this.getClass().getName() + ".onCreated()");
        super.doCreate(item);
        if ((item instanceof MavenModuleSet)) {
            MavenModuleSet mavenModuleSet = (MavenModuleSet) item;
            preConfigureMavenJob(mavenModuleSet);
        }
        log.finer("< " + this.getClass().getName() + ".onCreated()");
    }

    /**
     *
     * @param mavenModuleSet
     */
    private final void preConfigureMavenJob(@Nullable MavenModuleSet mavenModuleSet) {
        final CreateJobAdvancedPlugin cja = getPlugin();
        if (null != cja) {
            log.finer("> " + this.getClass().getName() + ".preConfigureMavenJob(MavenModuleSet)");
            mavenModuleSet.setIsArchivingDisabled(cja.isMvnArchivingDisabled());
            MavenMailer m = mavenModuleSet.getReporters().get(MavenMailer.class);
            if (m != null) {
                m.perModuleEmail = cja.isMvnPerModuleEmail();
            } else {
                mavenModuleSet.getReporters().add(new MavenMailer(null, true, false, cja.isMvnPerModuleEmail()));
            }
            log.finer("< " + this.getClass().getName() + ".preConfigureMavenJob(MavenModuleSet)");
        }
    }
}
