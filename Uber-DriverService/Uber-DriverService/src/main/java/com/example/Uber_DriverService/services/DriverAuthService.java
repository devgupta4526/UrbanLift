package com.example.Uber_DriverService.services;



import com.example.Uber_DriverService.dtos.*;
import com.example.Uber_DriverService.util.EmailNormalizer;
import com.example.Uber_EntityService.Models.*;
import com.example.Uber_DriverService.repositories.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DriverAuthService {
    private final DriverRepository driverRepository;
    private final CarRepository carRepository;
    private final ColorRepository colorRepository;
    private final PasswordEncoder passwordEncoder;

    public DriverAuthService(DriverRepository driverRepository, CarRepository carRepository,
                             ColorRepository colorRepository, PasswordEncoder passwordEncoder) {
        this.driverRepository = driverRepository;
        this.carRepository = carRepository;
        this.colorRepository = colorRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public DriverDto signUp(DriverSignUpRequestDto dto) {
        String email = EmailNormalizer.normalize(dto.getEmail());
        if (driverRepository.findByEmail(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered");
        }

        Driver driver = Driver.builder()
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .email(email)
                .phoneNumber(dto.getPhoneNumber())
                .password(passwordEncoder.encode(dto.getPassword()))
                .address(dto.getAddress())
                .licenseNumber(dto.getLicenseNumber())
                .aadharNumber(dto.getAadharNumber())
                .activeCity(dto.getActiveCity())
                .driverApprovalStatus(DriverApprovalStatus.PENDING)
                .isAvailable(false)
                .rating(0.0)
                .build();

        try {
            driver = driverRepository.save(driver);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email, license, or Aadhar already exists", ex);
        }

        if (dto.getCar() != null) {
            Color color = colorRepository.findByName(dto.getCar().getColorName())
                    .orElseGet(() -> colorRepository.save(Color.builder().name(dto.getCar().getColorName()).build()));
            Car car = Car.builder()
                    .plateNumber(dto.getCar().getPlateNumber())
                    .color(color)
                    .brand(dto.getCar().getBrand())
                    .model(dto.getCar().getModel())
                    .carType(dto.getCar().getCarType() != null ? CarType.valueOf(dto.getCar().getCarType()) : CarType.SEDAN)
                    .driver(driver)
                    .build();
            car = carRepository.save(car);
            driver.setCar(car);
            driver = driverRepository.save(driver);
        }

        return toDto(driver);
    }

    public DriverDto toDto(Driver driver) {
        CarDto carDto = null;
        if (driver.getCar() != null) {
            Car c = driver.getCar();
            carDto = CarDto.builder()
                    .plateNumber(c.getPlateNumber())
                    .colorName(c.getColor() != null ? c.getColor().getName() : null)
                    .brand(c.getBrand())
                    .model(c.getModel())
                    .carType(c.getCarType() != null ? c.getCarType().name() : null)
                    .build();
        }
        return DriverDto.builder()
                .id(driver.getId())
                .firstName(driver.getFirstName())
                .lastName(driver.getLastName())
                .email(driver.getEmail())
                .phoneNumber(driver.getPhoneNumber())
                .address(driver.getAddress())
                .licenseNumber(driver.getLicenseNumber())
                .driverApprovalStatus(driver.getDriverApprovalStatus() != null ? driver.getDriverApprovalStatus().name() : null)
                .activeCity(driver.getActiveCity())
                .isAvailable(driver.isAvailable())
                .rating(driver.getRating())
                .car(carDto)
                .build();
    }
}
