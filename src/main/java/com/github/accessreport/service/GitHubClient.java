package com.github.accessreport.service;

import com.github.accessreport.dto.MemberDto;
import com.github.accessreport.dto.RepoDto;
import com.github.accessreport.dto.TeamDto;
import com.github.accessreport.exception.GitHubApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GitHubClient {

    private final RestTemplate restTemplate;

    @Value("${github.api.url}")
    private String baseUrl;

    public List<RepoDto> getAllOrgRepos(String orgName) {
        List<RepoDto> allRepos = new ArrayList<>();
        int page = 1;

        log.info("Fetching all repos for org: {}", orgName);

        while (true) {
            String url = baseUrl + "/orgs/" + orgName + "/repos?per_page=100&page=" + page + "&type=all";
            try {
                ResponseEntity<RepoDto[]> response = restTemplate.getForEntity(url, RepoDto[].class);
                if (response.getBody() == null || response.getBody().length == 0) break;
                allRepos.addAll(Arrays.asList(response.getBody()));
                if (response.getBody().length < 100) break;
                page++;
            } catch (HttpClientErrorException ex) {
                if (ex.getStatusCode() == HttpStatus.NOT_FOUND)
                    throw new GitHubApiException("Organization '" + orgName + "' not found", 404);
                if (ex.getStatusCode() == HttpStatus.UNAUTHORIZED)
                    throw new GitHubApiException("Invalid GitHub token", 401);
                throw new GitHubApiException("GitHub API error: " + ex.getMessage(), ex.getStatusCode().value());
            }
        }

        log.info("Total repos fetched: {}", allRepos.size());
        return allRepos;
    }

    public List<MemberDto> getOrgMembers(String orgName) {
        List<MemberDto> allMembers = new ArrayList<>();
        int page = 1;

        log.info("Fetching members for org: {}", orgName);

        while (true) {
            String url = baseUrl + "/orgs/" + orgName + "/members?per_page=100&page=" + page;
            try {
                ResponseEntity<MemberDto[]> response = restTemplate.getForEntity(url, MemberDto[].class);
                if (response.getBody() == null || response.getBody().length == 0) break;
                Arrays.stream(response.getBody())
                        .filter(m -> !"Bot".equals(m.getType()))
                        .forEach(allMembers::add);
                if (response.getBody().length < 100) break;
                page++;
            } catch (HttpClientErrorException ex) {
                if (ex.getStatusCode() == HttpStatus.NOT_FOUND)
                    throw new GitHubApiException("Organization '" + orgName + "' not found", 404);
                if (ex.getStatusCode() == HttpStatus.UNAUTHORIZED)
                    throw new GitHubApiException("Invalid GitHub token", 401);
                if (ex.getStatusCode() == HttpStatus.FORBIDDEN)
                    throw new GitHubApiException("Token needs 'read:org' scope to read members", 403);
                throw new GitHubApiException("GitHub API error: " + ex.getMessage(), ex.getStatusCode().value());
            }
        }

        log.info("Total members: {}", allMembers.size());
        return allMembers;
    }

    public List<TeamDto> getOrgTeams(String orgName) {
        List<TeamDto> allTeams = new ArrayList<>();
        int page = 1;

        log.info("Fetching teams for org: {}", orgName);

        while (true) {
            String url = baseUrl + "/orgs/" + orgName + "/teams?per_page=100&page=" + page;
            try {
                ResponseEntity<TeamDto[]> response = restTemplate.getForEntity(url, TeamDto[].class);
                if (response.getBody() == null || response.getBody().length == 0) break;
                allTeams.addAll(Arrays.asList(response.getBody()));
                if (response.getBody().length < 100) break;
                page++;
            } catch (HttpClientErrorException ex) {
                if (ex.getStatusCode() == HttpStatus.FORBIDDEN) {
                    log.warn("Cannot fetch teams - token needs 'read:org' scope");
                    break;
                }
                throw new GitHubApiException("Error fetching teams: " + ex.getMessage(), ex.getStatusCode().value());
            }
        }

        log.info("Total teams: {}", allTeams.size());
        return allTeams;
    }

    public List<MemberDto> getTeamMembers(String orgName, String teamSlug) {
        List<MemberDto> members = new ArrayList<>();
        int page = 1;

        while (true) {
            String url = baseUrl + "/orgs/" + orgName + "/teams/" + teamSlug + "/members?per_page=100&page=" + page;
            try {
                ResponseEntity<MemberDto[]> response = restTemplate.getForEntity(url, MemberDto[].class);
                if (response.getBody() == null || response.getBody().length == 0) break;
                members.addAll(Arrays.asList(response.getBody()));
                if (response.getBody().length < 100) break;
                page++;
            } catch (HttpClientErrorException ex) {
                log.warn("Cannot fetch members for team {}: {}", teamSlug, ex.getStatusCode());
                break;
            }
        }

        return members;
    }

    public List<RepoDto> getTeamRepos(String orgName, String teamSlug) {
        List<RepoDto> repos = new ArrayList<>();
        int page = 1;

        while (true) {
            String url = baseUrl + "/orgs/" + orgName + "/teams/" + teamSlug + "/repos?per_page=100&page=" + page;
            try {
                ResponseEntity<RepoDto[]> response = restTemplate.getForEntity(url, RepoDto[].class);
                if (response.getBody() == null || response.getBody().length == 0) break;
                repos.addAll(Arrays.asList(response.getBody()));
                if (response.getBody().length < 100) break;
                page++;
            } catch (HttpClientErrorException ex) {
                log.warn("Cannot fetch repos for team {}: {}", teamSlug, ex.getStatusCode());
                break;
            }
        }

        return repos;
    }
}
