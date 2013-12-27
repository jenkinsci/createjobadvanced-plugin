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

import hudson.maven.MavenModuleSet;
import hudson.maven.reporters.MavenMailer;
import hudson.model.Hudson;
import hudson.model.Job;

/**
 * Changes the configuration of {@link MavenModuleSet}s.
 * 
 * @author Dominik Bartholdi (imod)
 */
public class MavenConfigurer {

    public MavenConfigurer() {
    }

    public void onCreated(Job<?, ?> job) {
        if (job instanceof MavenModuleSet) {
            preConfigureMavenJob((MavenModuleSet) job);
        }
    }

    private void preConfigureMavenJob(MavenModuleSet job) {
        final CreateJobAdvancedPlugin cja = Hudson.getInstance().getPlugin(CreateJobAdvancedPlugin.class);
        job.setIsArchivingDisabled(cja.isMvnArchivingDisabled());
        MavenMailer m = job.getReporters().get(MavenMailer.class);
        if(m != null) {
            m.perModuleEmail = cja.isMvnPerModuleEmail();
        } else {
            job.getReporters().add(new MavenMailer(null, true, false, cja.isMvnPerModuleEmail()));
        }
    }

}
