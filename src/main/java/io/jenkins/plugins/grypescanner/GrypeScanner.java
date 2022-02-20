package io.jenkins.plugins.grypescanner;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class GrypeScanner
{
  private static final String GRYPE_INSTALL_SCRIPT = "https://raw.githubusercontent.com/anchore/grype/main/install.sh";
  private String grypeInstallDir = "/tmp/grypeInstallDir";
  private String grypeInstallScript = grypeInstallDir + File.separator + "grypeInstallSrcript.sh";
  private String grypeTpl = grypeInstallDir + File.separator + "default.tmpl";
  private String grypeBinary = grypeInstallDir + File.separator + "grype";
  private String grypeOutputRep = grypeInstallDir + File.separator + "report.txt";
  private String SCAN_TARGET = "dir:/tmp";
  private PrintStream logger = System.out;

  public GrypeScanner(PrintStream logger, String grypeInstallDir)
  {
    this.logger = logger;
    this.grypeInstallDir = grypeInstallDir;
    grypeInstallScript = grypeInstallDir + File.separator + "grypeInstallSrcript.sh";
    grypeTpl = grypeInstallDir + File.separator + "default.tmpl";
    grypeBinary = grypeInstallDir + File.separator + "grype";
    grypeOutputRep = grypeInstallDir + File.separator + "report.txt";
  }

  public void downloadGrype() throws IOException
  {
    copyResource(new URL(GRYPE_INSTALL_SCRIPT), grypeInstallScript);
  }

  private void copyResource(URL url, String dest) throws IOException
  {
    Files.createDirectories(Paths.get(grypeInstallDir));
    try (ReadableByteChannel rbc = Channels.newChannel(url.openStream());
        FileOutputStream fos = new FileOutputStream(dest))
    {
      fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
    }
  }

  public void installGrype() throws IOException, InterruptedException
  {
    exec(null, "sh", grypeInstallScript, "-b", grypeInstallDir);
  }

  private void exec(Map<String, String> env, String... params) throws IOException, InterruptedException
  {
    ProcessBuilder processBuilder = new ProcessBuilder(params);
    if (env != null && !env.isEmpty())
    {
      processBuilder.environment().putAll(env);
    }
    logger.println("Executing: " + Arrays.toString(params));
    Process process = processBuilder.start();
    try (InputStreamReader inp = new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8);
        InputStreamReader errStream = new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8);
        BufferedReader reader = new BufferedReader(inp);
        BufferedReader errReader = new BufferedReader(errStream);)
    {
      String line;
      while ((line = reader.readLine()) != null)
      {
        logger.println(line);
      }

      while ((line = errReader.readLine()) != null)
      {
        logger.println(line);
      }
    }
    int exitCode = process.waitFor();
    logger.println("Exited with error code : " + exitCode);
  }

  public void executeGrype() throws IOException, InterruptedException
  {
    copyResource(GrypeScanner.class.getResource("/default.tmpl"), grypeTpl);
    Map<String, String> env = new HashMap<>();
    env.put("GRYPE_DB_CACHE_DIR", grypeInstallDir);
    exec(env, grypeBinary, SCAN_TARGET, "-o", "template", "-t", grypeTpl, "--file", grypeOutputRep);
  }

  public void updateGrypeDb() throws IOException, InterruptedException
  {
    Map<String, String> env = new HashMap<>();
    env.put("GRYPE_DB_CACHE_DIR", grypeInstallDir);
    exec(env, grypeBinary, "db", "update");
  }

  public static void main(String[] args) throws IOException, InterruptedException
  {
    System.out.println("start");
    GrypeScanner gs = new GrypeScanner(System.out, "/tmp/grypeInstallDir");
    gs.downloadGrype();
    gs.installGrype();
    gs.updateGrypeDb();
    gs.executeGrype();
    
    // downloadGrype();
    // installGrype();
    // updateGrypeDb();
    // executeGrype(System.out);
    System.out.println("end");
  }
}
