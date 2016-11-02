package org.tvrenamer.controller;

/**
 * Represents a change event of user preferences
 * @author Dave Harris
 */
public class UserPreferencesChangeEvent {
    /** The preference field that has changed */
    private String preference;
    /** The new value of the preference */
    private Object newValue;

    public UserPreferencesChangeEvent(String preference, Object newValue) {
        this.preference = preference;
        this.newValue = newValue;
    }

    public String getPreference() {
        return preference;
    }
    public void setPreference(String preference) {
        this.preference = preference;
    }
    public Object getNewValue() {
        return newValue;
    }
    public void setNewValue(Object newValue) {
        this.newValue = newValue;
    }
    @Override
    public String toString() {
        return "UserPreferencesChangeEvent [preference=" + preference + ", newValue=" + newValue + "]";
    }
}
