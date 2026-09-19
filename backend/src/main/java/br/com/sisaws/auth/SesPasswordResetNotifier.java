package br.com.sisaws.auth;

import br.com.sisaws.user.AppUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.Body;
import software.amazon.awssdk.services.sesv2.model.Content;
import software.amazon.awssdk.services.sesv2.model.Destination;
import software.amazon.awssdk.services.sesv2.model.EmailContent;
import software.amazon.awssdk.services.sesv2.model.Message;
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest;
import software.amazon.awssdk.services.sesv2.model.SesV2Exception;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class SesPasswordResetNotifier implements PasswordResetNotifier {

    private static final Logger log = LoggerFactory.getLogger(SesPasswordResetNotifier.class);

    private final String fromEmail;
    private final String baseUrl;
    private final SesV2Client ses;

    public SesPasswordResetNotifier(
            @Value("${sisaws.security.password-reset.from-email:}") String fromEmail,
            @Value("${sisaws.security.password-reset.base-url:http://localhost:5173}") String baseUrl,
            @Value("${sisaws.security.password-reset.aws-region:sa-east-1}") String awsRegion) {
        this.fromEmail = fromEmail == null ? "" : fromEmail.trim();
        this.baseUrl = baseUrl;
        this.ses = SesV2Client.builder().region(Region.of(awsRegion)).build();
    }

    @Override
    public void sendResetLink(AppUser user, String rawToken) {
        if (fromEmail.isBlank()) {
            log.warn("Password reset email delivery is disabled because no SES sender is configured.");
            return;
        }

        String separator = baseUrl.contains("?") ? "&" : "?";
        String link = baseUrl + separator + "resetToken="
                + URLEncoder.encode(rawToken, StandardCharsets.UTF_8);

        String text = """
                Olá, %s.

                Recebemos uma solicitação para redefinir a senha da sua conta SisAWS.

                Use o link abaixo. Ele é temporário, de uso único e expira em poucos minutos:

                %s

                Se você não solicitou esta alteração, ignore esta mensagem.
                """.formatted(user.getName(), link);

        try {
            ses.sendEmail(SendEmailRequest.builder()
                    .fromEmailAddress(fromEmail)
                    .destination(Destination.builder().toAddresses(user.getEmail()).build())
                    .content(EmailContent.builder()
                            .simple(Message.builder()
                                    .subject(Content.builder().data("Redefinição de senha - SisAWS").charset("UTF-8").build())
                                    .body(Body.builder()
                                            .text(Content.builder().data(text).charset("UTF-8").build())
                                            .build())
                                    .build())
                            .build())
                    .build());
        } catch (SesV2Exception exception) {
            log.error("Amazon SES could not send a password reset email: {}", exception.getMessage());
        }
    }
}
