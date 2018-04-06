package org.jenkinsci.plugins.radargun.config;

import hudson.DescriptorExtensionList;
import hudson.FilePath;
import hudson.model.Describable;
import hudson.model.AbstractBuild;
import hudson.model.Descriptor;

import java.io.IOException;

import jenkins.model.Jenkins;

import org.jenkinsci.plugins.radargun.util.Resolver;

/**
 * Base class for RG scenarios providers. Takes care about loading scenarios, expanding Jenkins variables and createing/deleteing temporal scenario file with resolved variables.
 * 
 * @author vjuranek
 * 
 */
public abstract class ScenarioSource implements Describable<ScenarioSource> {

    protected static final String DEFAULT_SCENARIO_NAME = "radarGunScenario";
    protected static final String DEFAULT_SCENARIO_SUFFIX = ".xml";

    private transient String tmpScenarioPath;
    private transient FilePath tmpScenario;

    protected abstract FilePath createTmpScenrioFile(AbstractBuild<?, ?> build) throws InterruptedException, IOException;

    public String getTmpScenarioPath(AbstractBuild<?, ?> build) throws InterruptedException, IOException {
        tmpScenario = createTmpScenrioFile(build);
        tmpScenarioPath = tmpScenario.getRemote();
        return tmpScenarioPath;
    }

    /**
     * Replace parameters in scenario and stores scenario into tmp file in workspace
     */
    public FilePath tmpScenarioFromContent(String scenarioContent, AbstractBuild<?, ?> build) throws InterruptedException, IOException {
        // String scenario = Resolver.buildVar(build, scenarioContent);
        // TODO env. var expansion? Expand on node where it will be launched
        FilePath path = null;
        FilePath ws = build.getWorkspace();
        if (ws != null) {
            path = ws.createTextTempFile(DEFAULT_SCENARIO_NAME, DEFAULT_SCENARIO_SUFFIX, scenarioContent, true);
        }
        return path;
    }

    public FilePath tmpScenarioFromFile(String scenarioPath, AbstractBuild<?, ?> build) throws InterruptedException, IOException {
        String path = Resolver.buildVar(build, scenarioPath);
        FilePath fp = new FilePath(build.getWorkspace(), path);
        String scenarioContent = fp.readToString(); // TODO not very safe, if e.g. some malicious user provide path to
                                                    // huge file
        // TODO env. var expansion? Expand on node where it will be launched
        return tmpScenarioFromContent(scenarioContent, build);
    }

    public void cleanup() throws InterruptedException, IOException {
        if (tmpScenario != null) {
            tmpScenario.delete();
            tmpScenario = null;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Descriptor<ScenarioSource> getDescriptor() {
        return Jenkins.getInstance().getDescriptorOrDie(getClass());
    }

    public static final DescriptorExtensionList<ScenarioSource, Descriptor<ScenarioSource>> all() {
        return Jenkins.getInstance().getDescriptorList(ScenarioSource.class);
    }

    public static abstract class ScenarioSourceDescriptor extends Descriptor<ScenarioSource> {
    }

}
