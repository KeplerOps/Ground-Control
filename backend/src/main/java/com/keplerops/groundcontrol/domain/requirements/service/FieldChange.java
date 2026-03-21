package com.keplerops.groundcontrol.domain.requirements.service;

/** Represents a single field-level change between two revisions. */
public record FieldChange(Object oldValue, Object newValue) {}
