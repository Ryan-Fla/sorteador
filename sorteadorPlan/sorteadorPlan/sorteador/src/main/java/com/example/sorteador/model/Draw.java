package com.example.sorteador.model;

  import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Draw {

    private Long id;
    private String filename;
    private Integer totalParticipants;
    private Integer winnerCount;
    private LocalDateTime createAT;


}
