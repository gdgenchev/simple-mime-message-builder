package com.example.mimemessage;

import jakarta.activation.DataHandler;
import jakarta.activation.FileDataSource;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class SimpleMimeMessageBuilder {

    public record Attachment(String fileName, Path filePath) {

    }

    private static final String FALLBACK_FILENAME_PREFIX = "file_";

    private String from;
    private String subject;
    private String to;
    private String text;
    private boolean isHtml;
    private List<Attachment> attachments = Collections.emptyList();

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
        this.attachments = attachments != null ? Collections.unmodifiableList(attachments) : Collections.emptyList();
        return this;
    }

    public MimeMessage build(Session session) throws MessagingException {
        validateFields();

        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(from));
        message.setRecipient(Message.RecipientType.TO, new InternetAddress(to));
        message.setSubject(subject, StandardCharsets.UTF_8.name());

        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(createBodyPart());
        addAttachments(multipart, attachments);

        message.setContent(multipart);

        return message;
    }

    private void validateFields() {
        Objects.requireNonNull(from, "Sender (from) must not be null");
        Objects.requireNonNull(to, "Recipient (to) must not be null");
        Objects.requireNonNull(subject, "Subject must not be null");
        Objects.requireNonNull(text, "Message body must not be null");
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
        for (int i = 0; i < attachments.size(); i++) {
            Attachment attachment = attachments.get(i);

            if (attachment.filePath() == null || !Files.exists(attachment.filePath())) {
                continue;
            }

            String filename = attachment.fileName();
            if (filename == null || filename.isEmpty()) {
                filename = FALLBACK_FILENAME_PREFIX + i;
            }

            MimeBodyPart attachmentPart = new MimeBodyPart();
            attachmentPart.setDataHandler(new DataHandler(new FileDataSource(attachment.filePath().toFile())));
            attachmentPart.setFileName(filename);

            multipart.addBodyPart(attachmentPart);
        }
    }
}
