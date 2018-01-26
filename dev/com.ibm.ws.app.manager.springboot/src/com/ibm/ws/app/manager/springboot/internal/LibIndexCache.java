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

import static com.ibm.ws.app.manager.springboot.internal.SpringConstants.SPRING_SHARED_LIB_CACHE_DIR;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.wsspi.adaptable.module.AdaptableModuleFactory;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.factory.ArtifactContainerFactory;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.location.WsLocationConstants;
import com.ibm.wsspi.kernel.service.location.WsResource;

/**
 *
 */
@Component(service = { LibIndexCache.class })
public final class LibIndexCache {
    private static final String CACHE_DIR = ".cache.dirs";
    private static final String CACHE_ARTIFACT_DIR = "artifact.cache";
    private static final String CACHE_ADAPT_DIR = "adapt.cache";
    private static final String CACHE_OVERLAY_DIR = "overlay.cache";

    private WsResource libraryIndexRoot;
    private ArtifactContainerFactory containerFactory;
    private AdaptableModuleFactory adaptableFactory;

    @Reference
    protected void setLocationAdmin(WsLocationAdmin locAdmin) {
        WsResource indexRes = locAdmin.resolveResource(WsLocationConstants.SYMBOL_SHARED_RESC_DIR + SPRING_SHARED_LIB_CACHE_DIR);
        indexRes.create();
        libraryIndexRoot = indexRes;
    }

    @Reference(target = "(&(category=DIR)(category=JAR)(category=BUNDLE))")
    protected void setArtifactContainerFactory(ArtifactContainerFactory containerFactory) {
        this.containerFactory = containerFactory;
    }

    @Reference
    protected void setAdaptableModuleFactory(AdaptableModuleFactory adaptableFactory) {
        this.adaptableFactory = adaptableFactory;
    }

    public String storeLibrary(Entry content) throws IOException {
        String hash = hash(content);
        storeAtHash(content, hash);
        return hash;
    }

    public File getLibrary(String hash) {
        WsResource libraryRes = getStoreLocation(hash);
        if (libraryRes.exists()) {
            return libraryRes.asFile();
        }
        return null;
    }

    @FFDCIgnore(UnableToAdaptException.class)
    private void storeAtHash(Entry content, String hash) throws IOException {
        try (InputStream in = content.adapt(InputStream.class)) {
            getStoreLocation(hash).put(in);
        } catch (UnableToAdaptException e) {
            throw new IOException(e);
        }
    }

    /**
     * @param hash
     * @return
     */
    private WsResource getStoreLocation(String hash) {
        CharSequence prefix = hash.subSequence(0, 2);
        CharSequence postFix = hash.subSequence(2, hash.length());
        WsResource prefixDir = libraryIndexRoot.resolveRelative(prefix.toString() + '/');
        return prefixDir.resolveRelative(postFix.toString() + ".jar");
    }

    private static String hash(Entry content) throws IOException {
        try (InputStream in = content.adapt(InputStream.class)) {
            if (in == null) {
                return null;
            }
            MessageDigest digest = MessageDigest.getInstance("sha-256");
            byte[] buffer = new byte[1024];
            int read = -1;

            while ((read = in.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }

            byte[] digested = digest.digest();
            return convertToHexString(digested);
        } catch (UnableToAdaptException | NoSuchAlgorithmException e) {
            throw new IOException(e);
        }
    }

    private static String convertToHexString(byte[] digested) {
        StringBuilder stringBuffer = new StringBuilder();
        for (int i = 0; i < digested.length; i++) {
            stringBuffer.append(Integer.toString((digested[i] & 0xff) + 0x100, 16).substring(1));
        }
        return stringBuffer.toString();
    }

    /**
     * @param originalLibName
     * @param value
     * @return
     * @throws UnableToAdaptException
     */
    public Container getLibraryContainer(String hash, String originalLibName) throws UnableToAdaptException {
        File libFile = getLibrary(hash);
        if (libFile == null) {
            return null;
        }
        ArtifactContainer libArtifactContainer = containerFactory.getContainer(getCache(libFile, CACHE_ARTIFACT_DIR), libFile);
        return adaptableFactory.getContainer(getCache(libFile, CACHE_ADAPT_DIR), getCache(libFile, CACHE_OVERLAY_DIR), libArtifactContainer);
    }

    /**
     * @param hash
     * @return
     */
    private File getCache(File libFile, String cacheName) {
        WsResource cacheDir = libraryIndexRoot.resolveRelative(CACHE_DIR + '/' + libFile.getParentFile().getName() + '/' + libFile.getName() + '/' + cacheName + '/');
        cacheDir.create();
        return cacheDir.asFile();
    }

}
