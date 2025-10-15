package com.gotree.API.dto.report;

import lombok.Data;

@Data
public class PhotoRecordDTO {
    private String description;
    private String imageBase64; // Receberá a imagem como texto
}