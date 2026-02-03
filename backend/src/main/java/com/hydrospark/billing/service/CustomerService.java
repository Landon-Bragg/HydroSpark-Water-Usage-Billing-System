package com.hydrospark.billing.service;

import com.hydrospark.billing.dto.CustomerDTO;
import com.hydrospark.billing.model.Customer;
import com.hydrospark.billing.model.Meter;
import com.hydrospark.billing.model.User;
import com.hydrospark.billing.repository.CustomerRepository;
import com.hydrospark.billing.repository.MeterRepository;
import com.hydrospark.billing.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final MeterRepository meterRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    /**
     * Get all customers with pagination
     */
    public Page<Customer> getAllCustomers(Pageable pageable) {
        return customerRepository.findAll(pageable);
    }

    /**
     * Get customer by ID
     */
    public Customer getCustomerById(String customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found with ID: " + customerId));
    }

    /**
     * Get customer by email
     */
    public Optional<Customer> getCustomerByEmail(String email) {
        return customerRepository.findByEmail(email);
    }

    /**
     * Get customers by billing cycle
     */
    public List<Customer> getCustomersByBillingCycle(Integer cycleNumber) {
        return customerRepository.findByBillingCycleNumber(cycleNumber);
    }

    /**
     * Create a new customer
     */
    @Transactional
    public Customer createCustomer(CustomerDTO customerDTO) {
        log.info("Creating new customer: {}", customerDTO.getName());

        // Check if email already exists
        if (customerDTO.getEmail() != null && customerRepository.findByEmail(customerDTO.getEmail()).isPresent()) {
            throw new RuntimeException("Customer with email " + customerDTO.getEmail() + " already exists");
        }

        // Create customer
        Customer customer = Customer.builder()
                .name(customerDTO.getName())
                .customerType(Customer.CustomerType.valueOf(customerDTO.getCustomerType()))
                .email(customerDTO.getEmail())
                .phone(customerDTO.getPhone())
                .mailingAddressLine1(customerDTO.getMailingAddress())
                .mailingCity(customerDTO.getMailingCity())
                .mailingState(customerDTO.getMailingState())
                .mailingZip(customerDTO.getMailingZip())
                .billingCycleNumber(customerDTO.getBillingCycleNumber() != null 
                        ? customerDTO.getBillingCycleNumber() : 20)
                .build();

        customer = customerRepository.save(customer);

        // Create user account if email provided
        if (customerDTO.getEmail() != null && !customerDTO.getEmail().isEmpty()) {
            createUserAccount(customer);
        }

        log.info("Customer created successfully: {}", customer.getId());
        return customer;
    }

    /**
     * Update an existing customer
     */
    @Transactional
    public Customer updateCustomer(String customerId, CustomerDTO customerDTO) {
        log.info("Updating customer: {}", customerId);

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        // Update fields
        if (customerDTO.getName() != null) {
            customer.setName(customerDTO.getName());
        }
        if (customerDTO.getCustomerType() != null) {
            customer.setCustomerType(Customer.CustomerType.valueOf(customerDTO.getCustomerType()));
        }
        if (customerDTO.getEmail() != null) {
            // Check if new email already exists
            Optional<Customer> existing = customerRepository.findByEmail(customerDTO.getEmail());
            if (existing.isPresent() && !existing.get().getId().equals(customerId)) {
                throw new RuntimeException("Email already in use by another customer");
            }
            customer.setEmail(customerDTO.getEmail());
        }
        if (customerDTO.getPhone() != null) {
            customer.setPhone(customerDTO.getPhone());
        }
        if (customerDTO.getMailingAddress() != null) {
            customer.setMailingAddressLine1(customerDTO.getMailingAddress());
        }
        if (customerDTO.getMailingCity() != null) {
            customer.setMailingCity(customerDTO.getMailingCity());
        }
        if (customerDTO.getMailingState() != null) {
            customer.setMailingState(customerDTO.getMailingState());
        }
        if (customerDTO.getMailingZip() != null) {
            customer.setMailingZip(customerDTO.getMailingZip());
        }
        if (customerDTO.getBillingCycleNumber() != null) {
            customer.setBillingCycleNumber(customerDTO.getBillingCycleNumber());
        }

        customer = customerRepository.save(customer);

        log.info("Customer updated successfully: {}", customerId);
        return customer;
    }

    /**
     * Delete a customer
     */
    @Transactional
    public void deleteCustomer(String customerId) {
        log.info("Deleting customer: {}", customerId);

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        // Check if customer has active meters
        List<Meter> meters = meterRepository.findByCustomerId(customerId);
        if (!meters.isEmpty()) {
            throw new RuntimeException("Cannot delete customer with active meters. " +
                    "Please deactivate all meters first.");
        }

        customerRepository.delete(customer);

        log.info("Customer deleted successfully: {}", customerId);
    }

    /**
     * Get all meters for a customer
     */
    public List<Meter> getCustomerMeters(String customerId) {
        // Verify customer exists
        customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        return meterRepository.findByCustomerId(customerId);
    }

    /**
     * Add a meter to a customer
     */
    @Transactional
    public Meter addMeter(String customerId, String externalLocationId, String serviceAddress) {
        log.info("Adding meter {} to customer {}", externalLocationId, customerId);

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        // Check if meter already exists
        if (meterRepository.findByExternalLocationId(externalLocationId).isPresent()) {
            throw new RuntimeException("Meter with location ID " + externalLocationId + " already exists");
        }

        Meter meter = Meter.builder()
                .customerId(customer.getId())
                .externalLocationId(externalLocationId)
                .serviceAddressLine1(serviceAddress)
                .status(Meter.Status.ACTIVE)
                .build();

        meter = meterRepository.save(meter);

        log.info("Meter added successfully: {}", meter.getId());
        return meter;
    }

    /**
     * Deactivate a meter
     */
    @Transactional
    public void deactivateMeter(String meterId) {
        log.info("Deactivating meter: {}", meterId);

        Meter meter = meterRepository.findById(meterId)
                .orElseThrow(() -> new RuntimeException("Meter not found"));

        meter.setStatus(Meter.Status.INACTIVE);
        meterRepository.save(meter);

        log.info("Meter deactivated successfully: {}", meterId);
    }

    /**
     * Create user account for customer
     */
    private void createUserAccount(Customer customer) {
        // Check if user already exists
        if (userRepository.findByEmail(customer.getEmail()).isPresent()) {
            log.info("User account already exists for email: {}", customer.getEmail());
            return;
        }

        // Generate temporary password
        String tempPassword = generateTemporaryPassword();

        User user = User.builder()
                .email(customer.getEmail())
                .passwordHash(passwordEncoder.encode(tempPassword))
                .role(User.Role.CUSTOMER)
                .customerId(customer.getId())
                .isActive(true)
                .failedLoginAttempts(0)
                .build();

        userRepository.save(user);

        // Send welcome email with temporary password
        emailService.sendWelcomeEmail(customer.getEmail(), customer.getName(), tempPassword);

        log.info("User account created for customer: {}", customer.getId());
    }

    /**
     * Convert Customer entity to DTO
     */
    public CustomerDTO toDTO(Customer customer) {
        return CustomerDTO.builder()
                .id(customer.getId())
                .name(customer.getName())
                .customerType(customer.getCustomerType().name())
                .email(customer.getEmail())
                .phone(customer.getPhone())
                .mailingAddress(customer.getMailingAddressLine1())
                .mailingCity(customer.getMailingCity())
                .mailingState(customer.getMailingState())
                .mailingZip(customer.getMailingZip())
                .billingCycleNumber(customer.getBillingCycleNumber())
                .createdAt(customer.getCreatedAt())
                .build();
    }

    /**
     * Search customers by name or email
     */
    public List<Customer> searchCustomers(String searchTerm) {
        // Simple search - can be enhanced with more sophisticated query
        return customerRepository.findAll().stream()
                .filter(c -> 
                    (c.getName() != null && c.getName().toLowerCase().contains(searchTerm.toLowerCase())) ||
                    (c.getEmail() != null && c.getEmail().toLowerCase().contains(searchTerm.toLowerCase()))
                )
                .toList();
    }

    /**
     * Get customer statistics
     */
    public CustomerStatistics getCustomerStatistics(String customerId) {
        Customer customer = getCustomerById(customerId);
        List<Meter> meters = meterRepository.findByCustomerId(customerId);

        int activeMeters = (int) meters.stream()
                .filter(m -> m.getStatus() == Meter.Status.ACTIVE)
                .count();

        return new CustomerStatistics(
                customer.getId(),
                customer.getName(),
                meters.size(),
                activeMeters
        );
    }

    private String generateTemporaryPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 12; i++) {
            int index = (int) (Math.random() * chars.length());
            sb.append(chars.charAt(index));
        }
        return sb.toString();
    }

    // Inner class for statistics
    public record CustomerStatistics(
            String customerId,
            String customerName,
            int totalMeters,
            int activeMeters
    ) {}
}
