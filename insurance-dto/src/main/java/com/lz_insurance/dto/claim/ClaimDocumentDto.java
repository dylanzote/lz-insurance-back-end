package com.lz_insurance.dto.claim;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClaimDocumentDto {

    private String documentId;
    private String documentName;
    private String documentType;
    private String storagePath;
    private String mimeType;
    private long fileSize;
    private LocalDateTime uploadedAt;
    private String uploadedBy;
}
