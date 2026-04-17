package com.example.market.service;

import com.example.market.signature.SigningService;
import com.example.market.dto.license.*;
import com.example.market.model.User;
import com.example.market.model.license.*;
import com.example.market.repository.UserRepository;
import com.example.market.repository.license.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class LicenseService {

    private final LicenseRepository licenseRepository;
    private final ProductRepository productRepository;
    private final LicenseTypeRepository licenseTypeRepository;
    private final DeviceRepository deviceRepository;
    private final DeviceLicenseRepository deviceLicenseRepository;
    private final LicenseHistoryRepository licenseHistoryRepository;
    private final UserRepository userRepository;
    private final SigningService signingService;

    public LicenseService(LicenseRepository licenseRepository,
                          ProductRepository productRepository,
                          LicenseTypeRepository licenseTypeRepository,
                          DeviceRepository deviceRepository,
                          DeviceLicenseRepository deviceLicenseRepository,
                          LicenseHistoryRepository licenseHistoryRepository,
                          UserRepository userRepository,
                          SigningService signingService) {
        this.licenseRepository = licenseRepository;
        this.productRepository = productRepository;
        this.licenseTypeRepository = licenseTypeRepository;
        this.deviceRepository = deviceRepository;
        this.deviceLicenseRepository = deviceLicenseRepository;
        this.licenseHistoryRepository = licenseHistoryRepository;
        this.userRepository = userRepository;
        this.signingService = signingService;
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Current user not found"));
    }

    public Long getCurrentUserId() {
        return getCurrentUser().getId();
    }

    private String generateActivationCode() {
        return "LIC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    @Transactional
    public LicenseResponse createLicense(CreateLicenseRequest request, Long adminId) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));
        LicenseType type = licenseTypeRepository.findById(request.getTypeId())
                .orElseThrow(() -> new RuntimeException("License type not found"));
        User owner = userRepository.findById(request.getOwnerId())
                .orElseThrow(() -> new RuntimeException("Owner user not found"));

        License license = new License();
        license.setCode(generateActivationCode());
        license.setProduct(product);
        license.setType(type);
        license.setOwner(owner);
        license.setUser(null);
        license.setBlocked(false);
        license.setDeviceCount(request.getDeviceCount() != null ? request.getDeviceCount() : 1);
        license.setDescription(request.getDescription());

        License savedLicense = licenseRepository.save(license);

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin not found"));
        LicenseHistory history = new LicenseHistory(savedLicense, admin, "CREATED", "License created");
        licenseHistoryRepository.save(history);

        return new LicenseResponse(
                savedLicense.getId(), savedLicense.getCode(),
                product.getId(), product.getName(),
                type.getId(), type.getName(),
                owner.getId(), owner.getEmail(),
                null, null,
                null, null,
                savedLicense.getBlocked(), savedLicense.getDeviceCount()
        );
    }

    @Transactional
    public TicketResponse activateLicense(ActivateLicenseRequest request) {
        User currentUser = getCurrentUser();

        License license = licenseRepository.findByCode(request.getActivationKey())
                .orElseThrow(() -> new RuntimeException("License not found"));

        if (license.getUser() != null && !license.getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException("License owned by another user");
        }

        Device device = deviceRepository.findByMacAddress(request.getDeviceMac())
                .orElseGet(() -> {
                    Device newDevice = new Device(request.getDeviceName(), request.getDeviceMac(), currentUser);
                    return deviceRepository.save(newDevice);
                });

        boolean firstActivation = (license.getUser() == null);

        if (firstActivation) {
            license.setUser(currentUser);
            license.setFirstActivationDate(LocalDate.now());
            LocalDate endingDate = LocalDate.now().plusDays(license.getType().getDefaultDurationInDays());
            license.setEndingDate(endingDate);
            licenseRepository.save(license);

            DeviceLicense dl = new DeviceLicense(license, device);
            deviceLicenseRepository.save(dl);

            LicenseHistory history = new LicenseHistory(license, currentUser, "ACTIVATED", "First activation");
            licenseHistoryRepository.save(history);
        } else {
            long activeDevicesCount = deviceLicenseRepository.countByLicenseId(license.getId());
            if (activeDevicesCount >= license.getDeviceCount()) {
                throw new RuntimeException("Device limit reached");
            }
            DeviceLicense dl = new DeviceLicense(license, device);
            deviceLicenseRepository.save(dl);

            LicenseHistory history = new LicenseHistory(license, currentUser, "ACTIVATED", "Additional device activation");
            licenseHistoryRepository.save(history);
        }

        Ticket ticket = new Ticket(
                LocalDateTime.now(),
                60000L,
                license.getFirstActivationDate(),
                license.getEndingDate(),
                currentUser.getId(),
                device.getMacAddress(),
                license.getBlocked()
        );
        String signature = signingService.sign(ticket);
        return new TicketResponse(ticket, signature);
    }

    @Transactional
    public TicketResponse renewLicense(RenewLicenseRequest request) {
        User currentUser = getCurrentUser();

        License license = licenseRepository.findByCode(request.getActivationKey())
                .orElseThrow(() -> new RuntimeException("License not found"));

        if (license.getUser() == null || !license.getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException("License not activated for this user");
        }

        if (license.getEndingDate() != null && license.getEndingDate().isAfter(LocalDate.now().plusDays(7))) {
            throw new RuntimeException("Renewal not allowed – license expires later than 7 days");
        }

        int daysToAdd = license.getType().getDefaultDurationInDays();
        LocalDate newEndingDate = (license.getEndingDate() != null && license.getEndingDate().isAfter(LocalDate.now()))
                ? license.getEndingDate().plusDays(daysToAdd)
                : LocalDate.now().plusDays(daysToAdd);
        license.setEndingDate(newEndingDate);
        licenseRepository.save(license);

        LicenseHistory history = new LicenseHistory(license, currentUser, "RENEWED", "License renewed");
        licenseHistoryRepository.save(history);

        Ticket ticket = new Ticket(
                LocalDateTime.now(),
                60000L,
                license.getFirstActivationDate(),
                license.getEndingDate(),
                currentUser.getId(),
                null,
                license.getBlocked()
        );
        String signature = signingService.sign(ticket);
        return new TicketResponse(ticket, signature);
    }

    public TicketResponse checkLicense(CheckLicenseRequest request) {
        User currentUser = getCurrentUser();

        Device device = deviceRepository.findByMacAddress(request.getDeviceMac())
                .orElseThrow(() -> new RuntimeException("Device not found"));

        License license = licenseRepository.findActiveByDeviceMacAndUserAndProduct(
                        device.getMacAddress(), currentUser, request.getProductId())
                .orElseThrow(() -> new RuntimeException("Active license not found"));

        Ticket ticket = new Ticket(
                LocalDateTime.now(),
                60000L,
                license.getFirstActivationDate(),
                license.getEndingDate(),
                currentUser.getId(),
                device.getMacAddress(),
                license.getBlocked()
        );
        String signature = signingService.sign(ticket);
        return new TicketResponse(ticket, signature);
    }
}