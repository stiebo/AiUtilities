package dev.stiebo.aiutilities.controller;

import dev.stiebo.aiutilities.dto.CVDataOutDto;
import dev.stiebo.aiutilities.exception.ErrorResponse;
import dev.stiebo.aiutilities.exception.ValidationErrorResponse;
import dev.stiebo.aiutilities.service.CVService;
import dev.stiebo.aiutilities.validation.NotEmptyFile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
@Validated
public class AnalyzeCvController {

    private final CVService cvService;

    @Autowired
    public AnalyzeCvController(CVService cvService) {
        this.cvService = cvService;
    }

    @Operation(summary = "Analyze CV",
            description = "Analyze any CV and get a breakdown of its content in json")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "CV analyzed",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CVDataOutDto.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed for request parameter",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ValidationErrorResponse.class)
                    )),
            @ApiResponse(responseCode = "422", description = "File Error",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    ))
    })
    @PostMapping(value = "/analyzeCV", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CVDataOutDto analyzeCv(@RequestParam(value = "file", required = false)
                                  @NotEmptyFile MultipartFile file) {
        return cvService.getCVData(file);
    }

}
