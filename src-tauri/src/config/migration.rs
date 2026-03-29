// XML → JSON migration for preferences.xml and overrides.xml (XStream format)
// On first launch: check for ~/.tvrenamer/prefs.json FIRST — skip if it already exists.
// If not found, check for ~/.tvrenamer/preferences.xml — migrate if present.
// Leave preferences.xml in place as a backup after migration.
// Implementation: preferences plan
