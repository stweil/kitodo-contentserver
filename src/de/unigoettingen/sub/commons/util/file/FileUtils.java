/*
 * This file is part of the ContentServer project.
 * Visit the websites for more information. 
 * 		- http://gdz.sub.uni-goettingen.de 
 * 		- http://www.intranda.com 
 * 		- http://www.digiverso.com
 * 
 * Copyright 2009, Center for Retrospective Digitization, Göttingen (GDZ),
 * intranda software
 * 
 * This is the extended version updated by intranda
 * Copyright 2012, intranda GmbH
 * 
 * Licensed under the Apache License, Version 2.0 (the “License�?);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an “AS IS�? BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.unigoettingen.sub.commons.util.file;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * The Class FileUtils provides sume utilities for file handling
 */
public class FileUtils {

    /**
     * 
     * Gets the file extension from file name.
     * 
     * @param inFileName the file name as String
     * 
     * @return the file extension from file name as String
     */
    public static String getFileExtensionFromFileName(String inFileName) {
        int dotPos = inFileName.lastIndexOf('.') + 1;
        String extension = inFileName.substring(dotPos).trim();
        return extension;
    }

    /**
     * 
     * Gets the filename without extension from file name.
     * 
     * @param inFileName the file name as String
     * 
     * @return the filename without extension from file name as String
     */
    public static String getFileNameWithoutExtensionFromFileName(String inFileName) {
        int dotPos = inFileName.lastIndexOf('.') + 1;
        if (dotPos > 1) {
            String name = inFileName.substring(0, dotPos - 1).trim();
            return name;
        } else {
            return inFileName;
        }
    }

    /**
     * Gets the file extension from a File as String.
     * 
     * @param inFile the File
     * 
     * @return the file extension from inFile as String
     */
    public static String getFileExtensionFromFile(File inFile) {
        return getFileExtensionFromFileName(inFile.getAbsolutePath());
    }

    /**
     * calculate all files with given file extension at specified directory recursivly.
     * 
     * @param inDir the directory to run through
     * @param ext the file extension to use for counting, not case sensitive
     * 
     * @return number of files as Integer
     * 
     * @author Steffen Hankiewicz
     */
    public static Integer getNumberOfFiles(File inDir, final String ext) {
        int count = 0;
        if (inDir.isDirectory()) {
            // Count the images
            FilenameFilter filter = new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.toLowerCase(Locale.getDefault()).endsWith(ext.toLowerCase(Locale.getDefault()));
                }
            };
            count = inDir.list(filter).length;

            // Count the contents of sub directories
            String[] children = inDir.list();
            for (String element : children) {
                count += getNumberOfFiles(new File(inDir, element), ext);
            }
        }
        return count;
    }

    /**
     * calculate all files with given file extension at specified directory recursivly.
     * 
     * @param inDir the directory to run through
     * @param ext the file extension to use for counting, not case sensitive
     * 
     * @return number of files as Integer
     * 
     * @author Steffen Hankiewicz
     */
    public static Integer getNumberOfFiles(String inDir, final String ext) {
        return getNumberOfFiles(new File(inDir), ext);
    }

    /**
     * The Class FileListFilter can be used to filter Files usinf a regular expression
     */
    public static class FileListFilter implements FilenameFilter {

        /** The name. */
        private String name;

        /**
         * Instantiates a new file list filter.
         * 
         * @param name the name
         */
        public FileListFilter(String name) {
            this.name = name;
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.io.FilenameFilter#accept(java.io.File, java.lang.String)
         */
        @Override
        public boolean accept(File directory, String filename) {
            boolean fileOK = true;
            if (name != null) {
                fileOK &= filename.matches(name);
            }
            return fileOK;
        }
    }

    /**
     * Deletes all files and subdirectories under dir. Returns true if all deletions were successful. If a deletion fails, the method stops attempting
     * to delete and returns false.
     * 
     * @param dir the directory to delete
     * 
     * @return true, if directory deleted or it doesn't exists
     */
    public static boolean deleteDir(File dir) {
        if (!dir.exists()) {
            return true;
        }
        if (!deleteInDir(dir)) {
            return false;
        }
        // The directory is now empty so delete it
        return dir.delete();
    }

    /**
     * Deletes all files and subdirectories under dir. But not the dir itself
     * 
     * @param dir the directory, which contents should be deleted
     * 
     * @return true, if contents directory are deleted
     */
    public static boolean deleteInDir(File dir) {
        if (dir.exists() && dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                if (!deleteDir(new File(dir, children[i]))) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Make a File List for a given File (non recursive).
     * 
     * @author cmahnke
     * @param inputFile the input file
     * @param filter the filter
     * @return the list
     */
    public static List<File> makeFileList(File inputFile, String filter) {
        List<File> fileList;
        if (inputFile.isDirectory()) {

            File files[] = inputFile.listFiles(new FileExtensionsFilter(filter));
            fileList = Arrays.asList(files);
            Collections.sort(fileList);

        } else {
            fileList = new ArrayList<File>();
            fileList.add(inputFile);
        }
        return fileList;
    }

    /**
     * Gets the extension of a given file.
     * 
     * @param file the File
     * @return the extension
     * @author cmahnke
     */
    public static String getExtension(String file) {
        if (file.contains(".")) {
            return file.substring(file.lastIndexOf('.') + 1).toLowerCase();
        } else {
            return "";
        }
    }

}
