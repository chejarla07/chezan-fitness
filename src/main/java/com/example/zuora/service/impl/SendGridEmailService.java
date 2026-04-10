package com.example.zuora.service.impl;

import com.example.zuora.service.EmailService;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Year;

/**
 * SendGrid implementation of the EmailService.
 */
@Service
public class SendGridEmailService implements EmailService {

    private static final Logger logger = LoggerFactory.getLogger(SendGridEmailService.class);

    private final SendGrid sendGrid;
    private final String baseUrl;
    private final String appName;

    @Value("${sendgrid.from-email:noreply@chezanfitness.com}")
    private String fromEmail;

    public SendGridEmailService(SendGrid sendGrid,
                                @Value("${app.base-url:http://localhost:8080}") String baseUrl,
                                @Value("${spring.application.name:Chezan Fitness}") String appName) {
        this.sendGrid = sendGrid;
        this.baseUrl = baseUrl;
        this.appName = appName;
    }

    @Override
    public void sendVerificationEmail(String to, String token) {
        String verificationLink = baseUrl + "/verify-email?token=" + token;
        String subject = "Verify Your Email Address";
        String content = buildVerificationEmailContent(verificationLink);
        sendEmail(to, subject, content);
    }

    @Override
    public void sendPasswordResetEmail(String to, String token) {
        String resetLink = baseUrl + "/reset-password?token=" + token;
        String subject = "Reset Your Password";
        String content = buildPasswordResetEmailContent(resetLink);
        sendEmail(to, subject, content);
    }

    @Override
    public void sendWelcomeEmail(String to, String accountNumber) {
        String subject = "Welcome to " + appName + "!";
        String content = buildWelcomeEmailContent(accountNumber);
        sendEmail(to, subject, content);
    }

    @Override
    public void sendTemporaryPasswordEmail(String to, String temporaryPassword, String resetLink) {
        String subject = "Your " + appName + " Account Has Been Created";
        String content = buildTemporaryPasswordEmailContent(temporaryPassword, resetLink);
        sendEmail(to, subject, content);
    }

    private void sendEmail(String to, String subject, String htmlContent) {
        Email from = new Email(fromEmail, appName);
        Email toEmail = new Email(to);
        Content content = new Content("text/html", htmlContent);
        Mail mail = new Mail(from, subject, toEmail, content);

        try {
            Request request = new Request();
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            Response response = sendGrid.api(request);

            if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
                logger.info("Email sent successfully to: {}", to);
            } else {
                logger.warn("Email send returned status {}: {}", response.getStatusCode(), response.getBody());
            }
        } catch (IOException e) {
            logger.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }

    private String buildVerificationEmailContent(String verificationLink) {
        return "<!DOCTYPE html>" +
            "<html><head><style>" +
            "body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }" +
            ".container { max-width: 600px; margin: 0 auto; padding: 20px; }" +
            ".button { display: inline-block; padding: 12px 24px; background-color: #3b82f6; color: white; text-decoration: none; border-radius: 8px; font-weight: 500; }" +
            ".footer { margin-top: 30px; padding-top: 20px; border-top: 1px solid #eee; font-size: 12px; color: #666; }" +
            "</style></head><body>" +
            "<div class=\"container\">" +
            "<h2>Welcome to " + appName + "!</h2>" +
            "<p>Thank you for signing up! Please verify your email address by clicking the button below:</p>" +
            "<p><a href=\"" + verificationLink + "\" class=\"button\">Verify Email Address</a></p>" +
            "<p>Or copy and paste this link into your browser:</p>" +
            "<p style=\"word-break: break-all;\">" + verificationLink + "</p>" +
            "<p>This link will expire in 24 hours.</p>" +
            "<div class=\"footer\">" +
            "<p>If you didn't create an account with us, please ignore this email.</p>" +
            "<p>&copy; " + Year.now() + " " + appName + ". All rights reserved.</p>" +
            "</div></div></body></html>";
    }

    private String buildPasswordResetEmailContent(String resetLink) {
        return "<!DOCTYPE html>" +
            "<html><head><style>" +
            "body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }" +
            ".container { max-width: 600px; margin: 0 auto; padding: 20px; }" +
            ".button { display: inline-block; padding: 12px 24px; background-color: #3b82f6; color: white; text-decoration: none; border-radius: 8px; font-weight: 500; }" +
            ".footer { margin-top: 30px; padding-top: 20px; border-top: 1px solid #eee; font-size: 12px; color: #666; }" +
            "</style></head><body>" +
            "<div class=\"container\">" +
            "<h2>Reset Your Password</h2>" +
            "<p>We received a request to reset your password. Click the button below to create a new password:</p>" +
            "<p><a href=\"" + resetLink + "\" class=\"button\">Reset Password</a></p>" +
            "<p>Or copy and paste this link into your browser:</p>" +
            "<p style=\"word-break: break-all;\">" + resetLink + "</p>" +
            "<p>This link will expire in 1 hour.</p>" +
            "<div class=\"footer\">" +
            "<p>If you didn't request a password reset, please ignore this email.</p>" +
            "<p>&copy; " + Year.now() + " " + appName + ". All rights reserved.</p>" +
            "</div></div></body></html>";
    }

    private String buildWelcomeEmailContent(String accountNumber) {
        return "<!DOCTYPE html>" +
            "<html><head><style>" +
            "body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }" +
            ".container { max-width: 600px; margin: 0 auto; padding: 20px; }" +
            ".button { display: inline-block; padding: 12px 24px; background-color: #3b82f6; color: white; text-decoration: none; border-radius: 8px; font-weight: 500; }" +
            ".footer { margin-top: 30px; padding-top: 20px; border-top: 1px solid #eee; font-size: 12px; color: #666; }" +
            "</style></head><body>" +
            "<div class=\"container\">" +
            "<h2>Welcome to " + appName + "!</h2>" +
            "<p>Thank you for joining our fitness community. Your account has been successfully created.</p>" +
            "<p><strong>Your Account Number:</strong> " + accountNumber + "</p>" +
            "<p>You can now access all our facilities and services. To get started:</p>" +
            "<ul>" +
            "<li>Log in to your account at <a href=\"" + baseUrl + "\">" + baseUrl + "</a></li>" +
            "<li>Complete your profile</li>" +
            "<li>Browse our membership plans and services</li>" +
            "</ul>" +
            "<p><a href=\"" + baseUrl + "/member/dashboard\" class=\"button\">Go to Dashboard</a></p>" +
            "<div class=\"footer\">" +
            "<p>If you have any questions, please don't hesitate to contact our support team.</p>" +
            "<p>&copy; " + Year.now() + " " + appName + ". All rights reserved.</p>" +
            "</div></div></body></html>";
    }

    private String buildTemporaryPasswordEmailContent(String temporaryPassword, String resetLink) {
        return "<!DOCTYPE html>" +
            "<html><head><style>" +
            "body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }" +
            ".container { max-width: 600px; margin: 0 auto; padding: 20px; }" +
            ".button { display: inline-block; padding: 12px 24px; background-color: #3b82f6; color: white; text-decoration: none; border-radius: 8px; font-weight: 500; }" +
            ".password-box { background-color: #f8fafc; padding: 15px; border-radius: 8px; font-family: monospace; font-size: 18px; letter-spacing: 2px; }" +
            ".footer { margin-top: 30px; padding-top: 20px; border-top: 1px solid #eee; font-size: 12px; color: #666; }" +
            "</style></head><body>" +
            "<div class=\"container\">" +
            "<h2>Your " + appName + " Account</h2>" +
            "<p>An account has been created for you at " + appName + ". Here are your login credentials:</p>" +
            "<p><strong>Temporary Password:</strong></p>" +
            "<div class=\"password-box\">" + temporaryPassword + "</div>" +
            "<p><strong>Important:</strong> Please change your password immediately after logging in.</p>" +
            "<p><a href=\"" + resetLink + "\" class=\"button\">Set Your Password</a></p>" +
            "<p>Or log in at: <a href=\"" + baseUrl + "/login\">" + baseUrl + "/login</a></p>" +
            "<div class=\"footer\">" +
            "<p>For security reasons, this temporary password will expire in 24 hours.</p>" +
            "<p>&copy; " + Year.now() + " " + appName + ". All rights reserved.</p>" +
            "</div></div></body></html>";
    }
}