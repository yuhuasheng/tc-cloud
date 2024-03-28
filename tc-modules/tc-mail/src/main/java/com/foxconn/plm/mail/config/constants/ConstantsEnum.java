package com.foxconn.plm.mail.config.constants;

/**
 * @Classname ConstantsEnum
 * @Description
 * @Date 2022/1/4 19:37
 * @Created by HuashengYu
 */
public enum ConstantsEnum {

    TCMAILFOLENAME("TCMail"), TC_MAIL_EXE_PATH("C:\\Siemens\\Teamcenter12\\bin\\tc_mail_smtp_ootb.exe"), TCMAILATTACHMENTFOLD("TCAttachment");

    private final String value;

    private ConstantsEnum(String value) {
        this.value = value;
    }

    public String value() {
        return this.value;
    }
}
