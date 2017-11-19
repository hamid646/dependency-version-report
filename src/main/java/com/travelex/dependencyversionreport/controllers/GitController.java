// Copyright (c) 2015 Travelex Ltd

package com.travelex.dependencyversionreport.controllers;

import com.travelex.dependencyversionreport.command.MavenCommand;
import com.travelex.dependencyversionreport.model.Report;
import com.travelex.dependencyversionreport.utils.Utils;

import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.service.ContentsService;
import org.eclipse.egit.github.core.service.DataService;
import org.eclipse.egit.github.core.service.RepositoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

@Component
public class GitController {

    private static final Logger LOGGER = LoggerFactory.getLogger(GitController.class);

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private DataService dataService;

    @Autowired
    private ContentsService contentsService;

    public GitController() {
    }

    public List<Repository> loadProjects() {
        List<Repository> repositories = Collections.emptyList();

        try {
            repositories = repositoryService.getOrgRepositories("Travelex");

        } catch (IOException e) {
            LOGGER.error("Failed for : ", e);
        }
        return repositories;
    }

    public List<Report> loadRepo(Repository repo) {
        StopWatch watch = new StopWatch();
        watch.start();
        List<Report> reportList = new ArrayList<>();

        try {
            Utils.scanProject(dataService, contentsService, repo);

            CountDownLatch latch = new CountDownLatch(2);

            MavenCommand update = new MavenCommand(latch, "mvn versions:display-dependency-updates", Paths.get("download/" + repo.getName()).toFile());
            MavenCommand tree = new MavenCommand(latch, "mvn dependency:tree", Paths.get("download/" + repo.getName()).toFile());

            new Thread(update).start();
            new Thread(tree).start();
            latch.await();
            Map<String, String> mainPom = Utils.renderTree(Utils.filterCommand2(tree.getLines()));
            Map<String, String> all = Utils.renderUpdate(Utils.filterCommand(update.getLines()));

            mainPom.forEach((k, v) -> {
                String s = all.get(k);
                if (s != null) {
                    reportList.add(new Report(k, s, v));
                    LOGGER.info("k: {}, v: {}, s: {}", k, v, s);
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
        // TODO do we need sort here?
        // Collections.sort(report, Comparator.naturalOrder());

        watch.stop();
        LOGGER.info("load took {} ms", watch.getTotalTimeMillis());
        return reportList;
    }
}
