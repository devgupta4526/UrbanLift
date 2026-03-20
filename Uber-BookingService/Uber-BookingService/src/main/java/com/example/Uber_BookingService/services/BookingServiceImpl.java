package com.example.Uber_BookingService.services;

import com.example.Uber_BookingService.apis.LocationServiceApi;
import com.example.Uber_BookingService.apis.UberSocketApi;
import com.example.Uber_BookingService.dtos.*;
import com.example.Uber_BookingService.repositories.BookingRepository;
import com.example.Uber_BookingService.repositories.DriverRepository;
import com.example.Uber_BookingService.repositories.PassengerRepository;
import com.example.Uber_EntityService.Models.Booking;
import com.example.Uber_EntityService.Models.BookingStatus;
import com.example.Uber_EntityService.Models.Passenger;
import org.springframework.web.client.RestTemplate;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class BookingServiceImpl implements BookingService{

    private final PassengerRepository passengerRepository;
    private final BookingRepository bookingRepository;

    private final RestTemplate restTemplate;

    private final LocationServiceApi locationServiceApi;

    private final UberSocketApi uberSocketApi;
    private final DriverRepository driverRepository;

    public BookingServiceImpl(PassengerRepository passengerRepository,
                              BookingRepository bookingRepository,
                              RestTemplate restTemplate,
                              LocationServiceApi locationServiceApi,
                              UberSocketApi uberSocketApi,
                              DriverRepository driverRepository) {
     this.driverRepository = driverRepository;
     this.passengerRepository = passengerRepository;
     this.bookingRepository = bookingRepository;
     this.restTemplate = restTemplate;
     this.locationServiceApi = locationServiceApi;
     this.uberSocketApi = uberSocketApi;
    }
    @Override
    public CreateBookingResponseDto createBooking(CreateBookingDto bookingDetails) {
        Optional<Passenger> passenger = passengerRepository.findById(bookingDetails.getPassengerId());
        Booking booking = Booking.builder()
                .bookingStatus(BookingStatus.ASSIGNING_DRIVER)
                .startLocation(bookingDetails.getStartLocation())
//                .endLocation(bookingDetails.getEndLocation())
                .passenger(passenger.get())
                .build();
        Booking newBooking = bookingRepository.save(booking);

        NearbyDriversRequestDto request = NearbyDriversRequestDto.builder()
                .latitude(bookingDetails.getStartLocation().getLatitude())
                .longitude(bookingDetails.getEndLocation().getLongitude())
                .build();

        processNearbyDrivers(request, bookingDetails.getPassengerId(), newBooking.getId());

        //
//        ResponseEntity<DriverLocationDto[]> result = restTemplate.postForEntity(LOCATION_SERVICE + "/api/location/nearby/drivers", request, DriverLocationDto[].class);
//
//        if(result.getStatusCode().is2xxSuccessful() && result.getBody() != null) {
//            List<DriverLocationDto> driverLocations = Arrays.asList(result.getBody());
//            driverLocations.forEach(driverLocationDto -> {
//                System.out.println(driverLocationDto.getDriverId() + " " + "lat: " + driverLocationDto.getLatitude() + "long: " + driverLocationDto.getLongitude());
//            });
//        }

        return CreateBookingResponseDto.builder()
                .bookingId(newBooking.getId())
                .bookingStatus(newBooking.getBookingStatus().toString())
                .build();



    }

    private void processNearbyDrivers(NearbyDriversRequestDto request, Long passengerId, Long bookingId) {
        Call<DriverLocationDto[]> call = locationServiceApi.getNearbyDrivers(request);

        call.enqueue(new Callback<DriverLocationDto[]>() {
            @Override
            public void onResponse(Call<DriverLocationDto[]> call, Response<DriverLocationDto[]> response) {

                if(response.isSuccessful() && response.body() != null) {
                    List<DriverLocationDto> driverLocations = Arrays.asList(response.body());
                    driverLocations.forEach(driverLocationDto -> {
                        System.out.println(driverLocationDto.getDriverId() + " " + "lat: " + driverLocationDto.getLatitude() + "long: " + driverLocationDto.getLongitude());
                    });

                    try {
                        raiseRideRequestAsync(RideRequestDto.builder().passengerId(passengerId).bookingId(bookingId).build());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                } else {
                    System.out.println("Request failed" + response.message());
                }
            }

            @Override
            public void onFailure(Call<DriverLocationDto[]> call, Throwable t) {
                t.printStackTrace();
            }
        });


    }


    private void raiseRideRequestAsync(RideRequestDto rideRequestDto) throws IOException {
        Call<Boolean> call = uberSocketApi.raiseRideRequest(rideRequestDto);
        System.out.println(call.request().url() + " " + call.request().method() + " " + call.request().headers());
        call.enqueue(new Callback<Boolean>() {
            @Override
            public void onResponse(Call<Boolean> call, Response<Boolean> response) {
                System.out.println(response.isSuccessful());
                System.out.println(response.message());
                if (response.isSuccessful() && response.body() != null) {
                    Boolean result = response.body();
                    System.out.println("Driver response is" + result.toString());

                } else {
                    System.out.println("Request for ride failed" + response.message());
                }
            }

            @Override
            public void onFailure(Call<Boolean> call, Throwable t) {
                t.printStackTrace();
            }
        });
    }

    @Override
    public UpdateBookingResponseDto updateBooking(UpdateBookingRequestDto bookingRequestDto, Long bookingId) {
        return null;
    }
}
