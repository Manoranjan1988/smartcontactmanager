package com.smartcontact.googleservice;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.smartcontact.dto.ImportProgress;

@Service
public class ImportProgressService {

    private final Map<Long, ImportProgress> progressMap = new ConcurrentHashMap<>();

    public void init(Long userId) {
        ImportProgress p = new ImportProgress();
        p.setProcessed(0);
        p.setSaved(0);
        p.setSkipped(0);
        p.setCompleted(false);
        progressMap.put(userId, p);
    }

    public ImportProgress get(Long userId) {
        return progressMap.get(userId);
    }

    public void update(Long userId, int processed, int saved, int skipped) {
        ImportProgress p =progressMap.get(userId);
        if (p != null) {
            p.setProcessed(processed);
            p.setSaved(saved);
            p.setSkipped(skipped);
        }
    }

    public void setTotal(Long userId, int total){
        ImportProgress p = progressMap.get(userId);
        if(p != null){
            p.setTotal(total);
        }
    }

    public void complete(Long userId){
        ImportProgress p = progressMap.get(userId);
        if(p != null){
            p.setCompleted(true);
        }
    }
}
