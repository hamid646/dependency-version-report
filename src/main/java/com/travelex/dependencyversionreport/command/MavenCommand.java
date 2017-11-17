// Copyright (c) 2015 Travelex Ltd

package com.travelex.dependencyversionreport.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StopWatch;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class MavenCommand implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(MavenCommand.class);

    private final CountDownLatch latch;
    private final String command;
    private final File loc;
    private List<String> lines = new ArrayList<>();

    public MavenCommand(CountDownLatch latch, String command, File loc) {
        this.latch = latch;
        this.command = command;
        this.loc = loc;
    }

    @Override
    public void run() {
        StopWatch watch = new StopWatch();
        watch.start();
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
        log.info("Command: {}, Lines: {}", command, lines.size());
        watch.stop();
        log.info("executeCommand took {} ms", watch.getTotalTimeMillis());
        this.latch.countDown();
    }

    public List<String> getLines() {
        return lines;
    }
}
