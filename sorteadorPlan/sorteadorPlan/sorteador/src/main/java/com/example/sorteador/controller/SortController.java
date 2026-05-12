package com.example.sorteador.controller;

import com.example.sorteador.dto.DrawRequestDTO;
import com.example.sorteador.dto.DrawResultDTO;
import com.example.sorteador.service.DrawService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/sort")
public class SortController {

    private final DrawService drawService;

    public SortController(DrawService drawService) {
        this.drawService = drawService;
    }

    @PostMapping
    public ResponseEntity<?> sort(
            @RequestParam("file") MultipartFile file,
            @RequestParam("winnersCount") Integer winnersCount,
            @RequestParam(value = "allowRepeat", required = false, defaultValue = "false") Boolean allowRepeat,
            @RequestParam(value = "removeDuplicates", required = false, defaultValue = "false") Boolean removeDuplicates
    ) {

        try {
            // 🔍 Logs pra debug
            System.out.println("Arquivo recebido: " + file.getOriginalFilename());
            System.out.println("Quantidade: " + winnersCount);
            System.out.println("AllowRepeat: " + allowRepeat);
            System.out.println("RemoveDuplicates: " + removeDuplicates);

            // 🧠 Monta DTO
            DrawRequestDTO request = new DrawRequestDTO();
            request.setWinnersCount(winnersCount);
            request.setAllowRepeat(allowRepeat);
            request.setRemoveDuplicates(removeDuplicates);

            DrawResultDTO result = drawService.execute(file, request);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
             e.printStackTrace();
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

     @PostMapping("/test")
    public ResponseEntity<?> test(@RequestParam("file") MultipartFile file) {
        System.out.println("BATEU NO ENDPOINT TEST");
        return ResponseEntity.ok("ok");
    }
}