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

            String replaceWith = null;
            String replaceFrom = null;
            
            // process each line
            System.err.println("reading diff file");
            while ((line = reader.readLine()) != null) {
                fileLine++;
                
                // begin replace
                if (line.startsWith("diff -r") || line.startsWith("diff -git")) {
                    // at the end of this line there's the file name we're searching
                    replaceFrom = line.split("\\s")[5];
                    String [] names = replaceFrom.split("/");
                    
                    String target = names[names.length - 1];
                    
                    System.err.println("--------- checking: " + target);
                    
                    // now search for this and replace it into the diff
                    Finder finder = new Finder(target, jdkLocation);
                    Files.walkFileTree(jdkLocation, finder);
                    if (finder.files.size() == 1) {
                        replaceWith = finder.files.get(0);
                    } else  {
                        // skip this one
                        System.err.println("please, manually update: " + line + " (" + fileLine + ")");
                        replaceWith = null;
                    }
                    
                } else if (line.startsWith("@@")) {
                    replaceWith = null;
                }
                
                if (replaceWith != null) {
                    System.err.println("delta: \t" + replaceWith);
                    System.err.println("source: \t" + line);
                    line = line.replaceAll(replaceFrom, replaceWith);
                    System.err.println("destination: \t" + line);
                }
                
                writer.write(line + "\n");                
            }
            
            System.err.println("done!");
        }
    }
}
