package dev.stiebo.aiutilities.service.impl;

import dev.stiebo.aiutilities.dto.Flashcard;
import dev.stiebo.aiutilities.dto.Flashcards;
import dev.stiebo.aiutilities.exception.FileErrorException;
import dev.stiebo.aiutilities.service.ChatClientService;
import dev.stiebo.aiutilities.service.FlashcardService;
import dev.stiebo.aiutilities.service.UtilityService;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;

@Service
public class FlashcardServiceImpl implements FlashcardService {
    private final ChatClientService chatClientService;
    private final UtilityService utilityService;

    @Value("classpath:/prompts/flashcardscsvprompt.st")
    private Resource flashcardsPrompt;

    @Autowired
    public FlashcardServiceImpl(ChatClientService chatClientService, UtilityService utilityService) {
        this.chatClientService = chatClientService;
        this.utilityService = utilityService;
    }

    @Override
    public byte[] createCsvFlashcardsFromFile(MultipartFile file) {
        return convertToCsv(createFlashcardsFromFile(file));
    }

    @Override
    public List<Flashcard> createFlashcardsFromFile(MultipartFile file) {
        String contentType = getContentTypeOrThrowException(file);
        Flashcards flashcards = switch (contentType) {
            // call chatClient with (Image-)Resource
            case "image/jpeg", "image/gif", "image/png" -> chatClientService.getResponse(
                    Flashcards.class, flashcardsPrompt, utilityService.convertImageFileToResource(file));
            // call with (String-)document
            case "application/pdf" -> chatClientService.getResponse(
                    Flashcards.class, flashcardsPrompt, utilityService.convertPdfToText(file));
            default -> throw new IllegalStateException("Unexpected value: " + contentType);
        };
        return flashcards.flashcards();
    }

    byte[] convertToCsv(List<Flashcard> flashcards) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             OutputStreamWriter osw = new OutputStreamWriter(baos);
             CSVPrinter csvPrinter = new CSVPrinter(osw,
                     CSVFormat.Builder.create()
                             .setDelimiter('\t')
                             .build())) {
            for (Flashcard flashcard : flashcards) {
                csvPrinter.printRecord(flashcard.question(), flashcard.answer());
            }
            csvPrinter.flush();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Error converting to csv");
        }
    }

    String getContentTypeOrThrowException(MultipartFile file) throws FileErrorException {
        return switch (file.getContentType()) {
            case "image/jpeg", "image/gif", "image/png", "application/pdf" -> file.getContentType();
            case null, default -> {
                // Fallback: determine the content type based on file extension
                String fileName = file.getOriginalFilename();
                if (fileName != null) {
                    if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
                        yield "image/jpeg";
                    } else if (fileName.endsWith(".gif")) {
                        yield "image/gif";
                    } else if (fileName.endsWith(".png")) {
                        yield "image/png";
                    } else if (fileName.endsWith(".pdf")) {
                        yield "application/pdf";
                    }
                }
                throw new FileErrorException("Invalid File Type: " + file.getContentType());
            }
        };
    }


}
