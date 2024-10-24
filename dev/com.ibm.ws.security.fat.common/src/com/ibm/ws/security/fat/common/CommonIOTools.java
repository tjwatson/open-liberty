/*******************************************************************************
 * Copyright (c) 2015, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.fat.common;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Vector;

//import org.jdom.Document;
//import org.jdom.input.SAXBuilder;

import com.ibm.websphere.simplicity.log.Log;

public class CommonIOTools {

    private final Class<?> c = CommonIOTools.class;

    public final static String NEW_LINE = System.getProperty("line.separator");
    public final static int ALL_INSTANCES = 0;

//    private final SAXBuilder builder = new SAXBuilder();
//
//    /**
//     * Creates a Document object from the specified path
//     *
//     * @param pathToDocument Path to the file to convert
//     * @return A Document file if successful, null otherwise.
//     */
//    public Document retrieveDocument(String pathToDocument) {
//        String method = "retrieveDocument";
//        try {
//            Log.info(c, method, "Attempting to retrieve and build document: " + pathToDocument);
//            File file = new File(pathToDocument);
//            Document doc = builder.build(file);
//            return doc;
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        Log.info(c, method, "Could not successfully return a document.");
//        return null;
//    }

    /**
     * Returns the full text of the given file as a string.
     *
     * @param filePath
     * @return
     */
    public String getFileText(String filePath) {
        String method = "getFileText";
        if (filePath == null || filePath.isEmpty()) {
            Log.info(c, method, "No file path provided");
            return null;
        }
        StringBuilder fileText = new StringBuilder();
        try {
            Log.info(c, method, "Getting text for file: " + filePath);

            File inp = new File(filePath);
            InputStreamReader inputStream = new InputStreamReader(new FileInputStream(inp));
            BufferedReader dataStream = new BufferedReader(inputStream);

            String currentLine = null;
            while ((currentLine = dataStream.readLine()) != null) {
                fileText.append(currentLine + NEW_LINE);
            }

        } catch (Exception e) {
            Log.error(c, method, e);
            return null;
        }

        Log.info(c, method, "Resulting string size: " + fileText.length());
        return fileText.toString();
    }

    /**
     * Replaces all occurrences of the strings included in {@code replaceValues} within the file specified by {@code filePath}.
     * If {@code outputFilePath} is null or empty, the resulting text will be output to the same path as {@code filePath}.
     *
     * @param filePath File in which the string will be replaced.
     * @param replaceValues Map of strings to be replaced.
     * @param outputFilePath File to which results will be written if a change was made. If null or empty, this is set to the
     *            value of {@code filePath}.
     * @return
     */
    public boolean replaceStringsInFile(String filePath, Map<String, String> replaceValues, String outputFilePath) {
        String method = "replaceStringsInFile";
        if (filePath == null || filePath.isEmpty()) {
            Log.info(c, method, "No file path provided");
            return false;
        }
        try {
            Log.info(c, method, "Source file name: " + filePath);

            File inp = new File(filePath);
            InputStreamReader inputStream = new InputStreamReader(new FileInputStream(inp));
            BufferedReader dataStream = new BufferedReader(inputStream);

            Vector<String> vec = new Vector<String>(200, 200);

            String currentLine = null;
            boolean changeMade = false;
            while ((currentLine = dataStream.readLine()) != null) {
                for (String key : replaceValues.keySet()) {
                    String origLine = currentLine;
                    String replaceVal = replaceValues.get(key);
                    currentLine = currentLine.replace(key, replaceVal);
                    if (!origLine.equals(currentLine)) {
                        changeMade = true;
                        // comment out for now to reduce size of output.txt
//                        Log.info(c, method, "origStr: [" + key + "], newStr: [" + replaceVal + "]");
//                        Log.info(c, method, "Before : " + origLine);
//                        Log.info(c, method, "After  : " + currentLine);
                    }
                }
                vec.addElement(currentLine);
            }

            dataStream.close();

            if (changeMade) {
                if (outputFilePath == null || outputFilePath.isEmpty()) {
                    Log.info(c, method, "Null or empty output file path provided; will write changes to original file");
                    outputFilePath = filePath;
                }
                Log.info(c, method, "Change detected; writing changes to file: " + outputFilePath);
                OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(outputFilePath));
                PrintWriter ps = new PrintWriter(osw, true);

                int totalLines = vec.size();
                for (int j = 0; j < totalLines; j++) {
                    currentLine = vec.elementAt(j);
                    ps.println(currentLine);
                }
                ps.close();
            } else {
                Log.info(c, method, "No changes detected - file was not written!");
            }
        } catch (Exception e) {
            Log.error(c, method, e);
            return false;
        }
        return true;
    }

}
