package com.github.accessreport.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TeamDto {

    private Long id;

    private String name;

    private String slug;

    @JsonProperty("repos_count")
    private int reposCount;

    @JsonProperty("members_count")
    private int membersCount;
}
