package com.adobe.acs.epic.model;

import com.sun.xml.internal.messaging.saaj.util.ByteInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Wrapper for zip entry which also provides contents
 */
public class ZipFullEntry {

    protected Supplier<Optional<InputStream>> contentsGetter;
    private ZipEntry entry;
    final private boolean insideJar;

    public ZipFullEntry(ZipFile file, ZipEntry e) {
        insideJar = false;
        entry = e;
        contentsGetter = () -> {
            try {
                return Optional.of(file.getInputStream(e));
            } catch (IOException ex) {
                Logger.getLogger(ZipFullEntry.class.getName()).log(Level.SEVERE, null, ex);
                return Optional.empty();
            }
        };
    }

    public ZipFullEntry(ZipInputStream stream, ZipEntry e) {
        insideJar = true;
        entry = e;
        contentsGetter = () -> {
            try {
                long size = e.getSize();
                // Note: This will fail to properly read 2gb+ files, but we have other problems if that's in this JAR file.
                byte[] buffer = new byte[(int) size];
                stream.read(buffer);
                return Optional.of(new ByteInputStream(new byte[(int) e.getSize()], buffer.length));
            } catch (IOException ex) {
                Logger.getLogger(ZipFullEntry.class.getName()).log(Level.SEVERE, null, ex);
                return Optional.empty();
            }
        };
    }

    public InputStream getInputStream() {
        return contentsGetter.get().orElse(null);
    }

    public boolean isJarEntry() {
        return insideJar;
    }
    
    public boolean isDirectory() {
        return entry.isDirectory();
    }

    public String getName() {
        return entry.getName();
    }

    public long getSize() {
        return entry.getSize();
    }

    public long getCrc() {
        return entry.getCrc();
    }
}
