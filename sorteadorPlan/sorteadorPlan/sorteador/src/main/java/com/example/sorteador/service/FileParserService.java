package com.example.sorteador.service;

import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class FileParserService {

    private final DataFormatter formatter = new DataFormatter();

    public List<String> parse(MultipartFile file) {

        String fileName = file.getOriginalFilename();

        if (fileName == null || fileName.isBlank()) {
            throw new RuntimeException("Arquivo inválido.");
        }

        fileName = fileName.toLowerCase();

        if (fileName.endsWith(".csv") || fileName.endsWith(".txt")) {
            return parseText(file);
        }

        if (fileName.endsWith(".xlsx") || fileName.endsWith(".xls")) {
            return parseExcel(file);
        }

        throw new RuntimeException("Formato não suportado.");
    }

    private List<String> parseText(MultipartFile file) {

        List<String> names = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        file.getInputStream(),
                        StandardCharsets.UTF_8))) {

            String line;

            while ((line = reader.readLine()) != null) {

                if (line.isBlank()) continue;

                String[] parts = line.split("[,;|\\t]+");

                for (String part : parts) {

                    String value = cleanText(part);

                    if (isValidName(value)) {
                        names.add(value);
                    }
                }
            }

            return names;

        } catch (IOException e) {

            e.printStackTrace();

            throw new RuntimeException(
                    "Erro ao ler TXT/CSV: " + e.getMessage()
            );
        }
    }

    private List<String> parseExcel(MultipartFile file) {

        List<String> names = new ArrayList<>();

        try (InputStream input = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(input)) {

            Sheet bestSheet = findBestSheet(workbook);

            if (bestSheet == null) {
                throw new RuntimeException("Nenhuma aba válida encontrada.");
            }

            Row headerRow = findHeaderRow(bestSheet);

            if (headerRow == null) {
                throw new RuntimeException("Cabeçalho não encontrado.");
            }

            int nameColumn = detectNameColumn(headerRow, bestSheet);

            if (nameColumn == -1) {
                throw new RuntimeException(
                        "Não foi possível identificar a coluna de nomes."
                );
            }

            int startRow = 0;

            boolean hasHeader = hasHeaderRow(headerRow);

            if (hasHeader) {
                startRow = headerRow.getRowNum() + 1;
            } else {
                startRow = headerRow.getRowNum();
            }

            for (int i = startRow; i <= bestSheet.getLastRowNum(); i++) {

                Row row = bestSheet.getRow(i);

                if (row == null) continue;

                // evita erro em linhas com poucas colunas

                if (nameColumn >= row.getLastCellNum()) {
                    continue;
                }

                Cell cell = row.getCell(nameColumn);

                if (cell == null) continue;

                String value = cleanText(
                        formatter.formatCellValue(cell)
                );

                if (isValidName(value)) {
                    names.add(value);
                }
            }

            return names;

        } catch (Exception e) {

            System.out.println("=================================");
            System.out.println("ERRO AO PROCESSAR EXCEL");
            System.out.println("Mensagem: " + e.getMessage());
            System.out.println("Classe: " + e.getClass().getName());

            e.printStackTrace();

            throw new RuntimeException(
                    "Erro ao processar planilha: " + e.getMessage()
            );
        }
    }

    private Sheet findBestSheet(Workbook workbook) {

        Sheet bestSheet = null;

        int bestScore = 0;

        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {

            Sheet sheet = workbook.getSheetAt(i);

            int rows = sheet.getPhysicalNumberOfRows();

            if (rows > bestScore) {
                bestScore = rows;
                bestSheet = sheet;
            }
        }

        return bestSheet;
    }

    private Row findHeaderRow(Sheet sheet) {

        for (int i = 0; i <= Math.min(sheet.getLastRowNum(), 15); i++) {

            Row row = sheet.getRow(i);

            if (row == null) continue;

            int filledCells = 0;

            for (Cell cell : row) {

                String value = cleanText(
                        formatter.formatCellValue(cell)
                ).toLowerCase();

                if (!value.isBlank()) {
                    filledCells++;
                }

                if (value.contains("nome")
                        || value.contains("colaborador")
                        || value.contains("funcionario")
                        || value.contains("funcionário")
                        || value.contains("participante")
                        || value.contains("cliente")
                        || value.contains("servidor")) {

                    return row;
                }
            }

            // fallback inteligente:
            // encontrou uma linha que parece tabela

            if (filledCells >= 2) {
                return row;
            }
        }

        return null;
    }

    private int detectNameColumn(Row headerRow, Sheet sheet) {

        String[] possibleNames = {
                "nome",
                "nome completo",
                "colaborador",
                "funcionario",
                "funcionário",
                "servidor",
                "empregado",
                "participante",
                "cliente",
                "usuario",
                "usuário",
                "integrante"
        };

        // PRIMEIRA TENTATIVA:
        // procura pelo cabeçalho

        for (Cell cell : headerRow) {

            String header = cleanText(
                    formatter.formatCellValue(cell)
            ).toLowerCase();

            for (String keyword : possibleNames) {

                if (header.contains(keyword)) {
                    return cell.getColumnIndex();
                }
            }
        }

        // SEGUNDA TENTATIVA:
        // inferência inteligente

        int bestColumn = -1;

        int bestScore = -9999;

        int totalColumns = headerRow.getLastCellNum();

        for (int col = 0; col < totalColumns; col++) {

            int score = 0;

            for (int rowIndex = headerRow.getRowNum() + 1;
                 rowIndex <= Math.min(sheet.getLastRowNum(), 100);
                 rowIndex++) {

                Row row = sheet.getRow(rowIndex);

                if (row == null) continue;

                if (col >= row.getLastCellNum()) continue;

                Cell cell = row.getCell(col);

                if (cell == null) continue;

                String value = cleanText(
                        formatter.formatCellValue(cell)
                );

                if (value.isBlank()) continue;

                String normalized = value.toLowerCase();

                // penalizações

                if (normalized.contains("@")) {
                    score -= 10;
                    continue;
                }

                if (normalized.matches("\\d+")) {
                    score -= 8;
                    continue;
                }

                if (normalized.matches(".*\\d{2}/\\d{2}/\\d{4}.*")) {
                    score -= 8;
                    continue;
                }

                // pontuação positiva

                if (value.length() >= 5) {
                    score += 2;
                }

                if (value.split(" ").length >= 2) {
                    score += 5;
                }

                if (looksLikePersonName(value)) {
                    score += 10;
                }
            }

            if (score > bestScore) {

                bestScore = score;

                bestColumn = col;
            }
        }

        // fallback final

        if (bestColumn == -1) {
            return 0;
        }

        return bestColumn;
    }

    private String cleanText(String value) {

        if (value == null) {
            return "";
        }

        return value
                .replace("\uFEFF", "")
                .replace(":", "")
                .trim();
    }

    private boolean isValidName(String value) {

        if (value == null || value.isBlank()) {
            return false;
        }

        value = cleanText(value);

        String normalized = value.toLowerCase();

        // ignora headers

        if (normalized.startsWith("nome")) return false;

        if (normalized.startsWith("servidor")) return false;

        if (normalized.startsWith("participante")) return false;

        if (normalized.startsWith("funcionario")) return false;

        if (normalized.startsWith("funcionário")) return false;

        // ignora números

        if (value.matches("\\d+")) return false;

        // ignora emails

        if (value.contains("@")) return false;

        // tamanho mínimo

        if (value.length() < 2) return false;

        return true;
    }

    private boolean looksLikePersonName(String value) {

        if (value == null || value.isBlank()) {
            return false;
        }

        value = cleanText(value);

        if (value.contains("@")) return false;

        if (value.matches("\\d+")) return false;

        if (value.matches(".*\\d{2}/\\d{2}/\\d{4}.*")) return false;

        return value.length() >= 3;
    }

    private boolean hasHeaderRow(Row row) {

        if (row == null) {
            return false;
        }

        for (Cell cell : row) {

            String value = cleanText(
                    formatter.formatCellValue(cell)
            ).toLowerCase();

            if (value.contains("nome")
                    || value.contains("participante")
                    || value.contains("cliente")
                    || value.contains("funcionario")
                    || value.contains("funcionário")
                    || value.contains("colaborador")
                    || value.contains("servidor")) {

                return true;
            }
        }

        return false;
    }
}