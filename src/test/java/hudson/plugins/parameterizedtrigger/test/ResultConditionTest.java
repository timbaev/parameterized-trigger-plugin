/*
 * The MIT License
 *
 * Copyright (c) 2004-2010, Sun Microsystems, Inc., Tom Huybrechts
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

import static org.junit.jupiter.api.Assertions.*;

import hudson.model.Build;
import hudson.model.FreeStyleProject;
import hudson.model.Project;
import hudson.model.Queue;
import hudson.model.Result;
import hudson.plugins.parameterizedtrigger.BuildTrigger;
import hudson.plugins.parameterizedtrigger.BuildTriggerConfig;
import hudson.plugins.parameterizedtrigger.PredefinedBuildParameters;
import hudson.plugins.parameterizedtrigger.ResultCondition;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.FailureBuilder;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.SleepBuilder;
import org.jvnet.hudson.test.UnstableBuilder;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class ResultConditionTest {

    @Test
    void testTriggerByStableBuild(JenkinsRule r) throws Exception {
        Project projectA = r.createFreeStyleProject("projectA");
        Project projectB = r.createFreeStyleProject("projectB");
        projectB.setQuietPeriod(1);

        schedule(r, projectA, projectB, ResultCondition.SUCCESS);
        assertEquals(1, projectB.getLastBuild().getNumber());

        schedule(r, projectA, projectB, ResultCondition.FAILED);
        assertEquals(1, projectB.getLastBuild().getNumber());

        schedule(r, projectA, projectB, ResultCondition.UNSTABLE_OR_BETTER);
        assertEquals(2, projectB.getLastBuild().getNumber());

        schedule(r, projectA, projectB, ResultCondition.UNSTABLE);
        assertEquals(2, projectB.getLastBuild().getNumber());

        schedule(r, projectA, projectB, ResultCondition.UNSTABLE_OR_WORSE);
        assertEquals(2, projectB.getLastBuild().getNumber());

        schedule(r, projectA, projectB, ResultCondition.FAILED_OR_BETTER);
        assertEquals(3, projectB.getLastBuild().getNumber());
    }

    @Test
    void testTriggerByUnstableBuild(JenkinsRule r) throws Exception {
        Project projectA = r.createFreeStyleProject("projectA");
        projectA.getBuildersList().add(new UnstableBuilder());
        Project projectB = r.createFreeStyleProject("projectB");
        projectB.setQuietPeriod(1);

        schedule(r, projectA, projectB, ResultCondition.SUCCESS);
        assertNull(projectB.getLastBuild());

        schedule(r, projectA, projectB, ResultCondition.FAILED);
        assertNull(projectB.getLastBuild());

        schedule(r, projectA, projectB, ResultCondition.UNSTABLE_OR_BETTER);
        assertEquals(1, projectB.getLastBuild().getNumber());

        schedule(r, projectA, projectB, ResultCondition.UNSTABLE);
        assertEquals(2, projectB.getLastBuild().getNumber());

        schedule(r, projectA, projectB, ResultCondition.UNSTABLE_OR_WORSE);
        assertEquals(3, projectB.getLastBuild().getNumber());

        schedule(r, projectA, projectB, ResultCondition.FAILED_OR_BETTER);
        assertEquals(4, projectB.getLastBuild().getNumber());
    }

    private static void schedule(JenkinsRule r, Project projectA, Project projectB, ResultCondition condition)
            throws IOException, InterruptedException, ExecutionException {
        projectA.getPublishersList()
                .replace(new BuildTrigger(
                        new BuildTriggerConfig("projectB", condition, new PredefinedBuildParameters(""))));
        r.jenkins.rebuildDependencyGraph();
        projectA.scheduleBuild2(0).get();
        Queue.Item q = r.jenkins.getQueue().getItem(projectB);
        if (q != null) q.getFuture().get();
    }

    private static Build<?, ?> waitForBuildStarts(Project<?, ?> project, long timeoutMillis) throws Exception {
        long current = System.currentTimeMillis();
        while (project.getLastBuild() == null || !project.getLastBuild().isBuilding()) {
            assertTrue(System.currentTimeMillis() - current < timeoutMillis);
            Thread.sleep(100);
        }
        Build<?, ?> build = project.getLastBuild();
        assertTrue(build.isBuilding());
        assertNotNull(build.getExecutor());

        return build;
    }

    private static void scheduleAndAbort(
            JenkinsRule r, Project<?, ?> projectA, Project<?, ?> projectB, ResultCondition condition) throws Exception {
        projectA.getPublishersList()
                .replace(new BuildTrigger(
                        new BuildTriggerConfig(projectB.getFullName(), condition, new PredefinedBuildParameters(""))));
        r.jenkins.rebuildDependencyGraph();
        projectA.scheduleBuild2(0);
        Build<?, ?> build = waitForBuildStarts(projectA, 5000);
        build.getExecutor().interrupt();
        r.waitUntilNoActivity();
        r.assertBuildStatus(Result.ABORTED, build);
    }

    @Test
    void testTriggerByFailedBuild(JenkinsRule r) throws Exception {
        Project projectA = r.createFreeStyleProject("projectA");
        projectA.getBuildersList().add(new FailureBuilder());
        Project projectB = r.createFreeStyleProject("projectB");
        projectB.setQuietPeriod(1);

        schedule(r, projectA, projectB, ResultCondition.SUCCESS);
        assertNull(projectB.getLastBuild());

        schedule(r, projectA, projectB, ResultCondition.FAILED);
        assertEquals(1, projectB.getLastBuild().getNumber());

        schedule(r, projectA, projectB, ResultCondition.UNSTABLE_OR_BETTER);
        assertEquals(1, projectB.getLastBuild().getNumber());

        schedule(r, projectA, projectB, ResultCondition.UNSTABLE);
        assertEquals(1, projectB.getLastBuild().getNumber());

        schedule(r, projectA, projectB, ResultCondition.UNSTABLE_OR_WORSE);
        assertEquals(2, projectB.getLastBuild().getNumber());

        schedule(r, projectA, projectB, ResultCondition.FAILED_OR_BETTER);
        assertEquals(3, projectB.getLastBuild().getNumber());
    }

    @Test
    void testTriggerByAbortedBuild(JenkinsRule r) throws Exception {
        Project projectA = r.createFreeStyleProject("projectA");
        projectA.getBuildersList().add(new AbortedBuilder());
        Project projectB = r.createFreeStyleProject("projectB");
        projectB.setQuietPeriod(1);

        schedule(r, projectA, projectB, ResultCondition.SUCCESS);
        assertNull(projectB.getLastBuild());

        schedule(r, projectA, projectB, ResultCondition.FAILED);
        assertNull(projectB.getLastBuild());

        schedule(r, projectA, projectB, ResultCondition.UNSTABLE_OR_BETTER);
        assertNull(projectB.getLastBuild());

        schedule(r, projectA, projectB, ResultCondition.UNSTABLE);
        assertNull(projectB.getLastBuild());

        schedule(r, projectA, projectB, ResultCondition.UNSTABLE_OR_WORSE);
        assertEquals(1, projectB.getLastBuild().getNumber());

        schedule(r, projectA, projectB, ResultCondition.FAILED_OR_BETTER);
        assertEquals(1, projectB.getLastBuild().getNumber());

        schedule(r, projectA, projectB, ResultCondition.ALWAYS);
        assertEquals(2, projectB.getLastBuild().getNumber());
    }

    @Test
    void testTriggerByAbortedByInterrupted(JenkinsRule r) throws Exception {
        FreeStyleProject projectA = r.createFreeStyleProject("projectA");
        projectA.getBuildersList().add(new SleepBuilder(10000));
        FreeStyleProject projectB = r.createFreeStyleProject("projectB");
        projectB.setQuietPeriod(1);

        scheduleAndAbort(r, projectA, projectB, ResultCondition.SUCCESS);
        assertNull(projectB.getLastBuild());

        scheduleAndAbort(r, projectA, projectB, ResultCondition.FAILED);
        assertNull(projectB.getLastBuild());

        scheduleAndAbort(r, projectA, projectB, ResultCondition.UNSTABLE_OR_BETTER);
        assertNull(projectB.getLastBuild());

        scheduleAndAbort(r, projectA, projectB, ResultCondition.UNSTABLE);
        assertNull(projectB.getLastBuild());

        scheduleAndAbort(r, projectA, projectB, ResultCondition.UNSTABLE_OR_WORSE);
        assertEquals(1, projectB.getLastBuild().getNumber());

        scheduleAndAbort(r, projectA, projectB, ResultCondition.FAILED_OR_BETTER);
        assertEquals(1, projectB.getLastBuild().getNumber());

        scheduleAndAbort(r, projectA, projectB, ResultCondition.ALWAYS);
        assertEquals(2, projectB.getLastBuild().getNumber());
    }
}
