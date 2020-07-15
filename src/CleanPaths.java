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

public class CleanPaths {
    public static void main(String[] args) throws Exception {
        File source = new File(args[0]);
        File destination = new File(source.getAbsoluteFile().getParentFile().getCanonicalPath(), source.getName() + "-clean.patch");

        try (var reader = new BufferedReader(new FileReader(source));
             var writer = new BufferedWriter(new FileWriter(destination)))
        {
            String line = null;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("diff -r")) {
                    String[] elements = line.split(" ");
                    String path = elements[elements.length - 1];
                    int idx = path.indexOf("/") + 1;
                    String replacedPath = path.substring(idx);
                    line = line.replace(path, replacedPath);

                } else if (line.startsWith("--- a/") || line.startsWith("+++ b/")) {
                    String[] elements = line.split(" ");
                    String path = elements[1];

                    int idx = path.indexOf("/", 2) + 1;
                    String replacedPath = path.substring(0, 2) + path.substring(idx);
                    line = line.replace(path, replacedPath);
                }

                writer.write(line + "\n");
            }
        }
    }
}