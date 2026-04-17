package com.smartcontact.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GooglePerson {
    private List<GoogleName> names;
    private List<GoogleEmail> emailAddresses;
    private List<GooglePhone> phoneNumbers;
}
