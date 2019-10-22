/*
 * Copyright (C) 2019 Red Hat, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

/**
 * Adapts a patch so that it fits in the OpenJDK 8 directory layout from later versions.
 */
public class RePatch {

    private static class Finder extends SimpleFileVisitor<Path> {
        private final PathMatcher matcher;
        private Path jdkLocation;
        
        private List<String> files = new ArrayList<>();
        
        private Finder(String pattern, Path jdkLocation) {
            this.jdkLocation = jdkLocation;
            matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
            Path name = file.getFileName();
            if (name != null && matcher.matches(name)) {
                files.add(jdkLocation.relativize(file).toString());
            }
            
            return FileVisitResult.CONTINUE;
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("Need patch and target OpenJDK 8 root repository!!");
            return;
        }
        
        Path jdkLocation = Paths.get(args[1]);
        
        File source = new File(args[0]);
        File destination = new File(source.getAbsoluteFile().getParentFile().getCanonicalPath(), source.getName() + "-jdk8.patch");

        try (var reader = new BufferedReader(new FileReader(source));
             var writer = new BufferedWriter(new FileWriter(destination)))
        {
            String line = null;
            int fileLine = 0;

            // process each line
            String fileName = null;
            String replaceWith = null;

            System.err.println("reading diff file");
            while ((line = reader.readLine()) != null) {
                fileLine++;
                
                // begin replace
                boolean replace = false;
                int replaceIdx = 5;
                if (line.startsWith("diff -r")) {
                    replace = true;
                    replaceIdx = 5;

                } else if (line.startsWith("diff --git")) {
                    replace = true;
                    replaceIdx = 3;
                }

                if (replace) {
                    // at the end of this line there's the file name we're searching
                    fileName = line.split("\\s")[replaceIdx];
                    if (fileName.startsWith("b/")) {
                        fileName = fileName.replaceFirst("b/", "");
                    }

                    String [] names = fileName.split("/");
                    
                    String target = names[names.length - 1];

                    System.err.println("--------- checking: " + target);
                    
                    // now search for this and replace it into the diff
                    Finder finder = new Finder(target, jdkLocation);
                    Files.walkFileTree(jdkLocation, finder);
                    if (finder.files.size() == 1) {
                        replaceWith = finder.files.get(0);
                    } else  {
                        // let's see if we can find some hints of where this file can be before bailing
                        System.err.println("multiple candidate for: " + target + " found, trying to match most likely");
                        for (String file : finder.files) {
                            // we just check the parent directory to see if there's a match
                            String[] parents = fileName.split("/");
                            String[] matchParents = file.split("/");
                            if (parents.length < 2 || matchParents.length < 2) {
                                // this shouldn't really be possible, since there would be more than one file
                                // named the same way in the same location! Nonetheless, parents may be a
                                // single file in the root directory
                                continue;
                            }

                            System.err.println("candidate: " + file);
                            System.err.println("comparing parents: source " + parents[parents.length - 2] + " candidate: " + matchParents[matchParents.length - 2]);

                            if (parents[parents.length - 2].equals(matchParents[matchParents.length - 2])) {
                                // found!
                                replaceWith = file;
                                System.err.println("found candidate for: " + target + ": " + replaceWith);
                                break;
                            }
                        }

                        // skip this one
                        if (replaceWith == null) {
                            System.err.println("please, manually update: " + line + " (" + fileLine + ")");
                        }
                    }
                    
                } else if (line.startsWith("@@")) {
                    replaceWith = null;
                }
                
                if (replaceWith != null) {
                    System.err.println("delta: \t" + replaceWith);
                    System.err.println("source: \t" + line);
                    line = line.replaceAll(fileName, replaceWith);
                    System.err.println("destination: \t" + line);
                }
                
                writer.write(line + "\n");
            }
            
            System.err.println("done: " + destination);
        }
    }
}
