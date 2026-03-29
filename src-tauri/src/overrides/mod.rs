// Show name overrides — ports GlobalOverrides.java
// CRITICAL: GlobalOverrides.getShowName() exists in Java but is NEVER called in production.
// This port must wire it into the lookup flow: AFTER parser output, BEFORE provider query.
// Format: JSON array [{"from": "Archer (2009)", "to": "Archer"}]
// Migrated from etc/default-overrides.xml on first launch.
