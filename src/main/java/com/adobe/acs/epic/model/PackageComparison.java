package com.adobe.acs.epic.model;

import com.adobe.acs.epic.DataUtils;
import com.adobe.acs.epic.PackageOps;
import com.adobe.acs.model.pkglist.PackageType;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Compare one or more packages
 */
public class PackageComparison {

    Map<String, Map<Long, Set<FileContents>>> comparison = new TreeMap<>();
    Set<PackageContents> comparedPackages = new TreeSet<>((o1, o2)
            -> PackageOps.orderPackagesByUnpacked(o1.getSourcePackage(), o2.getSourcePackage())
    );

    public void observe(PackageContents contents) {
        contents.getFiles().values().forEach(this::observe);
        comparedPackages.add(contents);
    }

    public void observe(FileContents contents) {
        if (contents.isDirectory()) {
            return;
        }
        if (!comparison.containsKey(contents.getPath())) {
            // No good way to automatically sort keys because the keys are CRC hashes
            // So instead we'll rely on the natural order in which data is processed
            comparison.put(contents.getPath(), new LinkedHashMap<>());
        }
        if (!comparison.get(contents.getPath()).containsKey(contents.getChecksum())) {
            comparison.get(contents.getPath()).put(contents.getChecksum(), new TreeSet<>((o1, o2)
                    -> PackageOps.orderPackagesByUnpacked(
                            o1.getPackageContents().getSourcePackage(),
                            o2.getPackageContents().getSourcePackage()
                    )));
        }
        comparison.get(contents.getPath()).get(contents.getChecksum()).add(contents);
    }

    public Map<String, Map<Long, Set<FileContents>>> getFullComparison() {
        return comparison;
    }

    public Map<String, Map<Long, Set<FileContents>>> getOverlaps(boolean ignoreSimilarFiles) {
        return comparison.entrySet().stream()
                .filter((e) -> {
                    Map<Long, Set<FileContents>> versions = e.getValue();
                    return versions.size() > 1 || (!ignoreSimilarFiles
                            && versions.get(versions.keySet().iterator().next()).size() > 1);
                })
                .collect(Collectors.toMap(
                        e -> e.getKey(),
                        e -> e.getValue(),
                        (k, v) -> k,
                        LinkedHashMap::new
                ));
    }

    public Collection<String> getFilesUniqueForPackage(PackageType pkg) {
        return comparison.entrySet().stream()
                .filter((e) -> {
                    Map<Long, Set<FileContents>> versions = e.getValue();
                    Collection<Set<FileContents>> allFiles = versions.values();
                    if (allFiles.size() == 1) {
                        Set<FileContents> f = allFiles.iterator().next();
                        return (f.size() == 1 && f.iterator().next().getPackageContents().getSourcePackage().equals(pkg));
                    } else {
                        return false;
                    }
                })
                .map(e -> e.getKey())
                .collect(Collectors.toList());
    }

    /**
     * Determines which files are in more than one package and there are no
     * alternate versions for that file.
     *
     * @return List of common files
     */
    public List<String> getCommonFiles() {
        return comparison.entrySet().stream()
                .filter((e) -> {
                    Map<Long, Set<FileContents>> versions = e.getValue();
                    if (versions.size() > 1) {
                        return false;
                    }
                    Collection<Set<FileContents>> allFiles = versions.values();
                    Set<FileContents> f = allFiles.iterator().next();
                    return (f.size() > 1);
                })
                .map(e -> e.getKey())
                .collect(Collectors.toList());
    }

    public void exportMasterReport(File saveFile) throws IOException {
        XSSFWorkbook workbook = new XSSFWorkbook();
        List<PackageContents> contents = new ArrayList<>(comparedPackages);
        List<PackageType> packages = comparedPackages.stream()
                .map(PackageContents::getSourcePackage).collect(Collectors.toList());
        String[] headers = new String[]{
            "Key", "Group", "Package Name",
            "Package File", "Size", "Version",
            "Created", "Created By",
            "Last Modified", "Modified By",
            "Last Unpacked", "Unpacked By"
        };
        DataUtils.addSheet("Summary", workbook, packages, headers,
                pkg -> (char) ('A' + packages.indexOf(pkg)),
                PackageType::getGroup, PackageType::getName,
                PackageType::getDownloadName, PackageType::getSize,
                PackageOps::getInformativeVersion,
                PackageType::getCreated, PackageType::getCreatedBy,
                PackageType::getLastModified, PackageType::getLastModifiedBy,
                PackageType::getLastUnpacked, PackageType::getLastUnpackedBy
        );

        headers = new String[packages.size() + 1];
        for (int i = 0; i < packages.size(); i++) {
            headers[i + 1] = String.valueOf((char) ('A' + i));
        }

        headers[0] = "Path";
        Function<Map.Entry<String, Map<PackageContents, Integer>>, Object>[] cols
                = new Function[packages.size() + 1];
        cols[0] = e -> e.getKey();
        IntStream.range(0, contents.size()).forEach(i -> cols[i + 1] = e -> e.getValue().get(contents.get(i)));
        DataUtils.addSheet("Root paths", workbook, getAllBaseCounts().entrySet(), headers, cols);

        headers[0] = "Type";
        IntStream.range(0, contents.size()).forEach(i -> cols[i + 1] = e -> e.getValue().get(contents.get(i)));
        DataUtils.addSheet("File types", workbook, getAllTypeCounts().entrySet(), headers, cols);

        headers[0] = "File path";
        Function<String, Object>[] commonCols
                = new Function[packages.size() + 1];
        commonCols[0] = path -> path;
        IntStream.range(0, contents.size()).forEach(i -> commonCols[i + 1] = path -> {
            Set<FileContents> allFiles = comparison.get(path) != null ? comparison.get(path).values().iterator().next() : null;
            if (allFiles == null) {
                return null;
            } else {
                return allFiles.stream().map(FileContents::getPackageContents).filter(contents.get(i)::equals).count() > 0 ? "X" : null;
            }
        });
        DataUtils.addSheet("Common files", workbook, getCommonFiles(), headers, commonCols);

        Function<Map.Entry<String, Map<Long, Set<FileContents>>>, Object>[] overlapCols = new Function[packages.size() + 1];
        overlapCols[0] = e -> e.getKey();
        IntStream.range(0, contents.size()).forEach(i -> overlapCols[i + 1] = e -> {
            Map<Long, Set<FileContents>> allFiles = e.getValue();
            int versionNum = 1;
            for (Iterator<Map.Entry<Long, Set<FileContents>>> iter = allFiles.entrySet().iterator(); iter.hasNext(); versionNum++) {
                Set<FileContents> versionFiles = iter.next().getValue();
                if (versionFiles.stream().map(FileContents::getPackageContents)
                        .filter(contents.get(i)::equals).count() > 0) {
                    return versionNum;
                }
            }
            return null;
        });
        DataUtils.addSheet("Changed files", workbook, getOverlaps(true).entrySet(), headers, overlapCols);

        FileOutputStream out = new FileOutputStream(saveFile);
        workbook.write(out);
        out.flush();
        out.close();
    }

    private Map<String, Map<PackageContents, Integer>> getAllBaseCounts() {
        Map<String, Map<PackageContents, Integer>> summary = new TreeMap<>();
        comparedPackages.forEach(pkg -> {
            pkg.getBaseCounts().forEach((path, count) -> {
                if (!summary.containsKey(path)) {
                    summary.put(path, new HashMap<>());
                }
                summary.get(path).put(pkg, count);
            });
        });
        return summary;
    }

    private Map<String, Map<PackageContents, Integer>> getAllTypeCounts() {
        Map<String, Map<PackageContents, Integer>> summary = new TreeMap<>();
        comparedPackages.forEach(pkg -> {
            pkg.getFilesByType().forEach((type, files) -> {
                if (!summary.containsKey(type)) {
                    summary.put(type, new HashMap<>());
                }
                summary.get(type).put(pkg, files.size());
            });
        });
        return summary;
    }
}
