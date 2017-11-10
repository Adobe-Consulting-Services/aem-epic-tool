package com.adobe.acs.epic.util;

import com.adobe.acs.epic.ApplicationState;
import static com.adobe.acs.epic.util.DataUtil.compareDates;
import com.adobe.acs.epic.controller.AppController;
import com.adobe.acs.epic.controller.AuthHandler;
import com.adobe.acs.epic.model.CrxPackage;
import com.adobe.acs.epic.model.PackageContents;
import com.adobe.acs.model.pkglist.PackageType;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;

/**
 * Package-specific utility functions
 */
public class PackageOps {

    static final private ApplicationState app = ApplicationState.getInstance();

    private PackageOps() {
        // Utility class has no constructor
    }

    public static boolean isInstalled(PackageType p) {
        return p.getLastUnpacked() != null && !p.getLastUnpacked().isEmpty();
    }

    public static boolean isProduct(PackageType p) {
        String group = p.getGroup().trim().toLowerCase();
        return group.startsWith("adobe/cq")
                || group.startsWith("adobe/aem6")
                || group.startsWith("com/adobe/cq")
                || group.startsWith("day/cq")
                || group.startsWith("cq/hotfix")
                || group.contains("featurepack")
                || group.startsWith("adobe/granite")
                || group.startsWith("com.adobe.cq")
                || group.startsWith("com.adobe.granite");
    }

    public static int orderPackagesByUnpacked(PackageType p1, PackageType p2) {
        int val = compareDates(p1.getLastUnpacked(), p2.getLastUnpacked());
        if (val != 0) {
            return val;
        } else {
            val = compareDates(p1.getCreated(), p2.getCreated());
            if (val != 0) {
                return val;
            } else {
                return getCompareName(p1).compareTo(getCompareName(p2));
            }
        }
    }

    public static String getCompareName(PackageType pkg) {
        return pkg.getGroup() + "~~~" + pkg.getName() + "~~~" + pkg.getVersion();
    }

    public static boolean hasPackageContents(PackageType pkg) {
        return app.getPackageContents(pkg) != null;
    }

    private static final Map<String, File> packageFiles = new HashMap<>();

    public static String getDownloadLink(PackageType pkg, AuthHandler authHandler) {
        boolean hasGroup = pkg.getGroup() != null && !pkg.getGroup().isEmpty();
        return authHandler.getUrlBase() + "/etc/packages/"
                + (hasGroup ? urlPathEscape(pkg.getGroup()) + "/" : "")
                + urlPathEscape(pkg.getDownloadName());
    }

    private static String urlPathEscape(String str) {
        try {
            return URLEncoder.encode(str, "UTF-8")
                    .replaceAll(Pattern.quote("+"), "%20")
                    .replaceAll(Pattern.quote("%21"), "!")
                    .replaceAll(Pattern.quote("%27"), "'")
                    .replaceAll(Pattern.quote("%28"), "(")
                    .replaceAll(Pattern.quote("%29"), ")")
                    .replaceAll(Pattern.quote("%2F"), "/")
                    .replaceAll(Pattern.quote("%7E"), "~");
        } catch (UnsupportedEncodingException ex) {
            return str;
        }
    }

    private static File getPackageFile(PackageType pkg, AuthHandler authHandler, DoubleProperty progress) {
        String filename = getPackageFileName(pkg);
        if (!packageFiles.containsKey(filename)) {
            try (CloseableHttpClient client = authHandler.getAuthenticatedClient()) {
                File targetFile = File.createTempFile(filename, ".zip");
                targetFile.deleteOnExit();
                String url = getDownloadLink(pkg, authHandler);
                retry(3, () -> {
                    HttpGet request = new HttpGet(url);
                    try (CloseableHttpResponse response = client.execute(request)) {
                        int statusCode = response.getStatusLine().getStatusCode();
                        if (statusCode < 200 || statusCode > 299) {
                            Logger.getLogger(AppController.class.getName()).log(Level.SEVERE,
                                    "Error retrieving {0}; Status code: {1}; Reason: {2}",
                                    new Object[]{url, statusCode, response.getStatusLine().getReasonPhrase()});
                            return null;
                        }
                        HttpEntity entity = response.getEntity();
                        if (Math.abs(pkg.getSize() - entity.getContentLength()) > 1024) {
                            Logger.getLogger(AppController.class.getName()).log(Level.SEVERE,
                                    "Error retrieving {0}; Expected size is not the same as download size",
                                    new Object[]{url});
                            return null;
                        }
                        if (entity != null) {
                            InputStream inputStream = entity.getContent();
                            try (OutputStream outputStream = new FileOutputStream(targetFile)) {
                                long fileSize = entity.getContentLength();
                                long downloaded = 0;
                                BufferedInputStream in = new BufferedInputStream(inputStream);
                                BufferedOutputStream out = new BufferedOutputStream(outputStream);
                                byte[] buffer = new byte[1024];
                                int size;
                                int updateCounter = 0;
                                while ((size = in.read(buffer)) > 0) {
                                    downloaded += size;
                                    out.write(buffer, 0, size);
                                    if (progress != null && (++updateCounter % 16) == 0) {
                                        final double newProgress = (double) downloaded / (double) fileSize;
                                        Platform.runLater(()
                                                -> progress.set(newProgress)
                                        );
                                    }
                                }
                                out.flush();
                                outputStream.flush();
                                outputStream.close();
                                inputStream.close();
                                response.close();
                                packageFiles.put(filename, targetFile);
                            } catch (IOException ex) {
                                return ex;
                            }
                        }
                        return null;
                    } catch (IOException ex) {
                        Logger.getLogger(PackageOps.class.getName()).log(Level.SEVERE, "Error downloading package " + url, ex);
                        return ex;
                    }
                });
            } catch (Exception ex) {
                Logger.getLogger(AppController.class.getName()).log(Level.SEVERE, "Unable to downlaod package after 3 attempts", ex);
            }
            if (progress != null) {
                Platform.runLater(() -> progress.set(1.0));
            }
        }
        return packageFiles.get(filename);
    }

    public static void retry(int tries, Supplier<Exception> action) throws Exception {
        Exception ex = null;
        for (int i = 0; i < tries; i++) {
            ex = action.get();
            if (ex == null) {
                return;
            }
        }
        if (ex != null) {
            throw ex;
        }
    }

    public static void relocatePackageFile(PackageType pkg, File destination) throws IOException {
        String filename = getPackageFileName(pkg);
        File packageFile = packageFiles.get(filename);
        File target = new File(destination, filename + ".zip");
        if (packageFile != null) {
            String tmpDir = System.getProperty("java.io.tmpdir");
            if (packageFile.getAbsolutePath().contains(tmpDir)) {
                Files.move(packageFile.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE);
                packageFiles.put(filename, target);
            } else {
                Files.copy(packageFile.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE);
            }
        }
    }

    private static String getPackageFileName(PackageType pkg) {
        return pkg.getGroup().replaceAll("[^A-Za-z]", "_") + "-"
                + pkg.getDownloadName() + "-"
                + pkg.getVersion() + "-"
                + pkg.getSize();
    }

    public static void importLocalPackageFile(PackageType pkg, File parentFolder) throws IOException {
        String filename = getPackageFileName(pkg);
        File target = new File(parentFolder, filename + ".zip");
        if (target.exists()) {
            PackageContents contents = new PackageContents(target, pkg);
            app.putPackageContents(pkg, contents);
            packageFiles.put(filename, target);
        }
    }

    public static PackageContents getPackageContents(PackageType pkg, AuthHandler handler, DoubleProperty progress) throws IOException {
        int retries = 3;
        if (app.getPackageContents(pkg) == null && retries > 0) {
            File targetFile = getPackageFile(pkg, handler, progress);
            Logger.getLogger(AppController.class.getName()).log(Level.INFO, "Package downloaded to {0}", targetFile.getPath());
            try {
                PackageContents contents = new PackageContents(targetFile, pkg);
                app.putPackageContents(pkg, contents);
                return contents;
            } catch (IOException ex) {
                Logger.getLogger(PackageOps.class.getName()).log(Level.SEVERE, null, ex);
                if (retries-- <= 0) {
                    throw ex;
                } else {
                    app.clearPackageContents(pkg);
                }
            }
        }
        return app.getPackageContents(pkg);
    }

    public static String getInformativeVersion(PackageType pkg) {
        String ver = pkg.getVersion();
        if (pkg instanceof CrxPackage) {
            int others = ((CrxPackage) pkg).getAllVersions().size() - 1;
            if (others > 1) {
                ver += " +" + others + " others";
            } else if (others == 1) {
                ver += " +1 other";
            }
        }
        return ver;
    }
}
