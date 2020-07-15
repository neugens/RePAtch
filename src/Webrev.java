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

import java.io.File;
import java.text.DecimalFormat;
import java.util.Arrays;

public class Webrev {
    public static void main(String[] args) throws Exception {

        if (args.length < 2) {
            System.err.println("Need arguments: [path-to-webrev.ksh] [bugId]");
            return;
        }

        String webrev = args[0];
        File destination = new File(args[1]).getCanonicalFile();

        if (!destination.exists()) {
            destination.mkdir();
        }

        System.err.println("working directory: " + destination);

        System.err.println("Creating webrev...");
        ProcessBuilder builder = new ProcessBuilder("ksh").command(webrev);
        Process process = builder.start();
        process.waitFor();

        File webrevBase =  new File("webrev");
        File webrevZip = new File("webrev.zip");
        if (!webrevZip.exists()) {
            System.err.println("No webrev created...");
            return;
        }

        System.err.println("Compacting webrev folder...");
        webrevZip.renameTo(new File(webrevBase, "webrev.zip"));

        System.err.println("Adding webrev to " + destination.getName());
        String[] content = destination.list();
        if (content != null && content.length > 0) {
            Arrays.sort(content);
            String path = content[content.length - 1].split("\\.")[1];

            Integer id = Integer.parseInt(path) + 1;
            DecimalFormat format = new DecimalFormat("#00");
            path = "webrev." + format.format(id);

            System.err.println("Creating: " + path);
            webrevBase.renameTo(new File(destination, path));

        } else {
            System.err.println("Creating: webrev.00");
            webrevBase.renameTo(new File(destination, "webrev.00"));
        }
    }
}
