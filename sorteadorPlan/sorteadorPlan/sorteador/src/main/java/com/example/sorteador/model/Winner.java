package com.example.sorteador.model;

 import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Winner {

    private Long id;
    private String name;
    private Integer position;
    private Draw draw;
}
