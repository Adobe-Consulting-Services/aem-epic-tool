package com.adobe.acs.epic.model;

/**
 * Abstraction of file contents (cached copy of zip entry information)
 */
public class FileContents {
    private final long fileSize;
    private final String path;
    private final String filename;
    private final long checksum;
    private final boolean directory;
    private final PackageContents pkg;
    
    public FileContents(ZipFullEntry source, PackageContents pkg) {
        fileSize = source.getSize();
        path = source.getName();
        filename = path.substring(path.lastIndexOf('/')+1);
        checksum = source.getCrc();
        directory = source.isDirectory();
        this.pkg = pkg;
    }
    
    public PackageContents getPackageContents() {
        return pkg;
    }

    /**
     * @return the fileSize
     */
    public long getFileSize() {
        return fileSize;
    }

    /**
     * @return the path
     */
    public String getPath() {
        return path;
    }

    /**
     * @return the filename
     */
    public String getFilename() {
        return filename;
    }

    /**
     * @return the checksum
     */
    public long getChecksum() {
        return checksum;
    }

    /**
     * @return the directory
     */
    public boolean isDirectory() {
        return directory;
    }
}
