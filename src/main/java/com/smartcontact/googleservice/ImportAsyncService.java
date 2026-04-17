package com.smartcontact.googleservice;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.smartcontact.dto.GoogleContactsResponse;
import com.smartcontact.dto.ImportResult;
import com.smartcontact.entities.User;

@Service
public class ImportAsyncService {

    @Autowired
    private GoogleContactService googleContactService;

    @Autowired
    private ImportProgressService progressService;


    private GoogleContactsResponse fetchFromGoogle(String token, String nextPageToken) {

    RestTemplate restTemplate = new RestTemplate();

    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);

    HttpEntity<String> entity = new HttpEntity<>(headers);

    String url = "https://people.googleapis.com/v1/people/me/connections"
            + "?personFields=names,emailAddresses,phoneNumbers&pageSize=200"
            + (nextPageToken != null ? "&pageToken=" + nextPageToken : "");

    ResponseEntity<GoogleContactsResponse> response = restTemplate.exchange(
            url,
            HttpMethod.GET,
            entity,
            GoogleContactsResponse.class);

    return response.getBody();
}
    @Async
    public void startFullImport(User user, String token) {

        progressService.init(user.getId());

        int totalFetched = 0;
        int totalSaved = 0;
        int totalSkipped = 0;

        String nextPageToken = null;

        do {
            GoogleContactsResponse body = fetchFromGoogle(token, nextPageToken);

            if (body != null && body.getConnections() != null) {

                totalFetched += body.getConnections().size();

                ImportResult result = googleContactService.importContacts(body.getConnections(), user);

                totalSaved += result.getSaved();
                totalSkipped += result.getSkipped();

                progressService.setTotal(user.getId(), totalFetched);
                progressService.update(user.getId(), totalFetched, totalSaved, totalSkipped);

                nextPageToken = body.getNextPageToken();
            }

        } while (nextPageToken != null);

        progressService.complete(user.getId());
    }
}