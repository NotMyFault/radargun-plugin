package org.jenkinsci.plugins.radargun;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.jenkinsci.plugins.radargun.model.RgScriptConfig;
import org.kohsuke.stapler.DataBoundConstructor;

import hudson.AbortException;
import hudson.EnvVars;
import hudson.Extension;
import hudson.Util;
import hudson.model.EnvironmentSpecific;
import hudson.model.ModelObject;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import hudson.slaves.NodeSpecific;
import hudson.tools.ToolDescriptor;
import hudson.tools.ToolInstallation;
import hudson.tools.ToolInstaller;
import hudson.tools.ToolProperty;
import jenkins.model.Jenkins;
import jenkins.security.MasterToSlaveCallable;

/**
 * RadarGunInstallation
 * 
 * @author vjuranek
 * 
 */
public class RadarGunInstallation extends ToolInstallation implements EnvironmentSpecific<RadarGunInstallation>,
        NodeSpecific<RadarGunInstallation>, ModelObject {

    @DataBoundConstructor
    public RadarGunInstallation(String name, String home, List<? extends ToolProperty<?>> properties) {
        super(name, home, properties);
    }

    @Override
    public String getDisplayName() {
        return getName();
    }
    
    @Override
    public RadarGunInstallation forEnvironment(EnvVars environment) {
        return new RadarGunInstallation(getName(), environment.expand(getHome()), getProperties().toList());
    }

    @Override
    public RadarGunInstallation forNode(Node node, TaskListener log) throws IOException, InterruptedException {
        return new RadarGunInstallation(getName(), translateFor(node, log), getProperties().toList());
    }

    /**
     * Gets the executable path of this RadarGun installation on the given target system.
     */
    public String getExecutable(final RgScriptConfig executable, VirtualChannel channel) throws IOException,
            InterruptedException {
        return channel.call(new MasterToSlaveCallable<String, IOException>() {

            public String call() throws IOException {
                File exec = getExeFile(executable);
                if (exec.exists()) {
                    return exec.getPath();
                }
                //abort build if we cannot locate RG executables
                throw new AbortException(
                        String.format(
                                "Cannot resolver path to executable %s, something wrong with your RG installation? Exiting ... ",
                                executable.getScriptName()));
            }

            private static final long serialVersionUID = 1L;

        });
    }

    private File getExeFile(final RgScriptConfig executable) {
        // TODO expand installation with t.translate(node, EnvVars.getRemote(checkChannel()))
        String rgHome = Util.replaceMacro(getHome(), EnvVars.masterEnvVars); // TODO - env var on slave??
        File binDir = new File(rgHome, "bin");
        return new File(binDir, executable.getScriptName());
    }

    @Extension
    public static class DescriptorImpl extends ToolDescriptor<RadarGunInstallation> {

        public DescriptorImpl() {
        }

        @Override
        public String getDisplayName() {
            return "RadarGun";
        }

        @Override
        public List<? extends ToolInstaller> getDefaultInstallers() {
            return Collections.singletonList(new RadarGunInstaller(null));
        }

        @Override
        public RadarGunInstallation[] getInstallations() {
            List<RadarGunInstallation> rdi = Jenkins.getInstance()
                    .getDescriptorByType(RadarGunBuilder.DescriptorImpl.class).getInstallations();
            RadarGunInstallation[] rdia = new RadarGunInstallation[rdi.size()];
            return rdi.toArray(rdia);
        }

        @Override
        public void setInstallations(RadarGunInstallation... installations) {
            Jenkins.getInstance().getDescriptorByType(RadarGunBuilder.DescriptorImpl.class)
                    .setInstallations(installations);
        }

    }

    private static final long serialVersionUID = 1L;

}
