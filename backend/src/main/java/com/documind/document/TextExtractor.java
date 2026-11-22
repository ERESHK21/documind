package com.documind.document;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

/**
 * Turns an uploaded file into per-page text.
 * PDF pages keep their real page numbers; plain text and Markdown are a single page.
 */
@Component
public class TextExtractor {

    public record Extracted(List<String> pages) {

        public String joined() {
            return String.join("\n\n", pages);
        }
    }

    public Extracted extract(byte[] bytes, String filename, String contentType) {
        boolean pdf = filename.toLowerCase().endsWith(".pdf")
                || "application/pdf".equalsIgnoreCase(contentType);
        return pdf ? extractPdf(bytes) : extractPlain(bytes);
    }

    private Extracted extractPdf(byte[] bytes) {
        try (PDDocument pdf = Loader.loadPDF(bytes)) {
            int pages = pdf.getNumberOfPages();
            if (pages == 0) {
                throw com.documind.common.ApiException.badRequest("This PDF has no pages");
            }
            if (pages > 300) {
                throw com.documind.common.ApiException.badRequest("This PDF is too large to index (max 300 pages)");
            }
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            List<String> result = new ArrayList<>();
            for (int i = 1; i <= pages; i++) {
                stripper.setStartPage(i);
                stripper.setEndPage(i);
                result.add(stripper.getText(pdf));
            }
            return new Extracted(result);
        } catch (com.documind.common.ApiException ex) {
            throw ex;
        } catch (IOException ex) {
            throw com.documind.common.ApiException.badRequest(
                    "Could not read this PDF. Use a text-based PDF (scans need OCR first).");
        }
    }

    private Extracted extractPlain(byte[] bytes) {
        String text = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
        if (text.startsWith("\uFEFF")) {
            text = text.substring(1);
        }
        if (text.indexOf('\u0000') >= 0) {
            throw com.documind.common.ApiException.badRequest("This file does not look like readable text");
        }
        return new Extracted(List.of(text));
    }
}
