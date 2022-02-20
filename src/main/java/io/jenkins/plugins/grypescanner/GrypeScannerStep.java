package io.jenkins.plugins.grypescanner;

import java.io.IOException;
import java.net.URL;

import org.apache.commons.lang.SystemUtils;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.Launcher.LocalLauncher;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.ArtifactArchiver;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;

public class GrypeScannerStep extends Builder implements SimpleBuildStep
{

  // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
  @DataBoundConstructor
  public GrypeScannerStep()
  {
    System.out.println("GrypeScannerStep()");
  }

  @Override
  public Descriptor<Builder> getDescriptor()
  {
    System.out.println("getDescriptor");
    return super.getDescriptor();
  }

  @Override
  public void perform(Run<?, ?> run, FilePath workspace, EnvVars env, Launcher launcher, TaskListener listener)
      throws InterruptedException, IOException
  {
    String SCAN_TARGET = "dir:/tmp";
    FilePath grypeInstallDir = workspace.child("grypeInstallDir");
    grypeInstallDir.mkdirs();
    env.put("GRYPE_DB_CACHE_DIR", grypeInstallDir.toURI().getPath());

    FilePath script = grypeInstallDir.child("install.sh");
    FilePath grypeBinary = grypeInstallDir.child("grype");
    FilePath resultReport = grypeInstallDir.child("grypeReport.txt");
    FilePath templateFile = grypeInstallDir.child("default.tmpl");
    script.copyFrom(new URL("https://raw.githubusercontent.com/anchore/grype/main/install.sh"));

    // launcher.launch().cmds("","").;
    
    launcher.launch().cmds("hostname").envs(env)
        .stdout(listener).stderr(listener.getLogger()).pwd(workspace).join();

    
    

    listener.getLogger().println(
        "Installing grype on destination: sh " + script.toURI().getPath() + " -b " + grypeInstallDir.toURI().getPath());
    int ret = launcher.launch().cmds("sh", script.toURI().getPath(), "-b", grypeInstallDir.toURI().getPath()).envs(env)
        .stdout(listener).stderr(listener.getLogger()).pwd(workspace).join();
    listener.getLogger().println("return value: " + ret);

    listener.getLogger().println("Updating grype database: grype db update");
    ret = launcher.launch().cmds(grypeBinary.toURI().getPath(), "db", "update").envs(env).stdout(listener)
        .stderr(listener.getLogger()).pwd(grypeInstallDir).join();
    listener.getLogger().println("return value: " + ret);

    templateFile.copyFrom(GrypeScanner.class.getResource("/default.tmpl"));
    listener.getLogger().println("Running: " + grypeBinary.toURI().getPath() + " " + SCAN_TARGET + " -o " + "template " +
        "-t " + templateFile.toURI().getPath() + " --file " + resultReport.toURI().getPath());
    ret = launcher.launch()
        .cmds(grypeBinary.toURI().getPath(), SCAN_TARGET, "-o", "template", "-t", templateFile.toURI().getPath(),
            "--file", resultReport.toURI().getPath())
        .envs(env).stdout(listener).stderr(listener.getLogger()).pwd(grypeInstallDir).join();
    listener.getLogger().println("return value: " + ret);

    ArtifactArchiver artifactArchiver = new ArtifactArchiver("grypeInstallDir/grypeReport.txt");
    artifactArchiver.perform(run, workspace, env, launcher, listener);
    
//  TODO:   LocalLauncher localLauncher = new LocalLauncher(listener);

    // GrypeScanner gs = new GrypeScanner(listener.getLogger(), workspace.absolutize().);
    // GrypeScanner.downloadGrype();
    //
    // GrypeScanner.installGrype(listener.getLogger());
    // GrypeScanner.updateGrypeDb(listener.getLogger());
    // GrypeScanner.executeGrype(listener.getLogger());
    //
    // ArtifactArchiver artifactArchiver = new ArtifactArchiver("scanout*");

  }

  @Override
  public boolean requiresWorkspace()
  {
    return true;
  }

  @Override
  public BuildStepMonitor getRequiredMonitorService()
  {
    System.out.println("getRequiredMonitorService");
    return super.getRequiredMonitorService();
  }

  @Extension(ordinal = -2)
  public static final class DescriptorImpl extends BuildStepDescriptor<Builder>
  {
    private static final String DEFAULT_VNS_SERV = "localhost:5900";

    public DescriptorImpl()
    {
      super(GrypeScannerStep.class);
      load();
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject json) throws FormException
    {
      System.out.println("configureeeeeeeeeeeeeeeeeeeeeee");
      req.bindJSON(this, json);
      save();
      return true;
    }

    public FormValidation doCheckVncServ(@AncestorInPath AbstractProject<?, ?> project, @QueryParameter String value)
    {
      System.out.println("doCheckVncServ");
      return FormValidation
          .okWithMarkup("<strong><font color=\"blue\">Please, make sure that your vncserver is running on '"
              + Util.xmlEscape(value) + "'</font></strong>");
    }

    @Override
    public String getDisplayName()
    {
      System.out.println("getDisplayName");
      return "Enable scannerrr";
    }

    public String getDefaultVncServ()
    {
      return DEFAULT_VNS_SERV;
    }

    @Override
    public boolean isApplicable(Class<? extends AbstractProject> jobType)
    {
      return !SystemUtils.IS_OS_WINDOWS;
    }
  }

}
