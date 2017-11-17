// Copyright (c) 2015 Travelex Ltd

package com.travelex.dependencyversionreport.utils;

import org.eclipse.egit.github.core.Blob;
import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.RepositoryContents;
import org.eclipse.egit.github.core.service.ContentsService;
import org.eclipse.egit.github.core.service.DataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;
import sun.misc.BASE64Decoder;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Component
public class Utils {

    private static final Logger log = LoggerFactory.getLogger(Utils.class);

    private static BASE64Decoder decoder = new BASE64Decoder();
    private static String MAIN_DIR = "download";

    public static Map<String, RepositoryContents> scanProject(DataService dataService,
                    ContentsService contentsService, Repository repo) throws IOException {
        StopWatch watch = new StopWatch();
        watch.start();
        Map<String, RepositoryContents> map = new HashMap<>();
        RepositoryContents main = null;
        try {
            main = contentsService.getContents(repo, "/pom.xml").get(0);
        } catch (IOException e) {
            e.printStackTrace();
        }
        map.put(main.getSha(), main);
        log.info("scanProject1 took {} ms", watch.getTotalTimeMillis());

        contentsService.getContents(repo).parallelStream()
                        .filter(r -> ("dir".equals(r.getType()) && r.getName()
                                        .startsWith(repo.getName()))).forEach(r -> {
            RepositoryContents content = null;
            try {
                content = contentsService.getContents(repo, r.getName() + "/pom.xml").get(0);
            } catch (IOException e) {
                e.printStackTrace();
            }
            map.put(content.getSha(), content);
        });

        log.info("scanProject2 took {} ms", watch.getTotalTimeMillis());

        Path path = Files.createDirectories(Paths.get(MAIN_DIR + "/" + repo.getName()));
        for (Map.Entry<String, RepositoryContents> entry : map.entrySet()) {
            Path pom = createPom(path, entry.getValue().getPath());
            Blob blob = dataService.getBlob(repo, entry.getKey());
            if (pom != null) {
                Files.write(pom, decoder.decodeBuffer(blob.getContent()));
            }
        }

        watch.stop();
        log.info("scanProject took {} ms", watch.getTotalTimeMillis());

        return map;
    }

    private static Path createPom(Path mainPath, String dir) throws IOException {
        StopWatch watch = new StopWatch();
        watch.start();
        String[] paths = dir.split("/");
        Path file = null;
        System.out.println(Arrays.toString(paths));
        if (paths.length > 1) {
            Path path1 = Files.createDirectories(Paths.get(mainPath + "/" + paths[0]));
            Path path2 = Paths.get(path1 + "/" + paths[1]);
            if (!Files.exists(path2)) {
                file = Files.createFile(path2);
            }
        } else {
            Path path1 = Paths.get(mainPath + "/" + paths[0]);
            if (!Files.exists(path1)) {
                file = Files.createFile(path1);
            }
        }
        watch.stop();
        log.info("createPom took {} ms", watch.getTotalTimeMillis());
        return file;
    }

    private static Optional<RepositoryContents> findPom(ContentsService contentsService,
                    Repository repo, RepositoryContents content) throws IOException {
        StopWatch watch = new StopWatch();
        watch.start();
        for (RepositoryContents rp : contentsService.getContents(repo, content.getPath())) {
            if (rp.getName().equals("pom.xml")) {
                return Optional.of(rp);
            }
        }
        watch.stop();
        log.info("findPom took {} ms", watch.getTotalTimeMillis());
        return Optional.empty();
    }

    public static List<String> executeCommand(String command, File loc) {
        StopWatch watch = new StopWatch();
        watch.start();
        List<String> lines = new ArrayList<>();
        Process p;
        try {
            p = Runtime.getRuntime().exec(command, null, loc);

            boolean code = p.waitFor(1000, TimeUnit.MICROSECONDS);
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        watch.stop();
        log.info("executeCommand took {} ms", watch.getTotalTimeMillis());
        return lines;
    }
}
