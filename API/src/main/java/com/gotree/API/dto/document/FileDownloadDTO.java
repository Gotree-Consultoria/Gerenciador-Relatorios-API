package com.gotree.API.dto.document;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FileDownloadDTO {
    private String filename;
    private byte[] data;
}