/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.app.manager.springboot.internal;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 *
 */
public class ZipUtils {
    private static final TraceComponent tc = Tr.register(ZipUtils.class);

    public void recursiveDelete(File f) throws IOException {
        File[] subFiles = f.listFiles();
        if (subFiles != null) {
            for (File c : subFiles) {
                recursiveDelete(c);
            }
        }
        if (!f.delete())
            throw new IOException("Failed to delete file " + f.getName());
    }

    /**
     * Unzip utility. Assumes that the source zip and target directory are valid.
     *
     * @param sourceZip
     * @param targetDir
     * @return
     * @throws IOException
     * @throws ZipException
     */
    public void unzip(File sourceZip, File targetDir) throws ZipException, IOException {
        if (!sourceZip.exists() || !targetDir.exists())
            return;

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Unzipping file: " + sourceZip);
        }

        ZipFile zf = new ZipFile(sourceZip);
        try {
            for (Enumeration<? extends ZipEntry> e = zf.entries(); e.hasMoreElements();) {
                ZipEntry ze = e.nextElement();
                String fileName = ze.getName();
                File targetFile = new File(targetDir, fileName);
                if (ze.isDirectory()) {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "Creating directory: " + targetFile);
                    }
                    if (!targetFile.exists() && !targetFile.mkdirs()) {
                        throw new IOException("Could not create directory " + targetFile.getAbsolutePath());
                    }
                } else {
                    if (!targetFile.getParentFile().mkdirs() && !targetFile.getParentFile().exists()) {
                        //we can't find or create the required file. Log error message.
                        throw new IOException("Could not create directory " + targetFile.getParentFile().getAbsolutePath());
                    }

                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "Unzipping: " + ze.getName() + " (" + ze.getSize() + " bytes) into " + targetFile);
                    }

                    byte[] buffer = new byte[2048];
                    BufferedInputStream bis = null;
                    BufferedOutputStream bos = null;

                    try {
                        bis = new BufferedInputStream(zf.getInputStream(ze));
                        bos = new BufferedOutputStream(new FileOutputStream(targetFile), buffer.length);
                        int size;
                        while ((size = bis.read(buffer, 0, buffer.length)) != -1) {
                            bos.write(buffer, 0, size);
                        }
                    } finally {
                        if (bis != null)
                            bis.close();

                        if (bos != null) {
                            bos.flush();
                            bos.close();
                        }

                    }

                    long modified = ze.getTime();
                    if (modified < 0) {
                        modified = sourceZip.lastModified();
                    }
                    targetFile.setLastModified(modified);
                }
            }
        } finally {
            if (zf != null)
                zf.close();
        }
    }

}
