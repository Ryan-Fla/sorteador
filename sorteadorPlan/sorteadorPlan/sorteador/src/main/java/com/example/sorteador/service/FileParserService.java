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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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

            // google forms
            if (isGoogleForms(file)) {
                return parseExcel(file);
            }

            // excel padrão
            return parseDefaultExcel(file);
        }

        throw new RuntimeException("Formato não suportado.");
    }

    private List<String> parseDefaultExcel(MultipartFile file) {

        Set<String> names = new LinkedHashSet<>();

        try (InputStream input = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(input)) {

            Sheet bestSheet = findBestSheet(workbook);

            if (bestSheet == null) {
                throw new RuntimeException(
                        "Nenhuma aba válida encontrada."
                );
            }

            String[] possibleNames = {
                    "nome",
                    "nome completo",
                    "seu nome",
                    "digite seu nome",
                    "nome e sobrenome",
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

            for (Row row : bestSheet) {

                if (row == null) {
                    continue;
                }

                boolean isNameRow = false;

                // detecta linha de nomes

                for (Cell cell : row) {

                    String value = cleanText(
                            formatter.formatCellValue(cell)
                    ).toLowerCase();

                    for (String keyword : possibleNames) {

                        if (value.contains(keyword)) {
                            isNameRow = true;
                            break;
                        }
                    }

                    if (isNameRow) {
                        break;
                    }
                }

                // lê apenas a linha correta

                if (isNameRow) {

                    for (Cell cell : row) {

                        if (cell == null) {
                            continue;
                        }

                        String value = cleanText(
                                formatter.formatCellValue(cell)
                        );

                        if (isValidName(value)) {
                            names.add(value);
                        }
                    }

                    break;
                }
            }

            return new ArrayList<>(names);

        } catch (Exception e) {

            throw new RuntimeException(
                    "Erro ao processar planilha: " + e.getMessage()
            );
        }
    }
    private boolean isGoogleForms(MultipartFile file) {

        try (InputStream input = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(input)) {

            Sheet sheet = workbook.getSheetAt(0);

            Row firstRow = sheet.getRow(0);

            if (firstRow == null) {
                return false;
            }

            for (Cell cell : firstRow) {

                String value = cleanText(
                        formatter.formatCellValue(cell)
                ).toLowerCase();

                if (value.contains("carimbo de data/hora")
                        || value.contains("timestamp")
                        || value.contains("endereço de e-mail")
                        || value.contains("email")) {

                    return true;
                }
            }

            return false;

        } catch (Exception e) {

            return false;
        }
    }

    private List<String> parseText(MultipartFile file) {

        Set<String> names = new LinkedHashSet<>();

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

            return new ArrayList<>(names);

        } catch (IOException e) {

            e.printStackTrace();

            throw new RuntimeException(
                    "Erro ao ler TXT/CSV: " + e.getMessage()
            );
        }
    }

    private List<String> parseExcel(MultipartFile file) {

        Set<String> names = new LinkedHashSet<>();

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

            int startRow;

            boolean hasHeader = hasHeaderRow(headerRow);

            if (hasHeader) {
                startRow = headerRow.getRowNum() + 1;
            } else {
                startRow = headerRow.getRowNum();
            }

            for (int i = startRow; i <= bestSheet.getLastRowNum(); i++) {

                Row row = bestSheet.getRow(i);

                if (row == null) continue;

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

            return new ArrayList<>(names);

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
                "seu nome",
                "digite seu nome",
                "nome e sobrenome",
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

        // procura cabeçalhoF

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
                    score -= 15;
                    continue;
                }

                if (normalized.matches("\\d+")) {
                    score -= 10;
                    continue;
                }

                if (normalized.matches(".*\\d{2}/\\d{2}/\\d{4}.*")) {
                    score -= 10;
                    continue;
                }

                if (normalized.contains("http")) {
                    score -= 10;
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

        // fallback

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
                .replace("\"", "")
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

        // ignora links

        if (normalized.contains("http")) return false;

        // ignora datas

        if (normalized.matches(".*\\d{2}/\\d{2}/\\d{4}.*")) return false;

        // ignora palavras comuns

        if (normalized.equals("sim")) return false;

        if (normalized.equals("não")) return false;

        if (normalized.equals("nao")) return false;

        if (normalized.equals("ok")) return false;

        if (normalized.equals("confirmado")) return false;

        if (normalized.equals("presente")) return false;

        // tamanho mínimo

        if (value.length() < 2) return false;

        return true;
    }

    private boolean looksLikePersonName(String value) {

        if (value == null || value.isBlank()) {
            return false;
        }

        value = cleanText(value);

        String normalized = value.toLowerCase();

        if (normalized.contains("@")) return false;

        if (normalized.matches("\\d+")) return false;

        if (normalized.matches(".*\\d{2}/\\d{2}/\\d{4}.*")) return false;

        if (normalized.contains("http")) return false;

        if (normalized.equals("sim")) return false;

        if (normalized.equals("não")) return false;

        if (normalized.equals("nao")) return false;

        if (normalized.equals("ok")) return false;

        if (normalized.equals("confirmado")) return false;

        if (normalized.equals("presente")) return false;

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