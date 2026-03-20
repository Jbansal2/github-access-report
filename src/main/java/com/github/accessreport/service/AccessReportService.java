package com.github.accessreport.service;

import com.github.accessreport.dto.AccessReportResponse;
import com.github.accessreport.dto.MemberDto;
import com.github.accessreport.dto.RepoDto;
import com.github.accessreport.dto.TeamDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccessReportService {

    private final GitHubClient gitHubClient;

    private final ExecutorService executor = Executors.newFixedThreadPool(5);

    public AccessReportResponse generateReport(String orgName) {
        log.info("Starting report for org: {}", orgName);

        List<RepoDto> allRepos = gitHubClient.getAllOrgRepos(orgName);
        log.info("Total repos: {}", allRepos.size());

        List<MemberDto> allMembers = gitHubClient.getOrgMembers(orgName);
        log.info("Total members: {}", allMembers.size());

        List<TeamDto> teams = gitHubClient.getOrgTeams(orgName);
        log.info("Total teams: {}", teams.size());

        if (teams.isEmpty()) {
            log.warn("No teams found - returning basic report without user-repo mapping");
            return new AccessReportResponse(orgName, allRepos.size(), allMembers.size(), 0, new HashMap<>());
        }
        List<CompletableFuture<Void>> futures = teams.stream()
                .map(team -> CompletableFuture.runAsync(() -> {
                    CompletableFuture<List<MemberDto>> membersFuture = CompletableFuture
                            .supplyAsync(() -> gitHubClient.getTeamMembers(orgName, team.getSlug()), executor);

                    CompletableFuture<List<RepoDto>> reposFuture = CompletableFuture
                            .supplyAsync(() -> gitHubClient.getTeamRepos(orgName, team.getSlug()), executor);

                    List<MemberDto> teamMembers = membersFuture.join();
                    List<RepoDto> teamRepos = reposFuture.join();

                    log.debug("Team '{}' - {} members, {} repos", team.getName(), teamMembers.size(), teamRepos.size());

                    team.setName(team.getName());
                    teamDataMap.computeIfAbsent(team.getSlug(), k -> new TeamData(team.getName(), teamMembers, teamRepos));

                }, executor))
                .collect(Collectors.toList());

        futures.forEach(CompletableFuture::join);

        Map<String, List<AccessReportResponse.UserRepoAccess>> userAccessMap = new HashMap<>();

        for (Map.Entry<String, TeamData> entry : teamDataMap.entrySet()) {
            TeamData data = entry.getValue();

            for (MemberDto member : data.members) {
                for (RepoDto repo : data.repos) {
                    AccessReportResponse.UserRepoAccess access = new AccessReportResponse.UserRepoAccess(
                            repo.getName(),
                            repo.getHtmlUrl(),
                            data.teamName,
                            repo.getPermission(),
                            repo.isPrivate()
                    );

                    userAccessMap.computeIfAbsent(member.getLogin(), k -> new ArrayList<>()).add(access);
                }
            }
        }

        userAccessMap.replaceAll((user, repoList) ->
                repoList.stream()
                        .collect(Collectors.collectingAndThen(
                                Collectors.toMap(
                                        AccessReportResponse.UserRepoAccess::getRepoName,
                                        r -> r,
                                        (existing, replacement) -> existing  // keep first occurrence
                                ),
                                m -> new ArrayList<>(m.values())
                        ))
        );

        log.info("Report done - repos: {}, members: {}, teams: {}, users with access: {}",
                allRepos.size(), allMembers.size(), teams.size(), userAccessMap.size());

        return new AccessReportResponse(
                orgName,
                allRepos.size(),
                allMembers.size(),
                teams.size(),
                userAccessMap
        );
    }

    private final Map<String, TeamData> teamDataMap = Collections.synchronizedMap(new HashMap<>());

    private static class TeamData {
        String teamName;
        List<MemberDto> members;
        List<RepoDto> repos;

        TeamData(String teamName, List<MemberDto> members, List<RepoDto> repos) {
            this.teamName = teamName;
            this.members = members;
            this.repos = repos;
        }
    }
}
