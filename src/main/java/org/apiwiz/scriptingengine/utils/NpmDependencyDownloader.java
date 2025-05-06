package org.apiwiz.scriptingengine.utils;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.*;

/**
 * Downloads NPM packages entirely in‐JVM into deps/js/node_modules,
 * patches them for GraalVM ES‑module/CommonJS loading, and installs
 * their dependencies recursively.
 */
public class NpmDependencyDownloader {

    public static final String DOWNLOAD_DIR   = "deps/js";                     // parent of node_modules
    private static final String NODE_MODULES  = DOWNLOAD_DIR + "/node_modules";
    private static final String NPM_REGISTRY  = "https://registry.npmjs.org/%s";

    /**
     * Installs the given packages (and all their transitive dependencies)
     * into deps/js/node_modules.
     */
    public static void installDependencies(Set<String> packages) throws IOException {
        // ensure root node_modules folder exists
        Files.createDirectories(Paths.get(NODE_MODULES));

        // BFS over dependencies
        Deque<String> queue   = new ArrayDeque<>(packages);
        Set<String> processed = new HashSet<>();

        while (!queue.isEmpty()) {
            String pkg = queue.removeFirst();
            if (!processed.add(pkg)) {
                continue;   // already installed
            }

            System.out.println("Installing: " + pkg);
            File downloaded = null;
            File moduleDir  = new File(NODE_MODULES, pkg);

            try {
                // 1) fetch tarball URL & download
                String tarballUrl = fetchTarballUrl(pkg);
                downloaded = downloadFile(tarballUrl, NODE_MODULES);

                // 2) extract only package/ → moduleDir
                extractTgz(downloaded, moduleDir);

                // 3) patch package.json (remove exports/browser + rename main→.mjs)
                patchPackageJson(moduleDir);

                // 4) read its dependencies and enqueue
                JSONObject json = new JSONObject(Files.readString(
                        Paths.get(moduleDir.getAbsolutePath(), "package.json")));
                JSONObject deps = json.optJSONObject("dependencies");
                if (deps != null) {
                    for (String dep : deps.keySet()) {
                        if (!processed.contains(dep)) {
                            queue.add(dep);
                        }
                    }
                }

                // 5) verify the entry file exists
                verifyEntry(moduleDir);
                System.out.println("Installed: " + pkg);

            } finally {
                // clean up .tgz
                if (downloaded != null && downloaded.exists() && !downloaded.delete()) {
                    System.err.println("Warning: failed to delete " + downloaded.getAbsolutePath());
                }
            }
        }
    }

    /** Fetches the “latest” tarball URL for a package from the npm registry. */
    private static String fetchTarballUrl(String pkg) throws IOException {
        URL url = new URL(String.format(NPM_REGISTRY, pkg));
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("Accept", "application/json");
        try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                sb.append(line);
            }
            JSONObject root = new JSONObject(sb.toString());
            String latest = root.getJSONObject("dist-tags").getString("latest");
            return root.getJSONObject("versions")
                    .getJSONObject(latest)
                    .getJSONObject("dist")
                    .getString("tarball");
        }
    }

    /** Downloads a URL into the given target directory, returns the downloaded File. */
    private static File downloadFile(String urlString, String targetDir) throws IOException {
        URL url = new URL(urlString);
        String name = Paths.get(url.getPath()).getFileName().toString();
        File out = new File(targetDir, name);
        try (InputStream in = url.openStream();
             FileOutputStream fos = new FileOutputStream(out)) {
            in.transferTo(fos);
        }
        return out;
    }

    /**
     * Extracts only entries under “package/” from the .tgz,
     * stripping that prefix so contents land directly in targetDir.
     */
    private static void extractTgz(File tarGzFile, File targetDir) throws IOException {
        final String PREFIX = "package/";
        try (FileInputStream fis = new FileInputStream(tarGzFile);
             GzipCompressorInputStream gcis = new GzipCompressorInputStream(fis);
             TarArchiveInputStream tais = new TarArchiveInputStream(gcis)) {

            TarArchiveEntry entry;
            while ((entry = tais.getNextTarEntry()) != null) {
                String name = entry.getName();
                if (!name.startsWith(PREFIX)) continue;

                String rel = name.substring(PREFIX.length());
                File out = new File(targetDir, rel);
                if (entry.isDirectory()) {
                    if (!out.exists() && !out.mkdirs()) {
                        throw new IOException("Could not create directory: " + out);
                    }
                } else {
                    File parent = out.getParentFile();
                    if (parent != null && !parent.exists() && !parent.mkdirs()) {
                        throw new IOException("Could not create directory: " + parent);
                    }
                    try (FileOutputStream fos = new FileOutputStream(out)) {
                        tais.transferTo(fos);
                    }
                }
            }
        }
    }

    /**
     * Patches package.json in moduleDir by removing “exports” and “browser” fields,
     * then renaming its main .js entry → .mjs so GraalVM’s ES‑module loader picks it up.
     */
    private static void patchPackageJson(File moduleDir) throws IOException {
        File pkgFile = new File(moduleDir, "package.json");
        if (!pkgFile.exists()) throw new IOException("package.json missing in " + moduleDir);

        JSONObject json = new JSONObject(Files.readString(pkgFile.toPath()));
        json.remove("exports");
        json.remove("browser");

        // rename “main” .js → .mjs
        String main = json.optString("main", "index.js");
        if (main.endsWith(".js")) {
            String esmMain = main.replaceAll("\\.js$", ".mjs");
            File oldF = new File(moduleDir, main);
            File newF = new File(moduleDir, esmMain);
            if (oldF.exists() && !newF.exists()) {
                File parent = newF.getParentFile();
                if (parent != null && !parent.exists() && !parent.mkdirs()) {
                    throw new IOException("Could not create directory for " + newF);
                }
                if (!oldF.renameTo(newF)) {
                    throw new IOException("Failed to rename " + oldF + " → " + newF);
                }
                json.put("main", esmMain);
            }
        }

        Files.writeString(pkgFile.toPath(),
                json.toString(2),
                StandardOpenOption.TRUNCATE_EXISTING);
    }

    /** Ensures that the “main” entry file actually exists in the module folder. */
    private static void verifyEntry(File moduleDir) throws IOException {
        File pkgFile = new File(moduleDir, "package.json");
        JSONObject json = new JSONObject(Files.readString(pkgFile.toPath()));
        String entry = json.optString("main", "index.mjs");
        File entryFile = new File(moduleDir, entry);
        if (!entryFile.exists()) {
            throw new IOException("Entry file '" + entry + "' missing in " + moduleDir);
        }
    }
}
