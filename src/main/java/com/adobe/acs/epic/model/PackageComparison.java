package com.adobe.acs.epic.model;

import com.adobe.acs.epic.PackageOps;
import com.adobe.acs.model.pkglist.PackageType;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

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
    
    public Collection<String> getFilesUniqueForPackage(PackageType pkg ) {
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
                .map(e->e.getKey())
                .collect(Collectors.toList());
    }

    public Collection<String> getUnchangedFiles() {
        return comparison.entrySet().stream()
                .filter((e) -> {
                    Map<Long, Set<FileContents>> versions = e.getValue();
                    Collection<Set<FileContents>> allFiles = versions.values();
                    if (allFiles.size() == 1) {
                        Set<FileContents> f = allFiles.iterator().next();
                        return (f.size() == comparedPackages.size());
                    } else {
                        return false;
                    }
                })
                .map(e->e.getKey())
                .collect(Collectors.toList());
    }    
}