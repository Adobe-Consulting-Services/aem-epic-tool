package com.adobe.acs.epic.model;

import com.adobe.acs.epic.util.DataUtil;
import com.adobe.acs.model.pkglist.PackageType;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.poi.util.SAXHelper;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import com.adobe.acs.epic.util.JcrNodeContentHandler;
import java.io.InputStream;
import java.util.zip.ZipInputStream;

/**
 * Represents everything inside of an actual package file
 */
public class PackageContents {

    private final File file;
    private int folderCount;
    private int fileCount;
    private final Map<String, Integer> baseCounts = new TreeMap<>();
    private final Map<String, FileContents> files;
    private final Map<String, FileContents> subfiles;
    private final Map<String, Set<String>> filesByType = new TreeMap<>();
    private final PackageType pkg;

    private final ZipFile packageFile;

    public PackageContents(File targetFile, PackageType pkg) throws IOException {
        this.pkg = pkg;
        file = targetFile;
        packageFile = new ZipFile(targetFile);
        subfiles = new TreeMap<>();
        files = DataUtil.enumerationAsStream(packageFile.entries())
                .map(entry -> new ZipFullEntry(packageFile, entry))
                .peek(this::observeFileEntry)
                .collect(Collectors.toMap(
                        ZipFullEntry::getName,
                        e -> new FileContents(e, this),
                        (k, v) -> k,
                        TreeMap::new));
        files.putAll(subfiles);
        packageFile.close();
        if (pkg instanceof CrxPackage) {
            ((CrxPackage) pkg).setContents(this);
        }

    }

//    public void withFileContents(String path, Consumer<InputStream> consumer) throws IOException {
//        try (ZipFile zipFile = new ZipFile(file)) {
//            Optional<? extends ZipEntry> entry
//                    = DataUtil.enumerationAsStream(zipFile.entries())
//                            .filter(e -> e.getName().equals(path))
//                            .findFirst();
//            entry.ifPresent(e -> {
//                try {
//                    consumer.accept(zipFile.getInputStream(e));
//                } catch (IOException ex) {
//                    Logger.getLogger(PackageContents.class.getName()).log(Level.SEVERE, null, ex);
//                }
//            });
//        }
//    }
    private void observeFileEntry(ZipFullEntry entry) {
        if (entry.isDirectory()) {
            folderCount++;
        } else {
            fileCount++;
            String filePath = entry.getName();
            String[] parts = filePath.split(Pattern.quote("/"));
            String base;
            if (parts.length > 3) {
                base = parts[0] + "/" + parts[1] + "/" + parts[2];
            } else if (parts.length > 2) {
                base = parts[0] + "/" + parts[1];
            } else {
                base = parts[0];
            }
            if (!baseCounts.containsKey(base)) {
                getBaseCounts().put(base, 1);
            } else {
                getBaseCounts().put(base, getBaseCounts().get(base) + 1);
            }
            String fileName = parts[parts.length - 1];
            String type = null;
            if (!entry.isJarEntry() && !parts[0].equals("jcr_root")) {
                type = "VLT Metadata";
            } else if (!entry.isJarEntry() && fileName.equalsIgnoreCase("_rep_policy.xml")) {
                type = "rep:policy";
            } else if (!entry.isJarEntry() && fileName.equalsIgnoreCase(".content.xml")) {
                folderCount--;
                determineTypesInXMLFile(entry);
            } else if (fileName.endsWith(".jar")) {
                determineTypesInZipFile(entry);
            } else if (!entry.isJarEntry() && fileName.endsWith(".xml")) {
                determineTypesInXMLFile(entry);

//                if (filePath.contains("jcr_root/home/users/")) {
//                    type = "user";
//                } else if (filePath.contains("jcr_root/home/groups/")) {
//                    type = "group";
//                } else if (filePath.contains("jcr_root/_oak_index")) {
//                    type = "oak index definition";
//                } else {
//                    type = "node metadata";
//                }
            } else {
                type = fileName.contains(".") ? fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase() : "unknown";
                if (entry.isJarEntry()) {
                    type = "JAR Entry;" + type;
                }
            }
            trackFilesByType(type, filePath);
        }
    }

    private void trackFilesByType(String type, String filePath) {
        if (type != null) {
            if (!filesByType.containsKey(type)) {
                filesByType.put(type, new TreeSet<>());
            }
            filesByType.get(type).add(filePath);
        }
    }

    public PackageType getSourcePackage() {
        return pkg;
    }

    /**
     * @return the folderCount
     */
    public int getFolderCount() {
        return folderCount;
    }

    /**
     * @return the fileCount
     */
    public int getFileCount() {
        return fileCount;
    }

    /**
     * @return the baseCounts
     */
    public Map<String, Integer> getBaseCounts() {
        return baseCounts;
    }

    /**
     * @return the files
     */
    public Map<String, FileContents> getFiles() {
        return files;
    }

    public Map<String, Set<String>> getFilesByType() {
        return filesByType;
    }

    public File getFile() {
        return file;
    }

    JcrNodeContentHandler jcrContentHandler = new JcrNodeContentHandler();

    private void determineTypesInXMLFile(ZipFullEntry entry) {
        if (entry.getSize() > 0) {
            try {
                XMLReader reader = SAXHelper.newXMLReader();
                jcrContentHandler.setLocation(entry.getName()
                        .replaceAll(Pattern.quote("/.content.xml"), "")
                        .replaceAll("jcr_root", ""));
                reader.setContentHandler(jcrContentHandler);
                reader.parse(new InputSource(entry.getInputStream()));
                jcrContentHandler.getTypesFound().forEach((type, paths) -> {
                    paths.forEach(path -> trackFilesByType(type, entry.getName() + "/" + path));
                });
            } catch (SAXException | ParserConfigurationException | IOException ex) {
                Logger.getLogger(PackageContents.class.getName()).log(
                        Level.SEVERE,
                        "Error parsing entry " + entry.getName()
                        + " in archive " + packageFile.getName(), ex);
            }
        }
    }

    private void determineTypesInZipFile(ZipFullEntry entry) {
        InputStream in = entry.getInputStream();
        if (in == null) {
                Logger.getLogger(PackageContents.class.getName()).log(Level.SEVERE, "No file contents provided for {0}", entry.getName());
        } else {
            try (ZipInputStream bundle = new ZipInputStream(in)) {
                ZipEntry jarEntry;
                while ((jarEntry = bundle.getNextEntry()) != null) {
                    ZipFullEntry jarFullEntry = new ZipFullEntry(bundle, jarEntry, entry.getName().endsWith(".jar"));
                    observeFileEntry(jarFullEntry);
                    subfiles.put(entry.getName() + "!" + jarFullEntry.getName(), new FileContents(jarFullEntry, this));
                }
            } catch (IOException ex) {
                Logger.getLogger(PackageContents.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
