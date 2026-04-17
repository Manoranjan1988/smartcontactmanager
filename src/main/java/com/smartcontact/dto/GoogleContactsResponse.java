package com.smartcontact.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GoogleContactsResponse {
    private List<GooglePerson> connections;
    private String nextPageToken;
}
