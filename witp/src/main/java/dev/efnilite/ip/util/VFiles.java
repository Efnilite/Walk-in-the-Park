package dev.efnilite.ip.util;

import java.io.File;
import java.io.IOException;

/**
 * Utilities for managing files
 */
public class VFiles {

    /**
     * Creates a file at the specified path.
     * If the parent directories do not exist, they will be created alongside the file.
     *
     * @param   path
     *          The path to the file
     *
     * @return true if the file was created, false if the file already existed.
     */
    public static boolean create(String path) {
        File file = new File(path);

        if (!file.exists()) {
            File folder = new File(file.getParent());
            if (!folder.exists()) {
                folder.mkdirs();
            }

            try {
                return file.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return false;
    }
}