package com.sitecentral.sitecentral.exeptions;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@AllArgsConstructor
@Getter
@Setter
public class ErrorsPayload {
    private String message;
    private LocalDateTime timestamp;
    private Object details;
    private String exceptionType;
}
