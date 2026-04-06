---- MODULE RequirementStatusMachine ----
EXTENDS Naturals, Sequences

CONSTANTS DRAFT, ACTIVE, DEPRECATED, ARCHIVED

VARIABLE status

Init == status = DRAFT

Next ==
    \/ /\ status = DRAFT
       /\ status' = ACTIVE
    \/ /\ status = ACTIVE
       /\ status' = DEPRECATED
    \/ /\ status = ACTIVE
       /\ status' = ARCHIVED
    \/ /\ status = DEPRECATED
       /\ status' = ARCHIVED

NoExitFromArchived == status = ARCHIVED => status' = ARCHIVED

====
