package io.jenkins.plugins.grypescanner;

import java.io.IOException;

import org.kohsuke.stapler.DataBoundConstructor;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Builder;
import jenkins.tasks.SimpleBuildStep;

public class GrypeScannerStep extends Builder implements SimpleBuildStep
{

  // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
  @DataBoundConstructor
  public GrypeScannerStep()
  {
    System.out.println("getRequiredMonitorServiceeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee");
  }

  @Override
  public Descriptor<Builder> getDescriptor()
  {
    System.out.println("getRequiredMonitorServiceeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee");
    return super.getDescriptor();
  }

  @Override
  public void perform(Run<?, ?> run, FilePath workspace, EnvVars env, Launcher launcher, TaskListener listener)
      throws InterruptedException, IOException
  {
    System.out.println("getRequiredMonitorServiceeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee");
    SimpleBuildStep.super.perform(run, workspace, env, launcher, listener);
  }

  @Override
  public void perform(Run<?, ?> run, EnvVars env, TaskListener listener) throws InterruptedException, IOException
  {
    System.out.println("getRequiredMonitorServiceeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee");
    SimpleBuildStep.super.perform(run, env, listener);
  }

  @Override
  public boolean requiresWorkspace()
  {
    System.out.println("getRequiredMonitorServiceeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee");
    return SimpleBuildStep.super.requiresWorkspace();
  }

  @Override
  public BuildStepMonitor getRequiredMonitorService()
  {
   System.out.println("getRequiredMonitorServiceeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee");
    return super.getRequiredMonitorService();
  }
}
