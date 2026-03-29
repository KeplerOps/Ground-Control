package com.keplerops.groundcontrol.domain.requirements.service;

import com.keplerops.groundcontrol.domain.exception.GroundControlException;

/** Thrown when export generation fails due to I/O or formatting errors. */
public class ExportException extends GroundControlException {

    public ExportException(String message, Throwable cause) {
        super(message, "export_failed", cause);
    }
}
