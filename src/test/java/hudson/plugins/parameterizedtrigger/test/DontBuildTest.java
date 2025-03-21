/*
 * The MIT License
 *
 * Copyright (c) 2004-2010, Sun Microsystems, Inc., Kohsuke Kawaguchi
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
package hudson.plugins.parameterizedtrigger.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hudson.model.AbstractBuild;
import hudson.model.Action;
import hudson.model.Project;
import hudson.model.TaskListener;
import hudson.plugins.parameterizedtrigger.AbstractBuildParameters;
import hudson.plugins.parameterizedtrigger.BuildTrigger;
import hudson.plugins.parameterizedtrigger.BuildTriggerConfig;
import hudson.plugins.parameterizedtrigger.ResultCondition;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class DontBuildTest {

    public static final class DontBuildTrigger extends AbstractBuildParameters {
        boolean called = false;

        @Override
        public Action getAction(AbstractBuild<?, ?> build, TaskListener listener) throws DontTriggerException {
            called = true;
            throw new DontTriggerException();
        }
    }

    @Test
    void test(JenkinsRule r) throws Exception {

        Project projectA = r.createFreeStyleProject("projectA");
        DontBuildTrigger dbt = new DontBuildTrigger();
        projectA.getPublishersList()
                .add(new BuildTrigger(new BuildTriggerConfig("projectB", ResultCondition.SUCCESS, dbt)));

        Project projectB = r.createFreeStyleProject("projectB");
        projectB.setQuietPeriod(0);
        r.jenkins.rebuildDependencyGraph();

        projectA.scheduleBuild2(0).get();
        Thread.sleep(1000);

        assertEquals(0, projectB.getBuilds().size());
        assertTrue(dbt.called, "trigger was not called");
    }
}
