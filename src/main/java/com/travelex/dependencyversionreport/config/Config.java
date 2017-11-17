// Copyright (c) 2015 Travelex Ltd

package com.travelex.dependencyversionreport.config;

import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.ContentsService;
import org.eclipse.egit.github.core.service.DataService;
import org.eclipse.egit.github.core.service.RepositoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class Config {

    @Autowired
    private Environment env;

    @Bean
    public GitHubClient gitHubClient() {
        GitHubClient gitHubClient = new GitHubClient();
        gitHubClient.setOAuth2Token(env.getProperty("github.token"));
        return gitHubClient;
    }

    @Bean
    public RepositoryService repositoryService(GitHubClient gitHubClient) {
        return new RepositoryService(gitHubClient);
    }

    @Bean
    public DataService dataService(GitHubClient gitHubClient) {
        return new DataService(gitHubClient);
    }

    @Bean
    public ContentsService contentsService(GitHubClient gitHubClient) {
        return new ContentsService(gitHubClient);
    }
}
