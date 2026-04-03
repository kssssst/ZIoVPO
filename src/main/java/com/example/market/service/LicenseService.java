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
                          UserRepository userRepository, SigningService signingService) {
        this.licenseRepository = licenseRepository;
        this.productRepository = productRepository;
        this.licenseTypeRepository = licenseTypeRepository;
        this.deviceRepository = deviceRepository;
        this.deviceLicenseRepository = deviceLicenseRepository;
        this.licenseHistoryRepository = licenseHistoryRepository;
        this.userRepository = userRepository;
        this.signingService = signingService;
    }

    // Вспомогательный метод – получить текущего аутентифицированного пользователя
    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Current user not found"));
    }

    // Вспомогательный метод – получить ID текущего пользователя
    public Long getCurrentUserId() {
        return getCurrentUser().getId();
    }

    // Генерация уникального активационного кода
    private String generateActivationCode() {
        return "LIC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    // Проверка лицензии (только администратор)
    @Transactional
    public LicenseResponse createLicense(CreateLicenseRequest request, Long adminId) {
        // Проверка существования продукта
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        //Проверка существования типа лицензии
        LicenseType type = licenseTypeRepository.findById(request.getTypeId())
                .orElseThrow(() -> new RuntimeException("License type not found"));

        //Проверка существования владельца (owner)
        User owner = userRepository.findById(request.getOwnerId())
                .orElseThrow(() -> new RuntimeException("Owner user not found"));

        //Создание лицензии
        License license = new License();
        license.setCode(generateActivationCode());
        license.setProduct(product);
        license.setType(type);
        license.setOwner(owner);
        license.setUser(null);                 // ещё не активирована
        license.setBlocked(false);
        license.setDeviceCount(request.getDeviceCount() != null ? request.getDeviceCount() : 1);
        license.setDescription(request.getDescription());

        License savedLicense = licenseRepository.save(license);

        //Запись в историю (кто создал – администратор)
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin not found"));
        LicenseHistory history = new LicenseHistory(savedLicense, admin, "CREATED", "License created");
        licenseHistoryRepository.save(history);

        //Формирование ответа
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

    // Активация лицензии
    @Transactional
    public TicketResponse activateLicense(ActivateLicenseRequest request) {
        User currentUser = getCurrentUser();

        //Поиск лицензии по коду
        License license = licenseRepository.findByCode(request.getActivationKey())
                .orElseThrow(() -> new RuntimeException("License not found"));

        //Проверка, что лицензия не принадлежит другому пользователю
        if (license.getUser() != null && !license.getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException("License owned by another user");
        }

        //Поиск или создание устройства
        Device device = deviceRepository.findByMacAddress(request.getDeviceMac())
                .orElseGet(() -> {
                    Device newDevice = new Device(request.getDeviceName(), request.getDeviceMac(), currentUser);
                    return deviceRepository.save(newDevice);
                });

        boolean firstActivation = (license.getUser() == null);

        if (firstActivation) {
            // Первая активация – заполняем даты
            license.setUser(currentUser);
            license.setFirstActivationDate(LocalDate.now());
            LocalDate endingDate = LocalDate.now().plusDays(license.getType().getDefaultDurationInDays());
            license.setEndingDate(endingDate);
            licenseRepository.save(license);

            // Связь с устройством
            DeviceLicense dl = new DeviceLicense(license, device);
            deviceLicenseRepository.save(dl);

            // История
            LicenseHistory history = new LicenseHistory(license, currentUser, "ACTIVATED", "First activation");
            licenseHistoryRepository.save(history);
        } else {
            // Повторная активация – проверка лимита устройств
            long activeDevicesCount = deviceLicenseRepository.countByLicenseId(license.getId());
            if (activeDevicesCount >= license.getDeviceCount()) {
                throw new RuntimeException("Device limit reached");
            }
            DeviceLicense dl = new DeviceLicense(license, device);
            deviceLicenseRepository.save(dl);

            LicenseHistory history = new LicenseHistory(license, currentUser, "ACTIVATED", "Additional device activation");
            licenseHistoryRepository.save(history);
        }

        // Формирование тикета
        Ticket ticket = new Ticket(
                LocalDateTime.now(),
                60000L,   // TTL 60 секунд (можно вынести в конфиг)
                license.getFirstActivationDate(),
                license.getEndingDate(),
                currentUser.getId(),
                device.getMacAddress(),
                license.getBlocked()
        );
        return new TicketResponse(ticket);
    }

    // Обновлене лецензи
    @Transactional
    public TicketResponse renewLicense(RenewLicenseRequest request) {
        User currentUser = getCurrentUser();

        License license = licenseRepository.findByCode(request.getActivationKey())
                .orElseThrow(() -> new RuntimeException("License not found"));

        // Проверка, что лицензия принадлежит текущему пользователю
        if (license.getUser() == null || !license.getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException("License not activated for this user");
        }

        // Проверка возможности продления (истекает через 7 дней или уже истекла)
        if (license.getEndingDate() != null && license.getEndingDate().isAfter(LocalDate.now().plusDays(7))) {
            throw new RuntimeException("Renewal not allowed – license expires later than 7 days");
        }

        // Продление: добавляем срок из типа лицензии
        int daysToAdd = license.getType().getDefaultDurationInDays();
        LocalDate newEndingDate = (license.getEndingDate() != null && license.getEndingDate().isAfter(LocalDate.now()))
                ? license.getEndingDate().plusDays(daysToAdd)
                : LocalDate.now().plusDays(daysToAdd);
        license.setEndingDate(newEndingDate);
        licenseRepository.save(license);

        // История
        LicenseHistory history = new LicenseHistory(license, currentUser, "RENEWED", "License renewed");
        licenseHistoryRepository.save(history);

        // Тикет
        Ticket ticket = new Ticket(
                LocalDateTime.now(),
                60000L,
                license.getFirstActivationDate(),
                license.getEndingDate(),
                currentUser.getId(),
                null,
                license.getBlocked()
        );
        return new TicketResponse(ticket);
    }

    // проверка лицензии
    public TicketResponse checkLicense(CheckLicenseRequest request) {
        User currentUser = getCurrentUser();

        // Поиск устройства
        Device device = deviceRepository.findByMacAddress(request.getDeviceMac())
                .orElseThrow(() -> new RuntimeException("Device not found"));

        // Поиск активной лицензии
        License license = licenseRepository.findActiveByDeviceMacAndUserAndProduct(
                        device.getMacAddress(), currentUser, request.getProductId())
                .orElseThrow(() -> new RuntimeException("Active license not found"));

        // Тикет
        Ticket ticket = new Ticket(
                LocalDateTime.now(),
                60000L,
                license.getFirstActivationDate(),
                license.getEndingDate(),
                currentUser.getId(),
                device.getMacAddress(),
                license.getBlocked()
        );
        return new TicketResponse(ticket);
    }
}