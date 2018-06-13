package org.tvrenamer.model;

import org.eclipse.swt.SWT;

public enum SWTMessageBoxType {
    DLG_ERR(SWT.ICON_ERROR),
    // DLG_INFO(SWT.ICON_INFORMATION),
    // DLG_QUES(SWT.ICON_QUESTION),
    DLG_WARN(SWT.ICON_WARNING),
    // DLG_WRKG(SWT.ICON_WORKING),
    DLG_OK(SWT.OK);

    private final int swtIconValue;
    SWTMessageBoxType(int swtIconValue) {
        this.swtIconValue = swtIconValue;
    }

    public int getSwtIconValue() {
        return swtIconValue;
    }
}
