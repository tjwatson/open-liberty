/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.boot.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.function.Function;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import org.atomos.framework.AtomosBundleInfo;
import org.atomos.framework.AtomosRuntime;
import org.osgi.framework.connect.ConnectContent;

import com.ibm.ws.kernel.boot.BootstrapConfig;
import com.ibm.ws.kernel.boot.LaunchException;
import com.ibm.ws.kernel.boot.cmdline.Utils;

/**
 * Contain the informations in bootstrap jar's manifest
 */
public class BootstrapManifest {

    /**  */
    private static final String KERNEL_BOOT_NAME = "com.ibm.ws.kernel.boot";
    static final String BUNDLE_VERSION = "Bundle-Version";
    static final String JAR_PROTOCOL = "jar";

    /** prefix for system-package files */
    static final String SYSTEM_PKG_PREFIX = "OSGI-OPT/websphere/system-packages_";

    /** suffix for system-package files */
    static final String SYSTEM_PKG_SUFFIX = ".properties";

    /**
     * Manifest header designating packages that should be exported into the
     * framework by this jar
     */
    static final String MANIFEST_EXPORT_PACKAGE = "Export-Package";

    private static BootstrapManifest instance = null;

    private final Attributes manifestAttributes;
    private final Object atomosRuntime;

    public static BootstrapManifest readBootstrapManifest(Object connectFactory) throws IOException {
        BootstrapManifest manifest = instance;
        if (manifest == null) {
            manifest = instance = new BootstrapManifest(connectFactory);
        }
        return manifest;
    }

    /** Clean up: allow garbage collection to clean up resources we don't need post-bootstrap */
    public static void dispose() {
        instance = null;
    }

    protected BootstrapManifest() throws IOException {
        this(null);
    }

    /**
     * In the case of liberty boot the manifest is discovered
     * by looking up the jar URL for this class.
     *
     * @param atomosRuntime enables the use of a atomos
     * @throws IOException if here is an error reading the manifest
     */
    protected BootstrapManifest(Object atomosRuntime) throws IOException {
        this.atomosRuntime = atomosRuntime;
        manifestAttributes = atomosRuntime != null ? getLibertyBootAttributes(atomosRuntime) : getAttributesFromBootstrapJar();
    }

    private static Attributes getAttributesFromBootstrapJar() throws IOException {
        JarFile jf = null;
        try {
            jf = new JarFile(KernelUtils.getBootstrapJar());
            Manifest mf = jf.getManifest();
            return mf.getMainAttributes();
        } catch (IOException e) {
            throw e;
        } finally {
            Utils.tryToClose(jf);
        }
    }

    private static Attributes getLibertyBootAttributes(Object at) {
        AtomosBundleInfo atomosBundle = ((AtomosRuntime) at).getBootLayer().findAtomosBundle(KERNEL_BOOT_NAME).orElseThrow(() -> new IllegalStateException("No kernel boot found."));
        ConnectContent content = atomosBundle.getConnectContent();
        return getManifestFromContent(content).getMainAttributes();
    }

    static Manifest getManifestFromContent(Object c) {
        ConnectContent content = (ConnectContent) c;
        try {
            content.open();
            Manifest mf = content.getEntry("META-INF/MANIFEST.MF").map((m) -> {
                try {
                    return m.getInputStream();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }).map((i) -> {
                try {
                    return new Manifest(i);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }).orElseThrow(IllegalStateException::new);
            return mf;
        } catch (IOException | UncheckedIOException e) {
            throw new IllegalStateException(e);
        } finally {
            try {
                content.close();
            } catch (IOException e) {
                // ignore
            }
        }

    }

    private static JarFile getLibertBootJarFile() {
        // here we assume we can lookup our own .class resource to find the JarFile
        return getJarFile(BootstrapManifest.class.getResource(BootstrapManifest.class.getSimpleName() + ".class"));
    }

    private static JarFile getJarFile(URL url) {
        if (JAR_PROTOCOL.equals(url.getProtocol())) {
            try {
                URLConnection conn = url.openConnection();
                if (conn instanceof JarURLConnection) {
                    return ((JarURLConnection) conn).getJarFile();
                }
            } catch (IOException e) {
                throw new IllegalStateException("No jar file found: " + url, e);
            }
        }
        throw new IllegalArgumentException("Not a jar URL: " + url);
    }

    /**
     * @return
     * @throws IOException
     */
    private JarFile getBootJar() throws IOException {
        // For liberty boot don't try to find the bootstrap jar.
        return atomosRuntime != null ? getLibertBootJarFile() : new JarFile(KernelUtils.getBootstrapJar());
    }

    /**
     * @return the bundleVersion
     */
    public String getBundleVersion() {
        return manifestAttributes.getValue(BUNDLE_VERSION);
    }

    private static final List<String> SYSTEM_PACKAGE_FILES //
                    = Arrays.asList(//
                                    "OSGI-OPT/websphere/system-packages_12.properties", //
                                    "OSGI-OPT/websphere/system-packages_11.properties", //
                                    "OSGI-OPT/websphere/system-packages_10.properties", //
                                    "OSGI-OPT/websphere/system-packages_9.properties", //
                                    "OSGI-OPT/websphere/system-packages_1.8.0.properties", //
                                    "OSGI-OPT/websphere/system-packages_1.7.0.properties", //
                                    "OSGI-OPT/websphere/system-packages_1.6.0.properties" //
                    );

    /**
     * @param bootProps
     * @throws IOException
     */
    public void prepSystemPackages(BootstrapConfig bootProps) {
        // Look for _extra_ system packages
        String packages = bootProps.get(BootstrapConstants.INITPROP_OSGI_EXTRA_PACKAGE);

        // Look for system packages set in bootstrap properties first
        String syspackages = bootProps.get(BootstrapConstants.INITPROP_OSGI_SYSTEM_PACKAGES);

        // Look for exported packages in manifest: append to bootstrap packages
        String mPackages = manifestAttributes.getValue(MANIFEST_EXPORT_PACKAGE);
        if (mPackages != null) {
            packages = (packages == null) ? mPackages : packages + "," + mPackages;

            // save new "extra" packages
            if (packages != null)
                bootProps.put(BootstrapConstants.INITPROP_OSGI_EXTRA_PACKAGE, packages);
        }

        // system packages are replaced, not appended
        // so we only go look for our list of system packages if it hasn't already been set in bootProps
        // (that's the difference, re: system packages vs. "Extra" packages.. )
        if (syspackages == null) {
            // Look for system packages property file in the jar
            String javaVersion = System.getProperty("java.version", "1.6.0");
            // the java version may have an update modifier in the version string so we need to remove it.
            int index = javaVersion.indexOf('_');
            index = (index == -1) ? javaVersion.indexOf('-') : index;
            javaVersion = (index == -1) ? javaVersion : javaVersion.substring(0, index);
            String pkgListFileName = SYSTEM_PKG_PREFIX + javaVersion + SYSTEM_PKG_SUFFIX;

            try {
                // work out the appropriate package file
                // otherwise try the default which will produce a nice error message

                //check if we have a package file for the version of Java we are using
                int indexOfPackageFileToUse = SYSTEM_PACKAGE_FILES.indexOf(pkgListFileName);
                // If not found, check for a more generic version string
                if (indexOfPackageFileToUse < 0 && javaVersion.indexOf('.') > 0) {
                    // If exact version match is not found, strip the minor/micro versions leaving just the major version
                    String genericPkgListFileName = SYSTEM_PKG_PREFIX + javaVersion.split("\\.")[0] + SYSTEM_PKG_SUFFIX;
                    indexOfPackageFileToUse = SYSTEM_PACKAGE_FILES.indexOf(genericPkgListFileName);
                }

                //if we don't, then we should use the highest available package list instead
                //unless there are no files at all, we don't worry about the case of not having
                //a matching file for a lower version because the minimum execution environment
                //means we will always be running on the minimum supported level.
                if (indexOfPackageFileToUse < 0)
                    indexOfPackageFileToUse = 0;
                //cut down the list to be from the current java version to the oldest version
                //we will read all the files and append the properties to save maintenance effort on the package lists
                List<String> systemPackageFileNames = SYSTEM_PACKAGE_FILES.subList(indexOfPackageFileToUse, SYSTEM_PACKAGE_FILES.size());

                syspackages = getMergedSystemProperties(systemPackageFileNames);

                // save new system packages
                if (syspackages != null)
                    bootProps.put(BootstrapConstants.INITPROP_OSGI_SYSTEM_PACKAGES, syspackages);

            } catch (IOException ioe) {
                throw new LaunchException("Unable to find or read specified properties file; "
                                          + pkgListFileName, MessageFormat.format(BootstrapConstants.messages.getString("error.unknownException"), ioe.toString()), ioe);
            }
        }

    }

    private String getMergedSystemProperties(List<String> pkgListFileNames) throws IOException {
        try {
            if (atomosRuntime == null) {
                try (JarFile jarFile = getBootJar()) {
                    return getMergedSystemProperties((p) -> {
                        ZipEntry entry = jarFile.getEntry(p);
                        if (entry != null) {
                            try {
                                return jarFile.getInputStream(entry);
                            } catch (IOException e) {
                                throw new UncheckedIOException(e);
                            }
                        }
                        return null;
                    }, pkgListFileNames);
                }
            } else {
                AtomosBundleInfo atomosBundle = ((AtomosRuntime) atomosRuntime).getBootLayer().findAtomosBundle(KERNEL_BOOT_NAME).orElseThrow(() -> new IllegalStateException("No kernel boot found."));
                ConnectContent content = atomosBundle.getConnectContent();
                content.open();
                try {
                    return getMergedSystemProperties((p) -> {
                        return content.getEntry(p).map((c) -> {
                            try {
                                return c.getInputStream();
                            } catch (IOException e) {
                                throw new UncheckedIOException(e);
                            }
                        }).orElse(null);
                    }, pkgListFileNames);
                } finally {
                    content.close();
                }
            }
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    private String getMergedSystemProperties(Function<String, InputStream> input, List<String> pkgListFileNames) throws IOException {
        String packages = null;
        boolean inheritSystemPackages = true;
        for (String pkgListFileName : pkgListFileNames) {
            if (!inheritSystemPackages)
                break;
            InputStream is = input.apply(pkgListFileName);
            if (is != null) {
                // read org.osgi.framework.system.packages property value from the file
                Properties properties = new Properties();
                try {
                    properties.load(is);
                    String loadedPackages = properties.getProperty(BootstrapConstants.INITPROP_OSGI_SYSTEM_PACKAGES);
                    if (loadedPackages != null) {
                        packages = (packages == null) ? loadedPackages : packages + "," + loadedPackages;
                    }
                    inheritSystemPackages &= Boolean.parseBoolean(properties.getProperty(BootstrapConstants.INITPROP_WAS_INHERIT_SYSTEM_PACKAGES, "true"));
                } finally {
                    Utils.tryToClose(is);
                }
            } else {
                throw new IOException("Unable to find specified properties file; " + pkgListFileName);
            }
        }
        return packages;
    }
}
