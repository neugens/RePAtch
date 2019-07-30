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

public class StripRoot {

    private static class FindMercurial extends SimpleFileVisitor<Path> {
        private final PathMatcher matcher;
        private Path jdk;
        
        private List<String> repositories = new ArrayList<>();
        
        private FindMercurial(Path jdkLocation) {
            this.jdk = jdkLocation.toAbsolutePath();
            matcher = FileSystems.getDefault().getPathMatcher("glob:hgrc");
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
            if (file != null && matcher.matches(file.getFileName())) {
                repositories.add(jdk.relativize(file.getParent().getParent()).toString());
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
        File destination = new File(source.getAbsoluteFile().getParentFile().getCanonicalPath(), source.getName() + "-jdk8-stripped.patch");

        var mercurial = new FindMercurial(jdkLocation);
        Files.walkFileTree(mercurial.jdk, mercurial);
        mercurial.repositories.forEach(repo -> System.err.println(repo));
        
        try (var reader = new BufferedReader(new FileReader(source));
             var writer = new BufferedWriter(new FileWriter(destination)))
        {
            String line = null;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("diff -r")) {
                    
                }
            }
        }
    }

}
