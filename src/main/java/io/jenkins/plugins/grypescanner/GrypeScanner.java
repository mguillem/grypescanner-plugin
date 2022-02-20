package io.jenkins.plugins.grypescanner;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
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
  private static final String GRYPE_INSTALL_DIR = "/tmp/grypeInstallDir";
  private static final String GRYPE_INSTALL_SCRIPT_LOCAL = GRYPE_INSTALL_DIR + File.separator
      + "grypeInstallSrcript.sh";
  private static final String GRYPE_DEFAULT_TPL = GRYPE_INSTALL_DIR + File.separator + "default.tmpl";

  private static final String GRYPE_BINARY = GRYPE_INSTALL_DIR + File.separator + "grype";
  
  private static final String GRYPE_OUTPUT_REPORT = GRYPE_INSTALL_DIR + File.separator + "report.txt";

  private static final String SCAN_TARGET = "dir:/tmp";

  public static void downloadGrype() throws IOException
  {
    copyResource(new URL(GRYPE_INSTALL_SCRIPT), GRYPE_INSTALL_SCRIPT_LOCAL);
  }

  private static void copyResource(URL url, String dest)
      throws IOException, FileNotFoundException
  {
    Files.createDirectories(Paths.get(GRYPE_INSTALL_DIR));
    try (ReadableByteChannel rbc = Channels.newChannel(url.openStream());
        FileOutputStream fos = new FileOutputStream(dest))
    {
      fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
    }
  }

  public static void installGrype() throws IOException, InterruptedException
  {
    exec(null, "sh", GRYPE_INSTALL_SCRIPT_LOCAL, "-b", GRYPE_INSTALL_DIR);
  }

  private static void exec(Map<String, String> env, String... params) throws IOException, InterruptedException
  {
    ProcessBuilder processBuilder = new ProcessBuilder(params);
    if (env != null && !env.isEmpty())
    {
      processBuilder.environment().putAll(env);
    }
    System.out.println("Executing: " + Arrays.toString(params));
    Process process = processBuilder.start();
    try (InputStreamReader inp = new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8);
        InputStreamReader errStream = new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8);
        BufferedReader reader = new BufferedReader(inp);
        BufferedReader errReader = new BufferedReader(errStream);)
    {
      String line;
      while ((line = reader.readLine()) != null)
      {
        System.out.println(line);
      }

      while ((line = errReader.readLine()) != null)
      {
        System.out.println(line);
      }
    }
    int exitCode = process.waitFor();
    System.out.println("\nExited with error code : " + exitCode);
  }

  public static void executeGrype() throws IOException, InterruptedException
  {
    copyResource(GrypeScanner.class.getResource("/default.tmpl"),GRYPE_DEFAULT_TPL);
    Map<String, String> env = new HashMap<>();
    env.put("GRYPE_DB_CACHE_DIR", GRYPE_INSTALL_DIR);
    exec(env, GRYPE_BINARY, SCAN_TARGET, "-o", "template", "-t", GRYPE_DEFAULT_TPL, "--file", GRYPE_OUTPUT_REPORT );
  }

  public static void updateGrypeDb() throws IOException, InterruptedException
  {
    Map<String, String> env = new HashMap<>();
    env.put("GRYPE_DB_CACHE_DIR", GRYPE_INSTALL_DIR);
    exec(env, GRYPE_BINARY, "db", "update");
  }

  public static void main(String[] args) throws IOException, InterruptedException
  {
    System.out.println("start");
    // downloadGrype();
    // installGrype();
    // updateGrypeDb();
    executeGrype();
    System.out.println("end");
  }
}
