package com.example.market.repository.license;

import com.example.market.model.license.License;
import com.example.market.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface LicenseRepository extends JpaRepository<License, Long> {
    Optional<License> findByCode(String code);

    @Query("SELECT l FROM License l " +
            "JOIN DeviceLicense dl ON dl.license = l " +
            "WHERE dl.device.macAddress = :macAddress " +
            "AND l.user = :user " +
            "AND l.product.id = :productId " +
            "AND l.blocked = false " +
            "AND l.endingDate >= CURRENT_DATE")
    Optional<License> findActiveByDeviceMacAndUserAndProduct(@Param("macAddress") String macAddress,
                                                             @Param("user") User user,
                                                             @Param("productId") Long productId);
}
