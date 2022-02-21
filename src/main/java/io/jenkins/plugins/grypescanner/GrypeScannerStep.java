package io.jenkins.plugins.grypescanner;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.SystemUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.ArtifactArchiver;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;

public class GrypeScannerStep extends Builder implements SimpleBuildStep
{
  private static final String SCAN_TARGET_DEFAULT = "dir:/";
  private static final String REP_NAME_DEFAULT = "grypeReport_${JOB_NAME}_${BUILD_NUMBER}.txt";
  
  private String scanDest;
  private String repName;

  // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
  @DataBoundConstructor
  public GrypeScannerStep(String scanDest, String repName)
  {
    this.scanDest = scanDest;
    this.repName = repName;
  }

  @Override
  public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener)
      throws InterruptedException, IOException
  {
    String scanDestresolved = Util.replaceMacro(scanDest, run.getEnvironment(listener));
    String repNameResolved = Util.replaceMacro(repName, run.getEnvironment(listener));
    
    FilePath grypeInstallDir = workspace.child("grypeInstallDir");
    grypeInstallDir.mkdirs();
    Map<String, String> env = new HashMap<>();
    env.put("GRYPE_DB_CACHE_DIR", grypeInstallDir.toURI().getPath());

    FilePath script = grypeInstallDir.child("install.sh");
    FilePath grypeBinary = grypeInstallDir.child("grype");
    FilePath resultReport = grypeInstallDir.child(repNameResolved);
    FilePath templateFile = grypeInstallDir.child("default.tmpl");
    script.copyFrom(new URL("https://raw.githubusercontent.com/anchore/grype/main/install.sh"));

    launcher.launch().cmds("hostname").envs(env).stdout(listener).stderr(listener.getLogger()).pwd(workspace).join();

    listener.getLogger().println(
        "Installing grype on destination:");
    int ret = launcher.launch().cmds("sh", script.toURI().getPath(), "-b", grypeInstallDir.toURI().getPath()).envs(env)
        .stdout(listener).stderr(listener.getLogger()).pwd(workspace).join();
    listener.getLogger().println("return value: " + ret);

    listener.getLogger().println("Updating grype database:");
    ret = launcher.launch().cmds(grypeBinary.toURI().getPath(), "db", "update").envs(env).stdout(listener)
        .stderr(listener.getLogger()).pwd(grypeInstallDir).join();
    listener.getLogger().println("return value: " + ret);

    templateFile.copyFrom(GrypeScanner.class.getResource("/default.tmpl"));
    listener.getLogger().println("Running grype scan: ");
    ret = launcher.launch()
        .cmds(grypeBinary.toURI().getPath(), scanDestresolved, "-o", "template", "-t", templateFile.toURI().getPath(),
            "--file", resultReport.toURI().getPath())
        .envs(env).stdout(listener).stderr(listener.getLogger()).pwd(grypeInstallDir).join();
    listener.getLogger().println("return value: " + ret);

    ArtifactArchiver artifactArchiver = new ArtifactArchiver("grypeInstallDir/" + repNameResolved);
    artifactArchiver.perform(run, workspace, launcher, listener);
  }

  public String getScanDest()
  {
    return scanDest;
  }

  public void setScanDest(String scanDest)
  {
    this.scanDest = scanDest;
  }

  public String getRepName()
  {
    return repName;
  }

  public void setRepName(String repName)
  {
    this.repName = repName;
  }

  @Extension(ordinal = -2)
  public static final class DescriptorImpl extends BuildStepDescriptor<Builder>
  {

    public DescriptorImpl()
    {
      super(GrypeScannerStep.class);
      load();
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject json) throws FormException
    {
      req.bindJSON(this, json);
      save();
      return true;
    }

    
    public String getDefaultScanDest()
    {
      return SCAN_TARGET_DEFAULT;
    }

    @Override
    public String getDisplayName()
    {
      return "Vulnerability scan with grype";
    }

    public String getDefaultRepName()
    {
      return REP_NAME_DEFAULT;
    }

    @Override
    public boolean isApplicable(Class<? extends AbstractProject> jobType)
    {
      return !SystemUtils.IS_OS_WINDOWS;
    }
  }
}
