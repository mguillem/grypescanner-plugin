package io.jenkins.plugins.grypescanner;

import java.io.IOException;
import java.net.URL;

import org.apache.commons.lang.SystemUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractProject;
import hudson.model.Result;
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
  private Boolean autoInstall;

  // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
  @DataBoundConstructor
  public GrypeScannerStep(String scanDest, String repName, Boolean autoInstall)
  {
    this.scanDest = scanDest;
    this.repName = repName;
    this.autoInstall = autoInstall;
  }

  @Override
  public void perform(Run<?, ?> run, FilePath workspace, EnvVars env, Launcher launcher, TaskListener listener)
      throws InterruptedException, IOException
  {

    if (!launcher.isUnix())
    {
      listener.fatalError(
          "The grypescanner requires a unix system, see https://github.com/anchore/grype for system requirements");
      run.setResult(Result.FAILURE);
      return;
    }
    if (Boolean.FALSE.equals(autoInstall))
    {
      int ret = launcher.launch().cmds("which", "grype").envs(env).stdout(listener).stderr(listener.getLogger()).join();
      listener.getLogger().println("return value: " + ret);
      if (ret != 0)
      {
        listener.fatalError(
            "Can't find 'grype' in PATH! Please, ensure that 'grype' is available in PATH or activate the 'Download and install grype automatically' setting!");
        run.setResult(Result.FAILURE);
        return;
      }
    }
    String scanDestresolved = Util.replaceMacro(scanDest, run.getEnvironment(listener));
    String repNameResolved = Util.replaceMacro(repName, run.getEnvironment(listener));

    FilePath grypeTmpDir = workspace.child("grypeTmpDir");
    FilePath templateFile = grypeTmpDir.child("default.tmpl");
    grypeTmpDir.mkdirs();
    templateFile.copyFrom(GrypeScannerStep.class.getResource("/default.tmpl"));
    listener.getLogger().println("Running grype scan: ");
    FilePath resultReport = grypeTmpDir.child(repNameResolved);
    if (Boolean.TRUE.equals(autoInstall))
    {
      env.put("GRYPE_DB_CACHE_DIR", grypeTmpDir.toURI().getPath());
      FilePath script = grypeTmpDir.child("install.sh");
      FilePath grypeBinary = grypeTmpDir.child("grype");
      script.copyFrom(new URL("https://raw.githubusercontent.com/anchore/grype/main/install.sh"));
      listener.getLogger().println("Installing grype on destination:");
      int ret = launcher.launch().cmds("sh", script.toURI().getPath(), "-b", grypeTmpDir.toURI().getPath()).envs(env)
          .stdout(listener).stderr(listener.getLogger()).pwd(workspace).join();
      listener.getLogger().println("return value: " + ret);
      ret = launcher.launch()
          .cmds(grypeBinary.toURI().getPath(), scanDestresolved, "-o", "template", "-t", templateFile.toURI().getPath(),
              "--file", resultReport.toURI().getPath())
          .envs(env).stdout(listener).stderr(listener.getLogger()).pwd(grypeTmpDir).join();
      listener.getLogger().println("return value: " + ret);
    }
    else
    {
      int ret = launcher.launch()
          .cmds("grype", scanDestresolved, "-o", "template", "-t", templateFile.toURI().getPath(), "--file",
              resultReport.toURI().getPath(), "--file", resultReport.toURI().getPath())
          .envs(env).stdout(listener).stderr(listener.getLogger()).pwd(grypeTmpDir).join();
      listener.getLogger().println("return value: " + ret);
    }

    ArtifactArchiver artifactArchiver = new ArtifactArchiver("grypeTmpDir/" + repNameResolved);
    artifactArchiver.perform(run, workspace, env, launcher, listener);
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

  public Boolean getAutoInstall()
  {
    return autoInstall;
  }

  public void setAutoInstall(Boolean autoInstall)
  {
    this.autoInstall = autoInstall;
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
      return true;
    }
  }
}
