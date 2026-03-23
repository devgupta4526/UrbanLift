package com.example.Uber_AuthService.services;

import com.example.Uber_AuthService.helpers.AuthPassengerDetails;
import com.example.Uber_AuthService.repositories.PassengerRepository;
import com.example.Uber_AuthService.util.EmailNormalizer;
import com.example.Uber_EntityService.Models.Passenger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;


@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private  PassengerRepository passengerRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Optional<Passenger> passenger = passengerRepository.findPassengerByEmail(EmailNormalizer.normalize(email));
        if(passenger.isPresent()) {
            return new AuthPassengerDetails(passenger.get());
        }
        else{
            throw new UsernameNotFoundException(email);
        }

    }
}
