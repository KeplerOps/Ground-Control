package com.keplerops.groundcontrol.domain.audit;

import org.hibernate.envers.RevisionListener;

public class GroundControlRevisionListener implements RevisionListener {

    @Override
    public void newRevision(Object revisionEntity) {
        var rev = (GroundControlRevisionEntity) revisionEntity;
        rev.setActor(ActorHolder.get());
    }
}
