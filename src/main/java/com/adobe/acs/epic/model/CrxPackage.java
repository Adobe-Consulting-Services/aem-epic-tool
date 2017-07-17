package com.adobe.acs.epic.model;

import com.adobe.acs.epic.DataUtils;
import com.adobe.acs.epic.PackageOps;
import com.adobe.acs.model.pkglist.PackageType;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import javafx.beans.property.DoubleProperty;

/**
 * Composite package object which collects all versions of the same package and orders 
 * them by their orthogonal version numbering.
 */
public class CrxPackage extends PackageType {
    PackageType mostRecent;
    Map<String, PackageType> allVersions = new TreeMap<>(this::compareVersions);
    PackageContents contents;
    
    public PackageContents getContents() throws IOException {
        if (contents == null) {
            contents = PackageOps.getPackageContents(getMostRecent(), null);
        }
        return contents;
    }

    public PackageContents getContentsWithProgress(DoubleProperty progress) throws IOException {
        if (contents == null) {
            contents = PackageOps.getPackageContents(getMostRecent(), progress);
        }
        return contents;
    }
    
    @Override
    public String getGroup() {
        return getMostRecent().getGroup();
    }

    @Override
    public String getName() {
        return getMostRecent().getName();
    }

    @Override
    public String getDownloadName() {
        return getMostRecent().getDownloadName(); 
    }

    @Override
    public int getSize() {
        return getMostRecent().getSize();
    }
    
    @Override
    public String getVersion() {
        return getMostRecent().getVersion();
    }

    @Override
    public String getLastUnpacked() {
        return getMostRecent().getLastUnpacked();
    }

    @Override
    public String getLastUnpackedBy() {
        return getMostRecent().getLastUnpackedBy();
    }

    @Override
    public String getCreated() {
        return getMostRecent().getCreated();
    }

   @Override
    public String getCreatedBy() {
        return getMostRecent().getCreatedBy();
    }
    
    @Override
    public String getLastModified() {
        return getMostRecent().getLastModified();
    }
    
    @Override
    public String getLastModifiedBy() {
        return getMostRecent().getLastModifiedBy();
    }    

    public PackageType getMostRecent() {
        if (mostRecent == null) {
            PackageType t = null;
            for (Iterator<PackageType> i = allVersions.values().iterator(); i.hasNext(); ) {
                t = i.next();
            }
            return t;
        } else {
            return mostRecent;
        }
    }
    
    public PackageType getInstalledVersion() {
        return mostRecent;
    }
    
    public void trackVersion(PackageType ver) {
        allVersions.put(ver.getVersion(), ver);
        if (ver.getLastUnpacked() != null && !ver.getLastUnpacked().isEmpty()) {
            if (mostRecent == null) {
                mostRecent = ver;
            } else {
                if (DataUtils.compareDates(ver.getLastUnpacked(), mostRecent.getLastUnpacked()) < 0) {
                    mostRecent = ver;
                }
            }
        }
    }

    public int compare(Object o1, Object o2) {
        if (o1 == null || !(o1 instanceof PackageType)) {
            return -1;
        }
        if (o2 == null || !(o2 instanceof PackageType)) {
            return 1;
        }
        
        return compareVersions((PackageType) o1, (PackageType) o2);
    }
    
    public int compareVersions(PackageType p1, PackageType p2) {
        int v1 = serializeVersionNumber(p1.getVersion());
        int v2 = serializeVersionNumber(p2.getVersion());
        return Integer.compare(v1, v2);
    }
    
    public int compareVersions(String p1, String p2) {
        if (p1.equals(p2)) {
            return 0;
        }
        int v1 = serializeVersionNumber(p1);
        int v2 = serializeVersionNumber(p2);
        return Integer.compare(v1, v2);
    }    
    
    public int serializeVersionNumber(String ver) {
        String[] parts = ver.split("[.-]");
        int num = 0;
        for (String s : parts) {
            try {
                int p = Integer.parseInt(s);
                num = num * 100 + p;
            } catch (NumberFormatException ex) {
                num -= 50;
            }
        }
        return num;
    }
    
    public Map<String, PackageType> getAllVersions() {
        return allVersions;
    }
}
