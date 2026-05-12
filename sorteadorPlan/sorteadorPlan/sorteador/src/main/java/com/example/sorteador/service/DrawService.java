package com.example.sorteador.service;

import com.example.sorteador.dto.DrawRequestDTO;
import com.example.sorteador.dto.DrawResultDTO;
import com.example.sorteador.strategy.DrawStrategy;
import com.example.sorteador.strategy.NoRepeatStrategy;
import com.example.sorteador.strategy.WithRepeatStrategy;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import org.slf4j.Logger;
@Service
public class DrawService {

    private static final Logger log = LoggerFactory.getLogger(DrawService.class);

    private final FileParserService parser;

    public DrawService(FileParserService parser) {
        this.parser = parser;
    }

    public DrawResultDTO execute(MultipartFile file, DrawRequestDTO request) {

        log.info("Iniciando sorteio");

        List<String> names = parser.parse(file);

        if (names.isEmpty()) {
            throw new RuntimeException("Nenhum nome encontrado");
        }

        if (request.isRemoveDuplicates()) {
            names = new ArrayList<>(new HashSet<>(names));
        }

        if (request.getWinnersCount() > names.size() && !request.isAllowRepeat()) {
            throw new RuntimeException("Quantidade de vencedores maior que participantes");
        }

        DrawStrategy strategy = request.isAllowRepeat()
                ? new WithRepeatStrategy()
                : new NoRepeatStrategy();

        List<String> winners = strategy.draw(names, request.getWinnersCount());

        DrawResultDTO result = new DrawResultDTO();
        result.setDrawId(UUID.randomUUID());
        result.setParticipants(names);
        result.setWinners(winners);
        result.setTimestamp(LocalDateTime.now());

        log.info("Sorteio finalizado: {}", result.getDrawId());

        return result;
    }
}