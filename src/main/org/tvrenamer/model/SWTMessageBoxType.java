package org.tvrenamer.model;

import org.eclipse.swt.SWT;

public enum SWTMessageBoxType {
    OK(SWT.OK),
    // QUESTION(SWT.ICON_QUESTION),
    // MESSAGE(SWT.ICON_INFORMATION),
    WARNING(SWT.ICON_WARNING),
    ERROR(SWT.ICON_ERROR);

    private final int swtIconValue;
    SWTMessageBoxType(int swtIconValue) {
        this.swtIconValue = swtIconValue;
    }

    public int getSwtIconValue() {
        return swtIconValue;
    }
}
