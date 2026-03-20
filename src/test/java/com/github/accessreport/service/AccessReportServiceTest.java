package com.github.accessreport.service;

import com.github.accessreport.dto.AccessReportResponse;
import com.github.accessreport.dto.CollaboratorDto;
import com.github.accessreport.dto.RepoDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccessReportServiceTest {

    @Mock
    private GitHubClient gitHubClient;

    @InjectMocks
    private AccessReportService accessReportService;

    private RepoDto repo1;
    private RepoDto repo2;
    private CollaboratorDto user1;
    private CollaboratorDto user2;

    @BeforeEach
    void setUp() {
        repo1 = new RepoDto();
        repo1.setId(1L);
        repo1.setName("backend-service");
        repo1.setHtmlUrl("https://github.com/testorg/backend-service");
        repo1.setPrivate(true);

        repo2 = new RepoDto();
        repo2.setId(2L);
        repo2.setName("frontend-app");
        repo2.setHtmlUrl("https://github.com/testorg/frontend-app");
        repo2.setPrivate(false);

        user1 = new CollaboratorDto();
        user1.setLogin("john_doe");
        user1.setRole("admin");

        user2 = new CollaboratorDto();
        user2.setLogin("jane_smith");
        user2.setRole("write");
    }

    @Test
    void shouldReturnEmptyReportWhenOrgHasNoRepos() {
        when(gitHubClient.getAllOrgRepos("emptyorg")).thenReturn(List.of());

        AccessReportResponse report = accessReportService.generateReport("emptyorg");

        assertThat(report.getTotalRepos()).isEqualTo(0);
        assertThat(report.getTotalUsers()).isEqualTo(0);
        assertThat(report.getUserAccessMap()).isEmpty();
    }

    @Test
    void shouldCorrectlyMapUsersToRepos() {
        when(gitHubClient.getAllOrgRepos("testorg")).thenReturn(List.of(repo1, repo2));
        when(gitHubClient.getRepoCollaborators("testorg", "backend-service")).thenReturn(List.of(user1, user2));
        when(gitHubClient.getRepoCollaborators("testorg", "frontend-app")).thenReturn(List.of(user1));

        AccessReportResponse report = accessReportService.generateReport("testorg");

        assertThat(report.getTotalRepos()).isEqualTo(2);
        assertThat(report.getTotalUsers()).isEqualTo(2);

        // john has access to both repos
        List<AccessReportResponse.UserRepoAccess> johnRepos = report.getUserAccessMap().get("john_doe");
        assertThat(johnRepos).hasSize(2);

        // jane only has access to backend-service
        List<AccessReportResponse.UserRepoAccess> janeRepos = report.getUserAccessMap().get("jane_smith");
        assertThat(janeRepos).hasSize(1);
        assertThat(janeRepos.get(0).getRepoName()).isEqualTo("backend-service");
    }

    @Test
    void shouldSetCorrectOrgNameInReport() {
        when(gitHubClient.getAllOrgRepos("myorg")).thenReturn(List.of(repo1));
        when(gitHubClient.getRepoCollaborators("myorg", "backend-service")).thenReturn(List.of());

        AccessReportResponse report = accessReportService.generateReport("myorg");

        assertThat(report.getOrganization()).isEqualTo("myorg");
    }
}
