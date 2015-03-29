package com.hubspot.shepard;

import com.hubspot.shepard.GitHub;
import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

public class Shepard {
    private static String ghURL = "github.com";
    private static String ghUser;
    private static String ghPass;
    private static String temp = System.getProperty("java.io.tmpdir");

    public static void main(String[] args) throws IOException, InterruptedException {
        Map<String, ArrayList<String>> pomMap;
        String[] organizations;
        GitHub github;
        File pomFile;
        FileWriter fileWriter;

        System.out.println("Shepard - Java dependency vulnerability scanner");
        System.out.println("-----------------------------------------------");

        deploy();
        getCreds();

        github = new GitHub(ghUser, ghPass, ghURL);
        organizations = github.getOrganizations(ghUser);

        /*pomMap = github.getPoms("reponame"); // get all POMs in a named repo
        pomFile = new File(temp + "pom.xml");


        Iterator iterator = pomMap.entrySet().iterator();
        while(iterator.hasNext()) { // project pom file loop
            fileWriter = new FileWriter(pomFile);
            Map.Entry pairs = (Map.Entry)iterator.next();
            String key = (String)pairs.getKey();  // pomURL
            String value = ((ArrayList<String>)pairs.getValue()).get(0); //  pom contents
            fileWriter.write(value);
            fileWriter.close();
            victimsScan(temp + "pom.xml"); // The magic happens here
            deleteFile(temp + "pom.xml");
        }
        */

            deleteFile(temp + "victims.jar");
            System.exit(0);
    }

    private static boolean getCreds() {
        Console console  = System.console();
        char[] password;

        System.out.println("Enter Hubteam Credentials");
        ghUser = console.readLine("Username: ");
        if ((password = console.readPassword("%s", "Password: ")) != null) {
            ghPass = new String(password);
        }

        return true;
    }

    private static void victimsScan(String pomPath) throws IOException, InterruptedException {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder();
            System.out.println(pomPath);
            processBuilder.command("java", "-jar", temp + "victims.jar", "--update", "--verbose", pomPath).inheritIO();
            Process process = processBuilder.start();
            process.waitFor();

            process.destroy();
        } catch (Exception e) {
            System.err.println(e);
        }
    }

    private static boolean deploy() throws IOException {
        // Load Victims JAR into temp folder
        try {
            ClassLoader classLoader = Shepard.class.getClassLoader();
            InputStream input = classLoader.getResourceAsStream("victims-client-1.0-SNAPSHOT-standalone.jar");
            OutputStream output = new FileOutputStream(temp + "victims.jar");

            int read = 0;
            byte[] bytes = new byte[1024];
            while ((read = input.read(bytes)) != -1) {
                output.write(bytes, 0, read);
            }

            output.flush();
            output.close();
            input.close();
        } catch(IOException e) {
            System.err.println(e);
            return false;
        }

        return true;
    }

    private static boolean deleteFile(String file) {
        Path path = Paths.get(file);
        try {
            Files.delete(path);
        } catch (NoSuchFileException x) {
            System.err.format("%s: no such" + " file or directory%n", path);
            return false;
        } catch (DirectoryNotEmptyException x) {
            System.err.format("%s not empty%n", path);
            return false;
        } catch (IOException x) {  // catch file permission issues
            System.err.println(x);
            return false;
        }
        return true;
    }
}
