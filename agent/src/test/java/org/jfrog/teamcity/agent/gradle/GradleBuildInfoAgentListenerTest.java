package org.jfrog.teamcity.agent.gradle;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import jetbrains.buildServer.ExtensionHolder;
import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.agent.ArtifactsPublisher;
import jetbrains.buildServer.agent.BuildAgentConfiguration;
import jetbrains.buildServer.agent.BuildParametersMap;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.BuildRunnerContext;
import jetbrains.buildServer.agent.impl.BuildRunnerContextImpl;
import jetbrains.buildServer.util.EventDispatcher;
import org.easymock.EasyMock;
import org.jfrog.teamcity.common.ConstantValues;
import org.jfrog.teamcity.common.RunTypeUtils;
import org.jfrog.teamcity.common.RunnerParameterKeys;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Noam Y. Tenne
 */
public class GradleBuildInfoAgentListenerTest {

    private GradleBuildInfoAgentListener listener;
    private ExtensionHolder holder;

    @BeforeClass
    public void setUp() throws Exception {
        EventDispatcher eventDispatcher = EasyMock.createMock(EventDispatcher.class);
        eventDispatcher.addListener(EasyMock.isA(GradleBuildInfoAgentListener.class));
        EasyMock.expectLastCall();

        holder = EasyMock.createMock(ExtensionHolder.class);

        EasyMock.replay(eventDispatcher, holder);
        listener = new GradleBuildInfoAgentListener(eventDispatcher, holder);
        EasyMock.verify(eventDispatcher, holder);
    }

    @Test
    public void testBeforeRunnerStartButNotGradleRunner() throws Exception {
        BuildRunnerContext runner = EasyMock.createMock(BuildRunnerContext.class);

        EasyMock.expect(runner.getRunnerParameters()).andReturn(Maps.<String, String>newHashMap());
        EasyMock.expect(runner.getRunType()).andReturn("notgradle");
        EasyMock.replay(runner);
        listener.beforeRunnerStart(runner);
        EasyMock.verify(runner);
    }

    @Test
    public void testBeforeRunnerStartButNoIntegrationEnabledParam() throws Exception {
        BuildRunnerContext runner = EasyMock.createMock(BuildRunnerContext.class);

        EasyMock.expect(runner.getRunnerParameters()).andReturn(Maps.<String, String>newHashMap());
        EasyMock.expect(runner.getRunType()).andReturn(RunTypeUtils.GRADLE_RUNNER);
        EasyMock.replay(runner);
        listener.beforeRunnerStart(runner);
        EasyMock.verify(runner);
    }

    @Test
    public void testBeforeRunnerStartButFalseIntegrationEnabledParam() throws Exception {
        BuildRunnerContext runner = EasyMock.createMock(BuildRunnerContext.class);

        HashMap<String, String> runnerParameters = Maps.newHashMap();
        runnerParameters.put(RunnerParameterKeys.GRADLE_INTEGRATION, Boolean.FALSE.toString());
        EasyMock.expect(runner.getRunnerParameters()).andReturn(runnerParameters);
        EasyMock.expect(runner.getRunType()).andReturn(RunTypeUtils.GRADLE_RUNNER);
        EasyMock.replay(runner);
        listener.beforeRunnerStart(runner);
        EasyMock.verify(runner);
    }

    @Test
    public void testBeforeRunnerStartButNoServerUrlAndNoSkipLogMessage() throws Exception {
        BuildRunnerContext runner = EasyMock.createMock(BuildRunnerContext.class);

        HashMap<String, String> runnerParameters = Maps.newHashMap();
        runnerParameters.put(RunnerParameterKeys.GRADLE_INTEGRATION, Boolean.TRUE.toString());
        EasyMock.expect(runner.getRunnerParameters()).andReturn(runnerParameters);
        EasyMock.expect(runner.getRunType()).andReturn(RunTypeUtils.GRADLE_RUNNER);

        BuildProgressLogger buildProgressLogger = EasyMock.createMock(BuildProgressLogger.class);
        AgentRunningBuild agentRunningBuild = EasyMock.createMock(AgentRunningBuild.class);
        EasyMock.expect(agentRunningBuild.getBuildLogger()).andReturn(buildProgressLogger);
        EasyMock.expect(runner.getBuild()).andReturn(agentRunningBuild);

        EasyMock.replay(runner, buildProgressLogger, agentRunningBuild);
        listener.beforeRunnerStart(runner);
        EasyMock.verify(runner, buildProgressLogger, agentRunningBuild);
    }

    @Test
    public void testBeforeRunnerStartButNoServerUrl() throws Exception {
        BuildRunnerContext runner = EasyMock.createMock(BuildRunnerContext.class);

        HashMap<String, String> runnerParameters = Maps.newHashMap();
        runnerParameters.put(RunnerParameterKeys.GRADLE_INTEGRATION, Boolean.TRUE.toString());
        runnerParameters.put(ConstantValues.PROP_SKIP_LOG_MESSAGE, "skipMessage");
        EasyMock.expect(runner.getRunnerParameters()).andReturn(runnerParameters);
        EasyMock.expect(runner.getRunType()).andReturn(RunTypeUtils.GRADLE_RUNNER);

        BuildProgressLogger buildProgressLogger = EasyMock.createMock(BuildProgressLogger.class);
        buildProgressLogger.warning("skipMessage");
        EasyMock.expectLastCall();
        AgentRunningBuild agentRunningBuild = EasyMock.createMock(AgentRunningBuild.class);
        EasyMock.expect(agentRunningBuild.getBuildLogger()).andReturn(buildProgressLogger);

        EasyMock.expect(runner.getBuild()).andReturn(agentRunningBuild);

        EasyMock.replay(runner, buildProgressLogger, agentRunningBuild);
        listener.beforeRunnerStart(runner);
        EasyMock.verify(runner, buildProgressLogger, agentRunningBuild);
    }

    @Test
    public void testBeforeRunnerStartButNoGradlePropsFile() throws Exception {
        BuildRunnerContext runner = EasyMock.createMock(BuildRunnerContext.class);

        HashMap<String, String> runnerParameters = Maps.newHashMap();
        runnerParameters.put(RunnerParameterKeys.GRADLE_INTEGRATION, Boolean.TRUE.toString());
        runnerParameters.put(RunnerParameterKeys.URL, "someUrl");
        runnerParameters.put(RunnerParameterKeys.ENABLE_RELEASE_MANAGEMENT, Boolean.TRUE.toString());
        EasyMock.expect(runner.getRunnerParameters()).andReturn(runnerParameters);
        EasyMock.expect(runner.getRunType()).andReturn(RunTypeUtils.GRADLE_RUNNER);

        BuildProgressLogger buildProgressLogger = EasyMock.createMock(BuildProgressLogger.class);
        buildProgressLogger.error(EasyMock.isA(String.class));
        EasyMock.expectLastCall();
        buildProgressLogger.buildFailureDescription(EasyMock.isA(String.class));
        EasyMock.expectLastCall();
        buildProgressLogger.exception(EasyMock.isA(FileNotFoundException.class));
        EasyMock.expectLastCall();

        AgentRunningBuild agentRunningBuild = EasyMock.createMock(AgentRunningBuild.class);
        EasyMock.expect(agentRunningBuild.getBuildLogger()).andReturn(buildProgressLogger);
        EasyMock.expect(runner.getWorkingDirectory()).andReturn(new File("doesntexist"));

        EasyMock.expect(runner.getBuild()).andReturn(agentRunningBuild);

        EasyMock.replay(runner, buildProgressLogger, agentRunningBuild);
        listener.beforeRunnerStart(runner);
        EasyMock.verify(runner, buildProgressLogger, agentRunningBuild);
    }

    @Test
    public void testBeforeRunnerStart() throws Exception {
        BuildRunnerContextImpl runner = EasyMock.createMock(BuildRunnerContextImpl.class);

        HashMap<String, String> runnerParameters = Maps.newHashMap();
        runnerParameters.put(ConstantValues.BUILD_NAME, "buildName");
        runnerParameters.put(ConstantValues.BUILD_NUMBER, "123123");
        runnerParameters.put(ConstantValues.PROP_BUILD_TIMESTAMP, "asdsafasdf");
        runnerParameters.put(ConstantValues.PROP_VCS_REVISION, "asdasdasd");
        runnerParameters.put(RunnerParameterKeys.GRADLE_INTEGRATION, Boolean.TRUE.toString());
        runnerParameters.put(RunnerParameterKeys.URL, "someUrl");
        runnerParameters.put(RunnerParameterKeys.ENABLE_RELEASE_MANAGEMENT, Boolean.TRUE.toString());
        runnerParameters.put("ui.gradleRunner.additional.gradle.cmd.params", "cmdParam");
        runnerParameters.put("ui.gradleRunner.gradle.tasks.names", "someTask");
        EasyMock.expect(runner.getRunnerParameters()).andReturn(runnerParameters).anyTimes();
        EasyMock.expect(runner.getConfigParameters()).andReturn(Maps.<String, String>newHashMap()).anyTimes();
        runner.addRunnerParameter("ui.gradleRunner.gradle.tasks.names", "someTask artifactoryPublish");
        EasyMock.expectLastCall();
        runner.addRunnerParameter(EasyMock.eq("ui.gradleRunner.additional.gradle.cmd.params"),
                EasyMock.matches("cmdParam -I .+ -DbuildInfoConfig.propertiesFile=.+"));
        EasyMock.expectLastCall();

        BuildParametersMap buildParametersMap = EasyMock.createMock(BuildParametersMap.class);
        EasyMock.expect(buildParametersMap.getAllParameters()).andReturn(Maps.<String, String>newHashMap()).anyTimes();

        EasyMock.expect(runner.getBuildParameters()).andReturn(buildParametersMap).anyTimes();
        EasyMock.expect(runner.getRunType()).andReturn(RunTypeUtils.GRADLE_RUNNER);

        AgentRunningBuild agentRunningBuild = EasyMock.createMock(AgentRunningBuild.class);
        EasyMock.expect(agentRunningBuild.getBuildLogger())
                .andReturn(EasyMock.createMock(BuildProgressLogger.class)).anyTimes();
        File tempDir = Files.createTempDir();
        new File(tempDir, ConstantValues.GRADLE_PROPERTIES_FILE_NAME).createNewFile();
        EasyMock.expect(runner.getWorkingDirectory()).andReturn(tempDir);
        EasyMock.expect(agentRunningBuild.getSharedConfigParameters()).andReturn(Maps.<String, String>newHashMap());

        BuildAgentConfiguration buildAgentConfiguration = EasyMock.createMock(BuildAgentConfiguration.class);
        EasyMock.expect(buildAgentConfiguration.getAgentPluginsDirectory()).andReturn(Files.createTempDir());

        EasyMock.expect(agentRunningBuild.getAgentConfiguration()).andReturn(buildAgentConfiguration);
        EasyMock.expect(runner.getBuild()).andReturn(agentRunningBuild).times(2);

        ArtifactsPublisher artifactsPublisher = EasyMock.createMock(ArtifactsPublisher.class);
        EasyMock.expect(artifactsPublisher.publishFiles(EasyMock.isA(Map.class))).andReturn(-1);

        EasyMock.reset(holder);
        EasyMock.expect(holder.getExtensions(ArtifactsPublisher.class)).andReturn(Sets.newHashSet(artifactsPublisher));

        EasyMock.replay(runner, buildParametersMap, agentRunningBuild, holder, artifactsPublisher,
                buildAgentConfiguration);
        listener.beforeRunnerStart(runner);
        EasyMock.verify(runner, buildParametersMap, agentRunningBuild, holder, artifactsPublisher,
                buildAgentConfiguration);
    }
}
