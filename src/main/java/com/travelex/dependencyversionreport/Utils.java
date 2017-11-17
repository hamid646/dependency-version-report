// Copyright (c) 2015 Travelex Ltd

package com.travelex.dependencyversionreport;

import org.eclipse.egit.github.core.Blob;
import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.RepositoryContents;
import org.eclipse.egit.github.core.service.ContentsService;
import org.eclipse.egit.github.core.service.DataService;
import org.springframework.stereotype.Component;
import sun.misc.BASE64Decoder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import javax.xml.bind.annotation.adapters.HexBinaryAdapter;

@Component
public class Utils {

    private static BASE64Decoder decoder = new BASE64Decoder();
    private static String MAIN_DIR = "download";

    public static Map<String, RepositoryContents> scanProject(DataService dataService, ContentsService contentsService, Repository repo)
                    throws IOException {
        Map<String, RepositoryContents> map = new HashMap<>();

        for (RepositoryContents contents : contentsService.getContents(repo)) {
            System.out.println(contents.getName() + " : " + contents.getType());
            if (contents.getType().equals("dir")) {
                Optional<RepositoryContents> ans = findPom(contentsService, repo, contents);
                if (ans.isPresent()) {
                    map.put(ans.get().getSha(), ans.get());
                }
            } else {
                if (contents.getName().equals("pom.xml")) {
                    map.put(contents.getSha(), contents);
                }
            }
        }
        Path path = Files.createDirectories(Paths.get(MAIN_DIR + "/" + repo.getName()));
        for (Map.Entry<String, RepositoryContents> entry : map.entrySet()) {
            Path pom = createPom(path, entry.getValue().getPath());
            Blob blob = dataService.getBlob(repo, entry.getKey());
            if (pom != null) {
                Files.write(pom, decoder.decodeBuffer(blob.getContent()));
            }

            //Files.createDirectories(Paths.get("beneficiary/"+entry.getValue().getPath()));
            //System.out.println(entry.getKey() + " " + entry.getValue().getName());
            //
            //            if (entry.getKey().equals("6a5b59231b27a0c1f93a6b298b6c25a1740180ac")) {
            //                Blob blob = dataService.getBlob(repo, entry.getKey());
            //                System.out.println(blob.getEncoding());
            //                String s = new String(decoder.decodeBuffer(blob.getContent()));
            //                System.out.println(s);
            //                System.out.println(blob.getContent());
            //            }
        }
        return map;
    }

    private static Path createPom(Path mainPath, String dir) throws IOException {


        String[] paths = dir.split("/");
        Path file = null;
        System.out.println(Arrays.toString(paths));
        if (paths.length > 1) {
            Path path1 = Files.createDirectories(Paths.get(mainPath + "/" + paths[0]));
            Path path2 = Paths.get(path1 + "/" + paths[1]);
            if (!Files.exists(path2))
                file = Files.createFile(path2);
        } else {
            Path path1 = Paths.get(mainPath + "/" + paths[0]);
            if (!Files.exists(path1))
                file = Files.createFile(path1);
        }
        return file;
    }

    private static Optional<RepositoryContents> findPom(ContentsService contentsService, Repository repo, RepositoryContents content)
                    throws IOException {
        for (RepositoryContents rp : contentsService.getContents(repo, content.getPath())) {
            if (rp.getName().equals("pom.xml")) {
                return Optional.of(rp);
            }
        }
        return Optional.empty();
    }

    public static List<String> executeCommand(String command, File loc) {
        List<String> lines = new ArrayList<>();
        Process p;
        try {
            p = Runtime.getRuntime().exec(command, null, loc);

            boolean code = p.waitFor(1000, TimeUnit.MICROSECONDS);
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = reader.readLine())!= null) {
                lines.add(line);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return lines;
    }
}
