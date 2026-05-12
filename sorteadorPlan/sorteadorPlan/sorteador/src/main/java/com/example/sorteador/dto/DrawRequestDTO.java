package com.example.sorteador.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;


public class DrawRequestDTO {

    private Integer winnersCount;
    private boolean allowRepeat;
    private boolean removeDuplicates;

    public boolean isRemoveDuplicates() {
        return removeDuplicates;
    }

    public void setRemoveDuplicates(boolean removeDuplicates) {
        this.removeDuplicates = removeDuplicates;
    }

    public Integer getWinnersCount() {
        return winnersCount;
    }

    public void setWinnersCount(Integer winnersCount) {
        this.winnersCount = winnersCount;
    }

    public boolean isAllowRepeat() {
        return allowRepeat;
    }

    public void setAllowRepeat(boolean allowRepeat) {
        this.allowRepeat = allowRepeat;
    }
}
