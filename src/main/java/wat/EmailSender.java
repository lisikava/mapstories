package wat;

import org.simplejavamail.api.email.Email;
import org.simplejavamail.api.mailer.Mailer;
import org.simplejavamail.api.mailer.config.TransportStrategy;
import org.simplejavamail.email.EmailBuilder;
import org.simplejavamail.mailer.MailerBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

public class EmailSender {
    private final static Mailer mailer = initializeMailer();

    private static Properties props;

    private static Properties loadProperties() {
        Properties properties = new Properties();
        try (InputStream stream = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream("mailer.properties");
             InputStream secretsStream = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream("mailer-secrets.properties")) {
            properties.load(stream);
            properties.load(secretsStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return properties;
    }

    private static Mailer initializeMailer() {
        props = loadProperties();
        return MailerBuilder.withSMTPServer(props.getProperty("host"),
                                            Integer.parseInt(props.getProperty(
                                                    "port")),
                                            props.getProperty("username"),
                                            props.getProperty("password")
                )
                .withTransportStrategy(TransportStrategy.SMTP_TLS)
                .async()
                .buildMailer();
    }

    /**
     * Tries sending the email asynchronously. Discarding the return value
     * constitutes non-blocking execution, but the results won't be tracked.
     * JVM exiting or mailer's ExecutorService shutting down might result in
     * failure to deliver the message.
     * @param recipient recipient email address
     * @param subject email subject
     * @param body email body
     * @return handle to the result of asynchronous execution
     */
    public static CompletableFuture<Void> composeEmail(String recipient,
                                                       String subject,
                                                       String body
    ) {
        Email email = EmailBuilder.startingBlank()
                .from(props.getProperty("nickname"),
                      props.getProperty("username")
                )
                .to(recipient)
                .withReplyTo(props.getProperty("nickname"),
                             props.getProperty("username")
                )
                .withSubject(subject)
                .withPlainText(body)
                .buildEmail();
        return mailer.sendMail(email);
    }

    private EmailSender() {}
}
