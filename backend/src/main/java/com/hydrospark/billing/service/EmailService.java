package com.hydrospark.billing.service;

import com.hydrospark.billing.model.Bill;
import com.hydrospark.billing.model.Customer;
import com.hydrospark.billing.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final CustomerRepository customerRepository;

    @Value("${spring.mail.username:noreply@hydrospark.com}")
    private String fromEmail;

    @Value("${app.name:HydroSpark Water Utility}")
    private String appName;

    /**
     * Send bill notification email to customer
     */
    public void sendBillEmail(Bill bill) {
        Customer customer = customerRepository.findById(bill.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        if (customer.getEmail() == null || customer.getEmail().isEmpty()) {
            log.warn("Cannot send bill email - customer {} has no email address", customer.getId());
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(customer.getEmail());
            helper.setSubject("Your Water Bill is Ready - " + formatCurrency(bill.getTotalAmount()));

            String emailBody = buildBillEmailBody(bill, customer);
            helper.setText(emailBody, true); // true = HTML

            mailSender.send(message);

            log.info("Bill email sent successfully to {} for bill {}", customer.getEmail(), bill.getId());

        } catch (MessagingException e) {
            log.error("Failed to send bill email to {}: {}", customer.getEmail(), e.getMessage(), e);
            throw new RuntimeException("Failed to send bill email: " + e.getMessage());
        }
    }

    /**
     * Send welcome email to new customer
     */
    public void sendWelcomeEmail(String email, String customerName, String temporaryPassword) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(email);
            helper.setSubject("Welcome to " + appName);

            String emailBody = buildWelcomeEmailBody(customerName, email, temporaryPassword);
            helper.setText(emailBody, true);

            mailSender.send(message);

            log.info("Welcome email sent to {}", email);

        } catch (MessagingException e) {
            log.error("Failed to send welcome email to {}: {}", email, e.getMessage(), e);
        }
    }

    /**
     * Send password reset email
     */
    public void sendPasswordResetEmail(String email, String resetToken) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(email);
            helper.setSubject("Password Reset Request - " + appName);

            String emailBody = buildPasswordResetEmailBody(resetToken);
            helper.setText(emailBody, true);

            mailSender.send(message);

            log.info("Password reset email sent to {}", email);

        } catch (MessagingException e) {
            log.error("Failed to send password reset email to {}: {}", email, e.getMessage(), e);
        }
    }

    /**
     * Send anomaly alert email to customer
     */
    public void sendAnomalyAlertEmail(String email, String customerName, String anomalyDescription) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(email);
            helper.setSubject("Water Usage Alert - " + appName);

            String emailBody = buildAnomalyAlertEmailBody(customerName, anomalyDescription);
            helper.setText(emailBody, true);

            mailSender.send(message);

            log.info("Anomaly alert email sent to {}", email);

        } catch (MessagingException e) {
            log.error("Failed to send anomaly alert email to {}: {}", email, e.getMessage(), e);
        }
    }

    /**
     * Send simple text email
     */
    public void sendSimpleEmail(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);

            mailSender.send(message);

            log.info("Simple email sent to {}: {}", to, subject);

        } catch (Exception e) {
            log.error("Failed to send simple email to {}: {}", to, e.getMessage(), e);
        }
    }

    private String buildBillEmailBody(Bill bill, Customer customer) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy");

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #1976d2; color: white; padding: 20px; text-align: center; }
                    .content { padding: 20px; background-color: #f9f9f9; }
                    .bill-summary { background-color: white; padding: 15px; margin: 20px 0; border-left: 4px solid #1976d2; }
                    .amount { font-size: 32px; font-weight: bold; color: #1976d2; }
                    .footer { text-align: center; padding: 20px; font-size: 12px; color: #666; }
                    .button { display: inline-block; padding: 12px 24px; background-color: #1976d2; color: white; text-decoration: none; border-radius: 4px; margin: 10px 0; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>%s</h1>
                        <p>Your Water Bill is Ready</p>
                    </div>
                    
                    <div class="content">
                        <p>Dear %s,</p>
                        
                        <p>Your water bill for the period ending <strong>%s</strong> is now available.</p>
                        
                        <div class="bill-summary">
                            <p style="margin: 0; color: #666;">Amount Due</p>
                            <p class="amount">%s</p>
                            <p style="margin: 0; color: #666;">Due Date: <strong>%s</strong></p>
                        </div>
                        
                        <p><strong>Bill Details:</strong></p>
                        <ul>
                            <li>Usage: <strong>%s CCF</strong></li>
                            <li>Base Charge: %s</li>
                            <li>Usage Charge: %s</li>
                            <li>Surcharges: %s</li>
                        </ul>
                        
                        <p style="text-align: center;">
                            <a href="#" class="button">View Full Bill</a>
                        </p>
                        
                        <p>Thank you for being a valued customer!</p>
                    </div>
                    
                    <div class="footer">
                        <p>%s | Customer Service: 1-800-HYDROSPARK</p>
                        <p>This is an automated message. Please do not reply to this email.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(
                appName,
                customer.getName(),
                bill.getDueDate().format(dateFormatter),
                formatCurrency(bill.getTotalAmount()),
                bill.getDueDate().format(dateFormatter),
                bill.getSubtotal() != null ? bill.getSubtotal().toString() : "0",
                formatCurrency(BigDecimal.ZERO), // Base charge
                formatCurrency(bill.getSubtotal() != null ? bill.getSubtotal() : BigDecimal.ZERO),
                formatCurrency(bill.getTotalSurcharges() != null ? bill.getTotalSurcharges() : BigDecimal.ZERO),
                appName
            );
    }

    private String buildWelcomeEmailBody(String customerName, String email, String temporaryPassword) {
        return """
            <!DOCTYPE html>
            <html>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                    <h2 style="color: #1976d2;">Welcome to %s!</h2>
                    
                    <p>Dear %s,</p>
                    
                    <p>Your customer account has been created successfully. You can now access your water usage information, view bills, and manage your account online.</p>
                    
                    <p><strong>Login Credentials:</strong></p>
                    <ul>
                        <li>Email: %s</li>
                        <li>Temporary Password: <strong>%s</strong></li>
                    </ul>
                    
                    <p style="color: #d32f2f;"><strong>Important:</strong> Please change your password after your first login.</p>
                    
                    <p>Visit our customer portal to get started: <a href="#">Customer Portal</a></p>
                    
                    <p>If you have any questions, please contact our customer service team.</p>
                    
                    <p>Best regards,<br>The %s Team</p>
                </div>
            </body>
            </html>
            """.formatted(appName, customerName, email, temporaryPassword, appName);
    }

    private String buildPasswordResetEmailBody(String resetToken) {
        return """
            <!DOCTYPE html>
            <html>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                    <h2 style="color: #1976d2;">Password Reset Request</h2>
                    
                    <p>We received a request to reset your password.</p>
                    
                    <p>Your temporary password is: <strong>%s</strong></p>
                    
                    <p style="color: #d32f2f;"><strong>Important:</strong> Please change your password immediately after logging in.</p>
                    
                    <p>If you did not request this password reset, please contact customer service immediately.</p>
                    
                    <p>This temporary password will expire in 24 hours.</p>
                    
                    <p>Best regards,<br>The %s Team</p>
                </div>
            </body>
            </html>
            """.formatted(resetToken, appName);
    }

    private String buildAnomalyAlertEmailBody(String customerName, String anomalyDescription) {
        return """
            <!DOCTYPE html>
            <html>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                    <h2 style="color: #d32f2f;">Water Usage Alert</h2>
                    
                    <p>Dear %s,</p>
                    
                    <p>We've detected unusual water usage activity on your account:</p>
                    
                    <div style="background-color: #fff3cd; border-left: 4px solid #ffc107; padding: 15px; margin: 20px 0;">
                        <p style="margin: 0;"><strong>Alert:</strong> %s</p>
                    </div>
                    
                    <p>This could indicate:</p>
                    <ul>
                        <li>A possible leak in your plumbing</li>
                        <li>Unusually high water consumption</li>
                        <li>A meter reading issue</li>
                    </ul>
                    
                    <p>We recommend checking your property for any visible leaks or unusual water usage.</p>
                    
                    <p>If you need assistance, please contact our customer service team.</p>
                    
                    <p>Best regards,<br>The %s Team</p>
                </div>
            </body>
            </html>
            """.formatted(customerName, anomalyDescription, appName);
    }

    private String formatCurrency(BigDecimal amount) {
        return String.format("$%.2f", amount);
    }
}
