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
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CommonBaseline {

    private static class Node {
        private String id;
        private Node next;
        private Map<String, Node> children = new HashMap<>();

        Node(String id) {
            this.id = id;
        }

        @Override
        public String toString() {
            return id;
        }
    }

    public static void main(String[] args) throws Exception {

        File source = new File(args[0]);

        List<String> allPaths = new ArrayList<>();
        try (var reader = new BufferedReader(new FileReader(source))) {
            Node root = new Node("root");

            String line = null;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("diff -r")) {
                    Node parent = root;

                    String[] elements = line.split(" ");
                    String path = elements[elements.length - 1];
                    allPaths.add(path);

                    String[] hierarchy = path.split("/");
                    for (String currentLevel : hierarchy) {

                        if (!parent.children.containsKey(currentLevel)) {
                            Node currentNode = new Node(currentLevel);
                            parent.children.put(currentLevel, currentNode);
                        }

                        parent = parent.children.get(currentLevel);
                    }
                }
            }

            System.err.println("all affected paths:");
            allPaths.forEach(path -> System.err.println(path));
            System.err.println("files found: " + allPaths.size());

            while (root.children.size() == 1) {
                Set<String> keys = root.children.keySet();
                root = root.children.get(keys.iterator().next());
            }
            System.err.println("common root: " + root);
        }
    }
}
