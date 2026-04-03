package com.example.market.repository.license;

import com.example.market.model.license.DeviceLicense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DeviceLicenseRepository extends JpaRepository<DeviceLicense, Long> {
    @Query("SELECT COUNT(dl) FROM DeviceLicense dl WHERE dl.license.id = :licenseId")
    long countByLicenseId(@Param("licenseId") Long licenseId);
}