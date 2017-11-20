// Copyright (c) 2015 Travelex Ltd

package com.travelex.dependencyversionreport.utils;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.travelex.dependencyversionreport.model.Depend;
import com.travelex.dependencyversionreport.model.Report;

import javafx.scene.control.TableCell;
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class Utils {

    private static final Logger LOGGER = LoggerFactory.getLogger(Utils.class);

    private static BASE64Decoder decoder = new BASE64Decoder();
    private static final String MAIN_DIR = "download";
    private static final String PATTERN_UPDATE = "([a-z0-9.-]+):([A-z0-9-_.]+)[\\. ]+ ([A-z0-9 .-]+) -> ([A-z0-9 .-]+)";
    private static final String PATTERN_TREE = "\\] \\+- ([a-z0-9-_.]+):([a-z0-9-_.]+):[a-z]+:([A-z0-9.-]+)";


    public static Map<String, RepositoryContents> scanProject(DataService dataService, ContentsService contentsService, Repository repo)
                    throws IOException {
        StopWatch watch = new StopWatch();
        watch.start();
        Map<String, RepositoryContents> map = new HashMap<>();
        RepositoryContents main = null;
        main = contentsService.getContents(repo, "/pom.xml").get(0);
        map.put(main.getSha(), main);
        LOGGER.info("scanProject1 took {} ms", watch.getTotalTimeMillis());

        contentsService.getContents(repo).parallelStream().filter(r -> ("dir".equals(r.getType()) && r.getName().startsWith(repo.getName())))
                        .forEach(r -> {
                            RepositoryContents content = null;
                            try {
                                content = contentsService.getContents(repo, r.getName() + "/pom.xml").get(0);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            map.put(content.getSha(), content);
                        });

        LOGGER.info("scanProject2 took {} ms", watch.getTotalTimeMillis());

        Path path = Files.createDirectories(Paths.get(MAIN_DIR + "/" + repo.getName()));

        map.entrySet().parallelStream().forEach(entry -> {
            Path pom = null;
            try {
                pom = createPom(path, entry.getValue().getPath());
                Blob blob = dataService.getBlob(repo, entry.getKey());
                if (pom != null) {
                    //                    byte[] ptext = blob.getContent().getBytes(UTF_8);
                    //                    System.out.println(new String(blob.getContent().getBytes(UTF_8)));
                    Files.write(pom, decoder.decodeBuffer(new String(blob.getContent().getBytes(UTF_8))));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        watch.stop();
        LOGGER.info("scanProject took {} ms", watch.getTotalTimeMillis());

        return map;
    }

    private static Path createPom(Path mainPath, String dir) throws IOException {
        StopWatch watch = new StopWatch();
        watch.start();
        String[] paths = dir.split("/");
        Path file = null;
        LOGGER.info("Path: {}", Arrays.toString(paths));
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
        LOGGER.info("createPom took {} ms", watch.getTotalTimeMillis());
        return file;
    }

    private static Optional<RepositoryContents> findPom(ContentsService contentsService, Repository repo, RepositoryContents content)
                    throws IOException {
        StopWatch watch = new StopWatch();
        watch.start();
        for (RepositoryContents rp : contentsService.getContents(repo, content.getPath())) {
            if (rp.getName().equals("pom.xml")) {
                return Optional.of(rp);
            }
        }
        watch.stop();
        LOGGER.info("findPom took {} ms", watch.getTotalTimeMillis());
        return Optional.empty();
    }

    public static void color(Depend rowDataItem, TableCell<Depend, String> tableCell) {
        String[] current = rowDataItem.getCurrentVersion().split("\\.");
        String[] next = rowDataItem.getNewVersion().split("\\.");
        int max = Math.max(current.length, next.length);
        int step = 0;
        for (step = 0; step < max; step++) {
            if (!current[step].equalsIgnoreCase(next[step])) {
                switch (step) {
                    case 0:
                        tableCell.setStyle("-fx-background-color: indianred");
                        break;
                    case 1:
                        tableCell.setStyle("-fx-background-color: orange");
                        break;
                    case 2:
                        tableCell.setStyle("-fx-background-color: yellow");
                        break;
                }
                return;
            }
        }
    }

    public static Map<String, Report> renderUpdate(List<String> output) {
        Map<String, Report> allDependencies = new HashMap<>();
        Pattern r = Pattern.compile(PATTERN_UPDATE);

        for (String line : output) {
            Matcher m = r.matcher(line);
            if (m.find()) {
                allDependencies.put(m.group(2), new Report(m.group(1), m.group(2), m.group(3), m.group(4)));
            } else {
                LOGGER.warn("RenderUpdate failed for {}", line);
            }
        }

        return allDependencies;
    }

    public static Map<String, Report> renderTree(List<String> output) {
        Pattern r = Pattern.compile(PATTERN_TREE);
        Map<String, Report> mainPom = new HashMap<>();

        for (String line : output) {
            Matcher m = r.matcher(line);
            if (m.find()) {
                mainPom.put(m.group(2), new Report(m.group(1), m.group(2), m.group(3)));
            } else {
                LOGGER.warn("RenderTree failed for {}", line);
            }
        }
        return mainPom;
    }

    public static List<String> filterUpdate(List<String> lines) {
        List<String> result = new ArrayList<>();

        for (String line : lines) {
            if (line.startsWith("[INFO]   ")) {
                if (line.startsWith("[INFO]                      ")) {
                    String s = result.get(result.size() - 1);
                    s += line.substring(15);
                    result.set(result.size() - 1, s);
                } else {
                    result.add(line);
                }
            }
        }
        return result;
    }

    public static List<String> filterTree(List<String> lines) {
        List<String> result = new ArrayList<>();

        for (String line : lines) {
            if (line.startsWith("[INFO] +-")) {
                result.add(line);
            }
        }
        return result;
    }
}
