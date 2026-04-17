package com.smartcontact.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ImportProgress {
    private int total;
    private int processed;
    private int saved;
    private int skipped;
    private boolean completed;

}
