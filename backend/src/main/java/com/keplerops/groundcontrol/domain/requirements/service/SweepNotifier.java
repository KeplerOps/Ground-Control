package com.keplerops.groundcontrol.domain.requirements.service;

/** Port for delivering analysis sweep notifications to external systems. */
public interface SweepNotifier {

    void notify(SweepReport report);
}
