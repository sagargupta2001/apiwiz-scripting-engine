package org.apiwiz.scriptingengine.utils;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class PyPIDependencyDownloader {

    private static final String DOWNLOAD_DIR = "deps/python";
    private static final String PYPI_URL = "https://pypi.org/pypi/%s/json";

    public static void installDependencies(Set<String> packages) throws IOException {
        new File(DOWNLOAD_DIR).mkdirs();

        for (String pkg : packages) {
            System.out.println("Downloading: " + pkg);
            String downloadUrl = getPackageDownloadUrl(pkg);
            File downloadedFile = downloadFile(downloadUrl, DOWNLOAD_DIR);

            if (downloadedFile.getName().endsWith(".zip")) {
                unzip(downloadedFile, new File(DOWNLOAD_DIR));
            } else if (downloadedFile.getName().endsWith(".tar.gz")) {
                untar(downloadedFile, new File(DOWNLOAD_DIR));
            } else if (downloadedFile.getName().endsWith(".zip") || downloadedFile.getName().endsWith(".whl")) {
                unzip(downloadedFile, new File(DOWNLOAD_DIR));
            } else {
                throw new IOException("Unsupported archive format: " + downloadedFile.getName());
            }
            System.out.println("Installed: " + downloadedFile);
        }
    }

    private static String getPackageDownloadUrl(String packageName) throws IOException {
        URL url = new URL(String.format(PYPI_URL, packageName));
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("Accept", "application/json");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            StringBuilder jsonBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonBuilder.append(line);
            }

            JSONObject root = new JSONObject(jsonBuilder.toString());
            JSONObject urlsByVersion = root.getJSONObject("releases");
            String latestVersion = root.getJSONObject("info").getString("version");

            JSONArray files = urlsByVersion.getJSONArray(latestVersion);

            for (int i = 0; i < files.length(); i++) {
                JSONObject file = files.getJSONObject(i);
                String pythonVersion = file.getString("python_version");
                String urlStr = file.getString("url");

                if ((pythonVersion.equals("py3") || pythonVersion.startsWith(">=3"))
                        && (urlStr.endsWith(".tar.gz") || urlStr.endsWith(".whl") || urlStr.endsWith(".zip"))) {
                    return urlStr;
                }
            }

            // fallback to any format if none match
            for (int i = 0; i < files.length(); i++) {
                String urlStr = files.getJSONObject(i).getString("url");
                if (urlStr.endsWith(".tar.gz") || urlStr.endsWith(".zip") || urlStr.endsWith(".whl")) {
                    return urlStr;
                }
            }

            throw new IOException("No compatible file found for package: " + packageName);
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

    private static void unzip(File zipFile, File targetDir) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                File newFile = new File(targetDir, entry.getName());
                if (entry.isDirectory()) {
                    newFile.mkdirs();
                } else {
                    new File(newFile.getParent()).mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(newFile)) {
                        zis.transferTo(fos);
                    }
                }
                zis.closeEntry();
            }
        }
    }

    private static void untar(File tarGzFile, File targetDir) throws IOException {
        try (
                FileInputStream fis = new FileInputStream(tarGzFile);
                GzipCompressorInputStream gcis = new GzipCompressorInputStream(fis);
                TarArchiveInputStream tais = new TarArchiveInputStream(gcis)
        ) {
            TarArchiveEntry entry;
            while ((entry = tais.getNextTarEntry()) != null) {
                File newFile = new File(targetDir, entry.getName());
                if (entry.isDirectory()) {
                    newFile.mkdirs();
                } else {
                    new File(newFile.getParent()).mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(newFile)) {
                        tais.transferTo(fos);
                    }
                }
            }
        }
    }

    public static String getDownloadPath() {
        return new File(DOWNLOAD_DIR).getAbsolutePath();
    }

    public static List<String> getImportableModulePaths() {
        List<String> paths = new ArrayList<>();
        collectPythonModuleDirs(new File(getDownloadPath()), paths);
        return paths;
    }

    private static void collectPythonModuleDirs(File dir, List<String> paths) {
        if (dir == null || !dir.isDirectory()) return;

        File[] files = dir.listFiles();
        if (files == null) return;

        boolean hasPythonCode = false;

        for (File file : files) {
            if (file.isDirectory()) {
                collectPythonModuleDirs(file, paths); // Recursive call
                hasPythonCode = true;
            } else if (file.getName().endsWith(".py")) {
                hasPythonCode = true;
            }
        }

        if (hasPythonCode) {
            paths.add(dir.getAbsolutePath().replace("\\", "/"));
        }
    }

}