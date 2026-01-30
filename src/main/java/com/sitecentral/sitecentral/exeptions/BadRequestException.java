package com.sitecentral.sitecentral.exeptions;

import lombok.Getter;

@Getter
public class BadRequestException extends RuntimeException {

    private final String[] details;

    public BadRequestException(String message, String... details) {
        super(message);
        this.details = details;
    }

}