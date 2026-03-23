package com.example.Uber_BookingService.services;

import com.example.Uber_BookingService.apis.LocationServiceApi;
import com.example.Uber_BookingService.apis.UberSocketApi;
import com.example.Uber_BookingService.dtos.*;
import com.example.Uber_BookingService.producers.KafkaProducerService;
import com.example.Uber_BookingService.repositories.BookingRepository;
import com.example.Uber_BookingService.repositories.DriverRepository;
import com.example.Uber_BookingService.repositories.PassengerRepository;
import com.example.Uber_EntityService.Models.Booking;
import com.example.Uber_EntityService.Models.BookingStatus;
import com.example.Uber_EntityService.Models.Driver;
import com.example.Uber_EntityService.Models.Passenger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@Service
public class BookingServiceImpl implements BookingService{

    private final PassengerRepository passengerRepository;
    private final BookingRepository bookingRepository;

    private static final Logger logger = LoggerFactory.getLogger(BookingServiceImpl.class);
    private final LocationServiceApi locationServiceApi;

    private final UberSocketApi uberSocketApi;
    private final DriverRepository driverRepository;

    private final KafkaProducerService kafkaProducerService;

    public BookingServiceImpl(PassengerRepository passengerRepository,
                              BookingRepository bookingRepository,
                              LocationServiceApi locationServiceApi,
                              UberSocketApi uberSocketApi,
                              KafkaProducerService kafkaProducerService,
                              DriverRepository driverRepository) {
     this.driverRepository = driverRepository;
     this.passengerRepository = passengerRepository;
     this.bookingRepository = bookingRepository;
     this.locationServiceApi = locationServiceApi;
     this.uberSocketApi = uberSocketApi;
     this.kafkaProducerService = kafkaProducerService;
    }
    @Override
    public CreateBookingResponseDto createBooking(CreateBookingDto bookingDetails) {
        Optional<Passenger> passenger = passengerRepository.findById(bookingDetails.getPassengerId());
        if (passenger.isEmpty()) {
            throw new IllegalArgumentException("Passenger not found: " + bookingDetails.getPassengerId());
        }
        Date now = new Date();
        Booking booking = Booking.builder()
                .bookingStatus(BookingStatus.ASSIGNING_DRIVER)
                .bookingDate(now)
                .startTime(now)
                .endTime(now)
                .startLocation(bookingDetails.getStartLocation())
                .endLocation(bookingDetails.getEndLocation())
                .passenger(passenger.get())
                .build();
        Booking newBooking = bookingRepository.save(booking);

        NearbyDriversRequestDto request = NearbyDriversRequestDto.builder()
                .latitude(bookingDetails.getStartLocation().getLatitude())
                .longitude(bookingDetails.getStartLocation().getLongitude())
                .build();

        processNearbyDrivers(request, bookingDetails, newBooking.getId());

        //


        return CreateBookingResponseDto.builder()
                .bookingId(newBooking.getId())
                .bookingStatus(newBooking.getBookingStatus().toString())
                .build();



    }

    private void processNearbyDrivers(NearbyDriversRequestDto request, CreateBookingDto bookingDetails, Long bookingId) {
        Call<DriverLocationDto[]> call = locationServiceApi.getNearbyDrivers(request);

        call.enqueue(new Callback<DriverLocationDto[]>() {
            @Override
            public void onResponse(Call<DriverLocationDto[]> call, Response<DriverLocationDto[]> response) {

                if (response.isSuccessful() && response.body() != null) {
                    List<DriverLocationDto> driverLocations = Arrays.asList(response.body());
                    logger.info("Found {} nearby drivers", driverLocations.size());
                    driverLocations.forEach(driverLocationDto -> {
                        logger.debug("Driver {} at lat: {}, lng: {}", driverLocationDto.getDriverId(), driverLocationDto.getLatitude(), driverLocationDto.getLongitude());
                    });

                    List<Long> driverIds = driverLocations.stream()
                            .map(d -> {
                                try {
                                    return Long.parseLong(d.getDriverId());
                                } catch (NumberFormatException e) {
                                    return null;
                                }
                            })
                            .filter(id -> id != null)
                            .collect(Collectors.toList());

                    RideRequestDto rideRequestDto = RideRequestDto.builder()
                            .passengerId(bookingDetails.getPassengerId())
                            .bookingId(bookingId)
                            .pickupLat(bookingDetails.getStartLocation().getLatitude())
                            .pickupLng(bookingDetails.getStartLocation().getLongitude())
                            .dropLat(bookingDetails.getEndLocation().getLatitude())
                            .dropLng(bookingDetails.getEndLocation().getLongitude())
                            .driverIds(driverIds.isEmpty() ? null : driverIds)
                            .build();

                    try {
                        raiseRideRequestAsync(rideRequestDto);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                } else {
                    logger.error("Failed to get nearby drivers: {}", response.message());
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
        logger.debug("Sending ride request to socket service: {} {} {}", call.request().url(), call.request().method(), call.request().headers());
        call.enqueue(new Callback<Boolean>() {
            @Override
            public void onResponse(Call<Boolean> call, Response<Boolean> response) {
                logger.info("Ride request sent successfully: {}", response.isSuccessful());
                if (response.isSuccessful() && response.body() != null) {
                    Boolean result = response.body();
                    logger.info("Socket service response: {}", result);

                } else {
                    logger.error("Failed to send ride request: {}", response.message());
                }
            }

            @Override
            public void onFailure(Call<Boolean> call, Throwable t) {
                logger.error("Failed to send ride request", t);
            }
        });
    }

    @Override
    @Transactional
    public UpdateBookingResponseDto updateBooking(UpdateBookingRequestDto bookingRequestDto, Long bookingId) {
        Optional<Booking> existingBooking = bookingRepository.findById(bookingId);
        if (existingBooking.isEmpty()) {
            throw new IllegalArgumentException("Booking not found: " + bookingId);
        }

        BookingStatus status = bookingRequestDto.getStatus() != null
                ? BookingStatus.valueOf(bookingRequestDto.getStatus())
                : BookingStatus.SCHEDULED;

        Driver driver = null;
        if (bookingRequestDto.getDriverId() != null && bookingRequestDto.getDriverId().isPresent()) {
            Long driverId = bookingRequestDto.getDriverId().get();
            Optional<Driver> driverOpt = driverRepository.findById(driverId);
            if (driverOpt.isPresent()) {
                driver = driverOpt.get();
            }
        }

        bookingRepository.updateBookingStatusAndDriverById(bookingId, status, driver);

        Optional<Booking> booking = bookingRepository.findById(bookingId);
        return UpdateBookingResponseDto.builder()
                .bookingId(bookingId)
                .status(booking.get().getBookingStatus())
                .driver(Optional.ofNullable(booking.get().getDriver()))
                .build();
    }

    @Override
    public BookingDetailDto getBookingById(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));
        return toDetailDto(booking);
    }

    @Override
    public List<BookingDetailDto> getBookingsByPassengerId(Long passengerId) {
        return bookingRepository.findByPassengerId(passengerId).stream()
                .map(this::toDetailDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<BookingDetailDto> getBookingsByDriverId(Long driverId) {
        return bookingRepository.findByDriverId(driverId).stream()
                .map(this::toDetailDto)
                .collect(Collectors.toList());
    }

    @Override
    public UpdateBookingResponseDto updateBookingStatus(Long bookingId, String status) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));
        BookingStatus newStatus = BookingStatus.valueOf(status);
        bookingRepository.updateBookingStatusById(bookingId, newStatus);
        Optional<Booking> updated = bookingRepository.findById(bookingId);

        // Send notification event
        sendNotificationForStatusChange(updated.get(), newStatus);

        // Publish event if completed
        if (newStatus == BookingStatus.COMPLETED) {
            BookingCompletedEventDto event = new BookingCompletedEventDto();
            event.setBookingId(bookingId);
            event.setPassengerId(updated.get().getPassenger().getId());
            if (updated.get().getDriver() != null) {
                event.setDriverId(updated.get().getDriver().getId());
            }
            event.setTotalDistance(BigDecimal.valueOf(updated.get().getTotalDistance()));
            // Assume fare is stored in booking or calculate
            // For now, set a default
            event.setFare(BigDecimal.valueOf(100.0)); // TODO: get from booking
            kafkaProducerService.sendBookingCompletedEvent(event);
        }

        return UpdateBookingResponseDto.builder()
                .bookingId(bookingId)
                .status(updated.get().getBookingStatus())
                .driver(Optional.ofNullable(updated.get().getDriver()))
                .build();
    }

    @Override
    public UpdateBookingResponseDto cancelBooking(Long bookingId) {
        return updateBookingStatus(bookingId, BookingStatus.CANCELLED.name());
    }

    private BookingDetailDto toDetailDto(Booking b) {
        return BookingDetailDto.builder()
                .id(b.getId())
                .bookingStatus(b.getBookingStatus() != null ? b.getBookingStatus().name() : null)
                .bookingDate(b.getBookingDate())
                .startTime(b.getStartTime())
                .endTime(b.getEndTime())
                .totalDistance(b.getTotalDistance())
                .passenger(Optional.ofNullable(b.getPassenger()))
                .driver(Optional.ofNullable(b.getDriver()))
                .startLocation(b.getStartLocation())
                .endLocation(b.getEndLocation())
                .build();
    }

    private void sendNotificationForStatusChange(Booking booking, BookingStatus newStatus) {
        String eventType = null;
        switch (newStatus) {
            case CAB_ARRIVED:
                eventType = "CAB_ARRIVED";
                break;
            case IN_RIDE:
                eventType = "RIDE_STARTED";
                break;
            case COMPLETED:
                eventType = "RIDE_COMPLETED";
                break;
            case CANCELLED:
                eventType = "RIDE_CANCELLED";
                break;
            default:
                return; // No notification for other statuses
        }

        NotificationEventDto event = new NotificationEventDto();
        event.setEventType(eventType);
        event.setUserId(booking.getPassenger().getId());
        event.setUserType("PASSENGER");
        // Add payload if needed
        kafkaProducerService.sendNotificationEvent(event);
    }
}
