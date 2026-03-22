package com.example.Uber_DriverService.repositories;
import com.example.Uber_EntityService.Models.Color;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ColorRepository extends JpaRepository<Color, Long> {

    Optional<Color> findByName(String name);
}

