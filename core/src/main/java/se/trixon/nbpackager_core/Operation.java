/*
 * Copyright 2020 Patrik Karlström.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package se.trixon.nbpackager_core;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.codec.digest.MessageDigestAlgorithms;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import se.trixon.almond.util.Log;
import se.trixon.almond.util.ProcessLogThread;
import static se.trixon.nbpackager_core.Options.*;

/**
 *
 * @author Patrik Karlström
 */
public class Operation {

    private String mContentDir = "NOT_AVAILABLE_IN_DRY_RUN";
    private Process mCurrentProcess;
    private File mDestDir;
    private final boolean mDryRun;
    private boolean mInterrupted;
    private final Log mLog;
    private final Options mOptions = Options.getInstance();
    private final Profile mProfile;
    private File mTempDir;

    public Operation(Profile profile, Log log) {
        mProfile = profile;
        mLog = log;
        mDryRun = mProfile.isDryRun();
    }

    public void start() throws IOException {
        long startTime = System.currentTimeMillis();

        if (!mDryRun) {
            if (!initTargetDirectory()) {
                mLog.err("\nOperation cancelled");
                return;
            }
        }

        if (mProfile.getPreScript() != null) {
            mLog.out("Run PRE execution script");
            executeScript(mProfile.getPreScript());
        }

        if (!mInterrupted) {
            unzip();
        }

        if (!mInterrupted & mProfile.isTargetAny()) {
            createPackage("any");
        }

        if (!mInterrupted & mProfile.isTargetLinux()) {
            createPackage("linux");
        }

        if (!mInterrupted & mProfile.isTargetMac()) {
            createPackage("mac");
        }

        if (!mInterrupted & mProfile.isTargetWindows()) {
            createPackage("windows");
        }

        if (!mInterrupted & mProfile.isTargetAppImage()) {
            createPackageAppImage();
        }

        if (!mInterrupted && mProfile.getPostScript() != null) {
            mLog.out("Run POST execution script");
            executeScript(mProfile.getPostScript());
        }

        FileUtils.deleteDirectory(mTempDir);

        if (mInterrupted) {
            mLog.err("\nOperation interrupted");
        } else {
            mLog.out("\nOperation completed");
        }
    }

    private void copyJre(File jreDir, File targetDir) throws IOException {
        String etc = String.format("etc/%s.conf", mContentDir);
        String jreName = jreDir.getName();
        File destDir = new File(targetDir, jreName);
        File etcFile = new File(targetDir, etc);
        mLog.out("set jdkhome in " + etcFile.getAbsolutePath());
        mLog.out("copy jre to: " + destDir.getAbsolutePath());
        if (!mDryRun) {
            FileUtils.write(etcFile, String.format("\n\n# Added by Packager\njdkhome=\"%s\"\n", jreName), "utf-8", true);
            FileUtils.copyDirectory(jreDir, destDir, true);
        }
    }

    private void createChecksum(File file, String algorithm) throws IOException {
        File digestFile = new File(file.getAbsolutePath() + String.format(".%s", StringUtils.remove(algorithm, "-").toLowerCase(Locale.getDefault())));
        mLog.out("create checksum: " + digestFile.getAbsolutePath());
        if (!mDryRun) {
            String digest = new DigestUtils(algorithm).digestAsHex(file);
            FileUtils.writeStringToFile(digestFile, String.format("%s  %s", digest, file.getName()), Charset.defaultCharset());
        }
    }

    private void createChecksums(File file) throws IOException {
        if (mProfile.isChecksumSha256()) {
            createChecksum(file, MessageDigestAlgorithms.SHA_256);
        }

        if (mProfile.isChecksumSha512()) {
            createChecksum(file, MessageDigestAlgorithms.SHA_512);
        }
    }

    private void createPackage(String target) throws IOException {
        mLog.out("\ncreate package: " + target);

        File targetDir = new File(mDestDir, target);

        mLog.out("copy zip contents to: " + targetDir.getAbsolutePath());
        if (!mDryRun) {
            targetDir.mkdirs();
            FileUtils.copyDirectory(mTempDir, targetDir, true);
        }

        targetDir = new File(targetDir, mContentDir);
        if (mProfile.getResources() != null) {
            mLog.out("copy resources to: " + targetDir.getAbsolutePath());
            if (!mDryRun) {
                FileUtils.copyDirectory(mProfile.getResources(), targetDir, true);
            }
        }

        boolean keepWindows = false;
        if (target.equalsIgnoreCase("linux") && mProfile.isTargetLinux()) {
            copyJre(mProfile.getJreLinux(), targetDir);
        } else if (target.equalsIgnoreCase("mac") && mProfile.isTargetMac()) {
            copyJre(mProfile.getJreMac(), targetDir);
        } else if (target.equalsIgnoreCase("windows") && mProfile.isTargetWindows()) {
            copyJre(mProfile.getJreWindows(), targetDir);
            keepWindows = true;
        }

        if (!target.equalsIgnoreCase("any")) {
            removeBin(new File(targetDir, "bin"), keepWindows);
        }

        File targetFile = new File(mDestDir, String.format("%s-%s.zip", mProfile.getBasename(), target));
        mLog.out("creating zip: " + targetFile.getAbsolutePath());
        if (!mDryRun) {
            FileUtils.deleteQuietly(targetFile);
            ZipParameters zipParameters = new ZipParameters();
            zipParameters.setIncludeRootFolder(true);
            new ZipFile(targetFile.getAbsolutePath()).addFolder(targetDir, zipParameters);
        }

        createChecksums(targetFile);
    }

    private void createPackageAppImage() throws IOException {
        mLog.out("\ncreate package: AppImage");
        mLog.out("copy template to: " + mDestDir.getAbsolutePath());
        String templateName = mProfile.getAppImageTemplate().getName();
        File targetDir = new File(mDestDir, templateName);
        File targetFile = new File(mDestDir, StringUtils.replace(templateName, "AppDir", "AppImage"));

        if (!mDryRun) {
            FileUtils.copyDirectory(mProfile.getAppImageTemplate(), targetDir, true);
        }

        File usrDir = new File(targetDir, "usr");
        mLog.out("copy zip contents to: " + usrDir.getAbsolutePath());
        if (!mDryRun) {
            for (File file : new File(mTempDir, mContentDir).listFiles()) {
                if (file.isFile()) {
                    FileUtils.copyFileToDirectory(file, usrDir, true);
                } else {
                    FileUtils.copyDirectoryToDirectory(file, usrDir);
                }
            }
        }

        removeBin(new File(usrDir, "bin"), false);
        copyJre(mProfile.getJreLinux(), usrDir);

        HashMap<String, String> environment = new HashMap<>();
        environment.put("ARCH", "x86_64");
        ArrayList<String> command = new ArrayList<>();
        command.add(mOptions.get(OPT_APP_IMAGE_TOOL, "NO_COMMAND_SPECIFIED"));
        for (String option : StringUtils.split(mOptions.get(OPT_APP_IMAGE_OPTIONS, ""))) {
            command.add(option);
        }

        command.add(targetDir.getAbsolutePath());
        command.add(targetFile.getAbsolutePath());
        execute(command, environment);

        createChecksums(targetFile);
    }

    private void execute(ArrayList<String> command, Map<String, String> environment) {
        mLog.out(getHeader() + String.join(" ", command));

        if (!mDryRun) {
            ProcessBuilder processBuilder = new ProcessBuilder(command).inheritIO();
            processBuilder.redirectOutput(ProcessBuilder.Redirect.PIPE);
            if (environment != null) {
                processBuilder.environment().putAll(environment);
            }

            try {
                mCurrentProcess = processBuilder.start();
                new ProcessLogThread(mCurrentProcess.getInputStream(), 0, mLog).start();
                new ProcessLogThread(mCurrentProcess.getErrorStream(), -1, mLog).start();
                Thread.sleep(1000);
                mCurrentProcess.waitFor();
            } catch (IOException ex) {
                mLog.timedErr(ex.getMessage());
            } catch (InterruptedException ex) {
                mCurrentProcess.destroy();
                mInterrupted = true;
            }
        }
    }

    private void executeScript(File script) {
        ArrayList<String> command = new ArrayList<>();
        command.add(script.getAbsolutePath());
        execute(command, null);
    }

    private String getHeader() {
        return mDryRun ? "execute: (dry-run) " : "execute: ";
    }

    private boolean initTargetDirectory() throws IOException {
        String name = FilenameUtils.getBaseName(mProfile.getSourceFile().getName());
        mDestDir = new File(mProfile.getDestDir(), name);
        boolean result = true;

        if (!mDestDir.exists()) {
            FileUtils.forceMkdir(mDestDir);
        } else {
            result = MainPanel.getDialogListener().onDialogRequest("Clear existing object?", String.format("Clear\n%s\nand continue?", mDestDir.getAbsolutePath()));
            if (result) {
                FileUtils.deleteQuietly(mDestDir);
                FileUtils.forceMkdir(mDestDir);
            }
        }

        return result;
    }

    private void removeBin(File file) throws IOException {
        mLog.out("remove: " + file.getAbsolutePath());
        if (!mDryRun) {
            FileUtils.forceDelete(file);
        }
    }

    private void removeBin(File binDir, boolean keepWindows) throws IOException {
        if (keepWindows) {
            removeBin(new File(binDir, mContentDir));
        } else {
            removeBin(new File(binDir, String.format("%s.exe", mContentDir)));
            removeBin(new File(binDir, String.format("%s64.exe", mContentDir)));
        }
    }

    private void unzip() throws IOException {
        mTempDir = Files.createTempDirectory("packager").toFile();
        mTempDir.deleteOnExit();
        mLog.out("create temp dir: " + mTempDir.getAbsolutePath());
        mLog.out("unzip: " + mProfile.getSourceFile());
        if (!mDryRun) {
            new ZipFile(mProfile.getSourceFile()).extractAll(mTempDir.getAbsolutePath());
            mContentDir = mTempDir.list()[0];
        }
    }
}
