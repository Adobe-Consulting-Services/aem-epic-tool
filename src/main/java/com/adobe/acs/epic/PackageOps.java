package com.adobe.acs.epic;

import static com.adobe.acs.epic.DataUtils.compareDates;
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
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
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

    public static String getDownloadLink(PackageType pkg) {
        AuthHandler authHandler = ApplicationState.getInstance().getAuthHandler();
        return authHandler.getUrlBase() + "/etc/packages/" + pkg.getGroup() + "/" + pkg.getDownloadName();
    }

    private static File getPackageFile(PackageType pkg, DoubleProperty progress) {
        String filename = pkg.getGroup().replaceAll("[^A-Za-z]", "_") + "-" + pkg.getDownloadName() + "-" + pkg.getVersion() + "-" + pkg.getSize();
        if (!packageFiles.containsKey(filename)) {
            AuthHandler authHandler = ApplicationState.getInstance().getAuthHandler();
            try (CloseableHttpClient client = authHandler.getAuthenticatedClient()) {
                File targetFile = File.createTempFile(filename, ".zip");
                targetFile.deleteOnExit();
                String url = getDownloadLink(pkg);
                HttpGet request = new HttpGet(url);
                try (CloseableHttpResponse response = client.execute(request)) {
                    HttpEntity entity = response.getEntity();
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
                                    Platform.runLater(()->
                                            progress.set(newProgress)
                                    );
                                }
                            }
                            out.flush();
                            outputStream.flush();
                            outputStream.close();
                            inputStream.close();
                            response.close();
                            packageFiles.put(filename, targetFile);
                        }
                    }
                }
            } catch (IOException ex) {
                Logger.getLogger(AppController.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        if (progress != null) {
            progress.set(1.0);
        }
        return packageFiles.get(filename);
    }

    public static PackageContents getPackageContents(PackageType pkg, DoubleProperty progress) throws IOException {
        if (app.getPackageContents(pkg) == null) {
            File targetFile = getPackageFile(pkg, progress);
            Logger.getLogger(AppController.class.getName()).log(Level.INFO, "Package downloaded to {0}", targetFile.getPath());
            app.putPackageContents(pkg, new PackageContents(targetFile, pkg));
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