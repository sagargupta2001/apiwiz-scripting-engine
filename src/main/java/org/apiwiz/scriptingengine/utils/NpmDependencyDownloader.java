package org.apiwiz.scriptingengine.utils;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.*;
import java.util.Comparator;
import java.util.Set;

public class NpmDependencyDownloader {

    public static final String DOWNLOAD_DIR = "deps/js/node_modules";
    private static final String NPM_REGISTRY_URL = "https://registry.npmjs.org/%s";

    public static void installDependencies(Set<String> packages) throws IOException {
        Files.createDirectories(Paths.get(DOWNLOAD_DIR));

        for (String pkg : packages) {
            System.out.println("Installing: " + pkg);
            File downloadedTgz = null;
            try {
                String tarballUrl = getPackageDownloadUrl(pkg);
                downloadedTgz = downloadFile(tarballUrl, DOWNLOAD_DIR);

                File targetModuleDir = new File(DOWNLOAD_DIR, pkg);
                extractTgz(downloadedTgz, targetModuleDir);

                // Move contents from "package/" to root
                Path packageDir = new File(targetModuleDir, "package").toPath();
                if (Files.exists(packageDir)) {
                    Files.walk(packageDir)
                            .forEach(source -> {
                                try {
                                    Path destination = targetModuleDir.toPath().resolve(packageDir.relativize(source));
                                    if (Files.isDirectory(source)) {
                                        Files.createDirectories(destination);
                                    } else {
                                        Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
                                    }
                                } catch (IOException e) {
                                    throw new UncheckedIOException(e);
                                }
                            });
                    deleteDirectory(packageDir);
                }

                // Read package.json after moving
                File pkgJsonFile = new File(targetModuleDir, "package.json");
                if (!pkgJsonFile.exists()) throw new IOException("package.json missing in " + pkg);

                JSONObject pkgJson = new JSONObject(Files.readString(pkgJsonFile.toPath()));

                // Correctly determine entryPath handling "browser" as object
                Object browserObj = pkgJson.opt("browser");
                String entryPath;
                if (browserObj instanceof String) {
                    entryPath = (String) browserObj;
                } else {
                    entryPath = pkgJson.optString("main", "index.js");
                }

                File entryFile = new File(targetModuleDir, entryPath);
                if (!entryFile.exists()) {
                    throw new IOException("Entry file " + entryPath + " not found for " + pkg);
                }

                System.out.println("Installed: " + pkg);
            } finally {
                // Ensure .tgz is deleted even if errors occur
                if (downloadedTgz != null && !downloadedTgz.delete()) {
                    System.err.println("Warning: Failed to delete " + downloadedTgz.getAbsolutePath());
                }
            }
        }
    }

    private static String getPackageDownloadUrl(String packageName) throws IOException {
        URL url = new URL(String.format(NPM_REGISTRY_URL, packageName));
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("Accept", "application/json");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            StringBuilder jsonBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) jsonBuilder.append(line);

            JSONObject root = new JSONObject(jsonBuilder.toString());
            String latestVersion = root.getJSONObject("dist-tags").getString("latest");
            JSONObject versionData = root.getJSONObject("versions").getJSONObject(latestVersion);
            return versionData.getJSONObject("dist").getString("tarball");
        }
    }

    private static File downloadFile(String urlString, String targetDir) throws IOException {
        URL url = new URL(urlString);
        String fileName = Paths.get(url.getPath()).getFileName().toString();
        File outputFile = new File(targetDir, fileName);

        try (InputStream in = url.openStream(); FileOutputStream out = new FileOutputStream(outputFile)) {
            in.transferTo(out);
        }
        return outputFile;
    }

    private static void extractTgz(File tarGzFile, File targetDir) throws IOException {
        try (
                FileInputStream fis = new FileInputStream(tarGzFile);
                GzipCompressorInputStream gcis = new GzipCompressorInputStream(fis);
                TarArchiveInputStream tais = new TarArchiveInputStream(gcis)
        ) {
            TarArchiveEntry entry;
            while ((entry = tais.getNextTarEntry()) != null) {
                String entryName = entry.getName();
                if (!entryName.startsWith("package/")) continue;

                // Preserve the "package/" prefix in the path
                File newFile = new File(targetDir, entryName);

                if (entry.isDirectory()) {
                    newFile.mkdirs();
                } else {
                    Files.createDirectories(newFile.getParentFile().toPath());
                    try (FileOutputStream fos = new FileOutputStream(newFile)) {
                        tais.transferTo(fos);
                    }
                }
            }
        }
    }

    private static void deleteDirectory(Path dir) throws IOException {
        if (!Files.exists(dir)) return;
        Files.walk(dir)
                .sorted(Comparator.reverseOrder()) // delete children before parents
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        System.err.println("Failed to delete " + path + ": " + e.getMessage());
                    }
                });
    }
}
