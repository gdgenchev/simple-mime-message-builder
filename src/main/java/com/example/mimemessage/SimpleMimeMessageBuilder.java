package com.example.mimemessage;

import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class SimpleMimeMessageBuilder {

    private static final String FALLBACK_FILENAME_PREFIX = "file_";
    private static final String APPLICATION_OCTET_STREAM = "application/octet-stream";

    private String from;
    private String subject;
    private String to;
    private String text;
    private boolean isHtml;
    private List<Attachment> attachments;

    public SimpleMimeMessageBuilder from(String from) {
        this.from = from;
        return this;
    }

    public SimpleMimeMessageBuilder subject(String subject) {
        this.subject = subject;
        return this;
    }

    public SimpleMimeMessageBuilder to(String to) {
        this.to = to;
        return this;
    }

    public SimpleMimeMessageBuilder text(String text) {
        this.text = text;
        this.isHtml = false;
        return this;
    }

    public SimpleMimeMessageBuilder html(String html) {
        this.text = html;
        this.isHtml = true;
        return this;
    }

    public SimpleMimeMessageBuilder attachments(List<Attachment> attachments) {
        this.attachments = attachments;
        return this;
    }

    public MimeMessage build(Session session) throws EmailServiceException {
        MimeMessage message = new MimeMessage(session);

        try {
            message.setFrom(new InternetAddress(from));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(to));
            message.setSubject(subject, StandardCharsets.UTF_8.name());

            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(createBodyPart());
            addAttachments(multipart, attachments);

            message.setContent(multipart);

            return message;
        } catch (MessagingException e) {
            throw new EmailServiceException(e);
        }
    }

    private MimeBodyPart createBodyPart() throws MessagingException {
        MimeBodyPart bodyPart = new MimeBodyPart();
        if (isHtml) {
            bodyPart.setContent(text, "text/html; charset=UTF-8");
        } else {
            bodyPart.setText(text, StandardCharsets.UTF_8.name());
        }
        return bodyPart;
    }

    private void addAttachments(Multipart multipart, List<Attachment> attachments) throws MessagingException {
        if (attachments == null) {
            return;
        }

        for (int i = 0; i < attachments.size(); i++) {
            Attachment attachment = attachments.get(i);
            if (attachment.content() == null || attachment.content().length == 0) {
                continue;
            }

            String filename = attachment.fileName();
            if (filename == null || filename.isEmpty()) {
                filename = FALLBACK_FILENAME_PREFIX + i;
            }

            MimeBodyPart attachmentPart = new MimeBodyPart();
            attachmentPart.setContent(attachment.content(), APPLICATION_OCTET_STREAM);
            attachmentPart.setFileName(filename);

            multipart.addBodyPart(attachmentPart);
        }
    }
}
