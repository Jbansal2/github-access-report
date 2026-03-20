package com.github.accessreport.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CollaboratorDto {

    private Long id;

    private String login;

    @JsonProperty("html_url")
    private String profileUrl;

    @JsonProperty("role_name")
    private String role;
}
