package com.github.accessreport.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
public class AccessReportResponse {

    private String organization;

    private int totalRepos;

    private int totalMembers;

    private int totalTeams;

    private Map<String, List<UserRepoAccess>> userAccessMap;

    @Data
    @AllArgsConstructor
    public static class UserRepoAccess {
        private String repoName;
        private String repoUrl;
        private String accessViaTeam;
        private String permission;
        private boolean isPrivate;
    }
}
