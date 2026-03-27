package com.example.Uber_BookingService.services;

import com.example.Uber_BookingService.apis.LocationServiceApi;
import com.example.Uber_BookingService.apis.UberSocketApi;
import com.example.Uber_BookingService.dtos.DriverLocationDto;
import com.example.Uber_BookingService.dtos.BookingCompletedEventDto;
import com.example.Uber_BookingService.dtos.BookingParticipantDto;
import com.example.Uber_BookingService.dtos.CreateBookingDto;
import com.example.Uber_BookingService.dtos.CreateBookingResponseDto;
import com.example.Uber_BookingService.dtos.BookingDetailDto;
import com.example.Uber_BookingService.dtos.NearbyDriversRequestDto;
import com.example.Uber_BookingService.dtos.NotificationEventDto;
import com.example.Uber_BookingService.dtos.RideRequestDto;
import com.example.Uber_BookingService.dtos.TripRatingRequestDto;
import com.example.Uber_BookingService.dtos.TripRatingSummaryDto;
import com.example.Uber_BookingService.dtos.UpdateBookingRequestDto;
import com.example.Uber_BookingService.dtos.UpdateBookingResponseDto;
import com.example.Uber_BookingService.locking.LockHandle;
import com.example.Uber_BookingService.locking.RedlockManager;
import com.example.Uber_BookingService.producers.KafkaProducerService;
import com.example.Uber_BookingService.repositories.BookingRepository;
import com.example.Uber_BookingService.repositories.BookingIdempotencyRepository;
import com.example.Uber_BookingService.repositories.DriverRepository;
import com.example.Uber_BookingService.repositories.PassengerRepository;
import com.example.Uber_BookingService.repositories.TripRatingRepository;
import com.example.Uber_EntityService.Models.Booking;
import com.example.Uber_EntityService.Models.BookingStatus;
import com.example.Uber_EntityService.Models.Driver;
import com.example.Uber_EntityService.Models.Passenger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DuplicateKeyException;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class BookingServiceImpl implements BookingService {

    private static final List<BookingStatus> ACTIVE_BOOKING_STATUSES = List.of(
            BookingStatus.ASSIGNING_DRIVER,
            BookingStatus.SCHEDULED,
            BookingStatus.CAB_ARRIVED,
            BookingStatus.IN_RIDE);

    private final PassengerRepository passengerRepository;
    private final BookingRepository bookingRepository;

    private static final Logger logger = LoggerFactory.getLogger(BookingServiceImpl.class);
    private final LocationServiceApi locationServiceApi;

    private final UberSocketApi uberSocketApi;
    private final DriverRepository driverRepository;
    private final BookingIdempotencyRepository bookingIdempotencyRepository;
    private final RedlockManager redlockManager;
    private final TripRatingRepository tripRatingRepository;

    private final KafkaProducerService kafkaProducerService;

    public BookingServiceImpl(PassengerRepository passengerRepository,
                              BookingRepository bookingRepository,
                              LocationServiceApi locationServiceApi,
                              UberSocketApi uberSocketApi,
                              KafkaProducerService kafkaProducerService,
                              DriverRepository driverRepository,
                              BookingIdempotencyRepository bookingIdempotencyRepository,
                              RedlockManager redlockManager,
                              TripRatingRepository tripRatingRepository) {
     this.driverRepository = driverRepository;
     this.passengerRepository = passengerRepository;
     this.bookingRepository = bookingRepository;
     this.locationServiceApi = locationServiceApi;
     this.uberSocketApi = uberSocketApi;
     this.kafkaProducerService = kafkaProducerService;
     this.bookingIdempotencyRepository = bookingIdempotencyRepository;
     this.redlockManager = redlockManager;
     this.tripRatingRepository = tripRatingRepository;
    }

    @Override
    @Transactional
    public CreateBookingResponseDto createBooking(CreateBookingDto bookingDetails, String idempotencyKey) {
        validateCreateBookingPayload(bookingDetails);
        String cleanedIdempotencyKey = sanitizeIdempotencyKey(idempotencyKey);
        LockHandle lock = redlockManager.acquireForPassenger(bookingDetails.getPassengerId());
        CreateBookingResponseDto response;
        NearbyDriversRequestDto request;
        CreateBookingDto immutableRequest = bookingDetails;
        Long createdBookingId;
        try {
            Passenger passenger = passengerRepository.findWithActiveBooking(bookingDetails.getPassengerId())
                    .orElseThrow(() -> new IllegalArgumentException("Passenger not found: " + bookingDetails.getPassengerId()));

            if (cleanedIdempotencyKey != null) {
                Long existingBookingId = bookingIdempotencyRepository.findBookingId(passenger.getId(), cleanedIdempotencyKey);
                if (existingBookingId != null) {
                    Booking existingBooking = bookingRepository.findDetailedById(existingBookingId)
                            .orElseThrow(() -> new IllegalStateException("Idempotency mapping points to missing booking"));
                    if (!sameRoute(existingBooking, bookingDetails)) {
                        throw new IllegalStateException("Idempotency-Key already used with different booking details");
                    }
                    return CreateBookingResponseDto.builder()
                            .bookingId(existingBooking.getId())
                            .bookingStatus(existingBooking.getBookingStatus().toString())
                            .build();
                }
            }

            long activeCount = bookingRepository.countByPassengerIdAndBookingStatusIn(
                    passenger.getId(), ACTIVE_BOOKING_STATUSES);
            if (activeCount > 0 || passenger.getActiveBooking() != null) {
                throw new IllegalStateException(
                        "Passenger already has an active booking. Finish or cancel it before booking again.");
            }

            Date now = new Date();
            Booking booking = Booking.builder()
                    .bookingStatus(BookingStatus.ASSIGNING_DRIVER)
                    .bookingDate(now)
                    .startTime(now)
                    .endTime(now)
                    .startLocation(bookingDetails.getStartLocation())
                    .endLocation(bookingDetails.getEndLocation())
                    .passenger(passenger)
                    .build();
            Booking newBooking = bookingRepository.save(booking);

            if (cleanedIdempotencyKey != null) {
                try {
                    bookingIdempotencyRepository.insertMapping(passenger.getId(), cleanedIdempotencyKey, newBooking.getId());
                } catch (DuplicateKeyException ex) {
                    // Race-safe path: another request created mapping first; return canonical booking.
                    Long existingBookingId = bookingIdempotencyRepository.findBookingId(passenger.getId(), cleanedIdempotencyKey);
                    if (existingBookingId != null && !Objects.equals(existingBookingId, newBooking.getId())) {
                        Booking existingBooking = bookingRepository.findDetailedById(existingBookingId)
                                .orElseThrow(() -> new IllegalStateException("Idempotency mapping points to missing booking"));
                        if (!sameRoute(existingBooking, bookingDetails)) {
                            throw new IllegalStateException("Idempotency-Key already used with different booking details");
                        }
                        return CreateBookingResponseDto.builder()
                                .bookingId(existingBooking.getId())
                                .bookingStatus(existingBooking.getBookingStatus().toString())
                                .build();
                    }
                }
            }

            passenger.setActiveBooking(newBooking);
            passengerRepository.save(passenger);

            request = NearbyDriversRequestDto.builder()
                    .latitude(bookingDetails.getStartLocation().getLatitude())
                    .longitude(bookingDetails.getStartLocation().getLongitude())
                    .build();
            createdBookingId = newBooking.getId();
            response = CreateBookingResponseDto.builder()
                    .bookingId(newBooking.getId())
                    .bookingStatus(newBooking.getBookingStatus().toString())
                    .build();
        } finally {
            redlockManager.release(lock);
        }

        processNearbyDrivers(request, immutableRequest, createdBookingId);
        return response;
    }

    private static String sanitizeIdempotencyKey(String raw) {
        if (raw == null) return null;
        String cleaned = raw.trim();
        if (cleaned.isEmpty()) return null;
        if (cleaned.length() > 128) {
            throw new IllegalArgumentException("Idempotency-Key length must be <= 128");
        }
        return cleaned;
    }

    private static boolean sameRoute(Booking existing, CreateBookingDto request) {
        if (existing.getPassenger() == null || request.getPassengerId() == null) {
            return false;
        }
        if (!Objects.equals(existing.getPassenger().getId(), request.getPassengerId())) {
            return false;
        }
        return sameLocation(existing.getStartLocation(), request.getStartLocation())
                && sameLocation(existing.getEndLocation(), request.getEndLocation());
    }

    private static boolean sameLocation(com.example.Uber_EntityService.Models.ExactLocation a,
                                        com.example.Uber_EntityService.Models.ExactLocation b) {
        if (a == null || b == null) return false;
        return Objects.equals(a.getLatitude(), b.getLatitude())
                && Objects.equals(a.getLongitude(), b.getLongitude());
    }

    private void validateCreateBookingPayload(CreateBookingDto bookingDetails) {
        if (bookingDetails.getPassengerId() == null) {
            throw new IllegalArgumentException("passengerId is required");
        }
        if (bookingDetails.getStartLocation() == null || bookingDetails.getEndLocation() == null) {
            throw new IllegalArgumentException("startLocation and endLocation are required");
        }
        validateCoordinates(bookingDetails.getStartLocation().getLatitude(),
                bookingDetails.getStartLocation().getLongitude(), "startLocation");
        validateCoordinates(bookingDetails.getEndLocation().getLatitude(),
                bookingDetails.getEndLocation().getLongitude(), "endLocation");
    }

    private static void validateCoordinates(Double lat, Double lng, String field) {
        if (lat == null || lng == null) {
            throw new IllegalArgumentException(field + ": latitude and longitude are required");
        }
        if (lat < -90 || lat > 90 || lng < -180 || lng > 180) {
            throw new IllegalArgumentException(field + ": coordinates out of valid range");
        }
    }

    @Override
    public Long getPassengerIdForBooking(Long bookingId) {
        return bookingRepository.findPassengerIdByBookingId(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));
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
                        logger.error("Failed to enqueue ride request to socket service for booking {}", bookingId, e);
                    }

                } else {
                    logger.error("Failed to get nearby drivers: {}", response.message());
                }
            }

            @Override
            public void onFailure(Call<DriverLocationDto[]> call, Throwable t) {
                // REMOVED: t.printStackTrace() — use structured logging for production observability.
                logger.error("Location service call failed while fetching nearby drivers for booking {}", bookingId, t);
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
        Optional<Booking> existingBooking = bookingRepository.findDetailedById(bookingId);
        if (existingBooking.isEmpty()) {
            throw new IllegalArgumentException("Booking not found: " + bookingId);
        }
        Booking current = existingBooking.get();

        BookingStatus status = bookingRequestDto.getStatus() != null
                ? BookingStatus.valueOf(bookingRequestDto.getStatus())
                : BookingStatus.SCHEDULED;

        validateStatusTransition(current.getBookingStatus(), status);

        Driver driver = null;
        if (bookingRequestDto.getDriverId() != null) {
            driver = driverRepository.findById(bookingRequestDto.getDriverId())
                    .orElseThrow(() -> new IllegalArgumentException("Driver not found: "
                            + bookingRequestDto.getDriverId()));
        }
        if (status == BookingStatus.SCHEDULED && driver == null && current.getDriver() == null) {
            throw new IllegalStateException("Driver assignment requires driverId");
        }
        bookingRepository.updateBookingStatusAndDriverById(bookingId, status, driver);

        if (status == BookingStatus.COMPLETED || status == BookingStatus.CANCELLED) {
            clearPassengerActiveBookingIfMatches(bookingId);
        }

        Booking booking = bookingRepository.findDetailedById(bookingId).orElseThrow(
                () -> new IllegalArgumentException("Booking not found: " + bookingId)
        );
        return UpdateBookingResponseDto.builder()
                .bookingId(bookingId)
                .status(booking.getBookingStatus().name())
                .driver(toParticipantDto(booking.getDriver()))
                .build();
    }

    @Override
    public BookingDetailDto getBookingById(Long bookingId) {
        Booking booking = bookingRepository.findDetailedById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));
        return toDetailDto(booking);
    }

    @Override
    public List<BookingDetailDto> getBookingsByPassengerId(Long passengerId) {
        return bookingRepository.findDetailedByPassengerId(passengerId).stream()
                .map(this::toDetailDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<BookingDetailDto> getBookingsByDriverId(Long driverId) {
        return bookingRepository.findDetailedByDriverId(driverId).stream()
                .map(this::toDetailDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public UpdateBookingResponseDto updateBookingStatus(Long bookingId, String status) {
        Booking booking = bookingRepository.findDetailedById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));
        BookingStatus newStatus = BookingStatus.valueOf(status);
        validateStatusTransition(booking.getBookingStatus(), newStatus);
        if (newStatus == BookingStatus.SCHEDULED && booking.getDriver() == null) {
            throw new IllegalStateException("Cannot move to SCHEDULED without assigned driver");
        }
        bookingRepository.updateBookingStatusById(bookingId, newStatus);
        Booking updated = bookingRepository.findDetailedById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));

        sendNotificationForStatusChange(updated, newStatus);

        if (newStatus == BookingStatus.COMPLETED) {
            BookingCompletedEventDto event = new BookingCompletedEventDto();
            event.setBookingId(bookingId);
            event.setPassengerId(updated.getPassenger().getId());
            if (updated.getDriver() != null) {
                event.setDriverId(updated.getDriver().getId());
            }
            event.setTotalDistance(BigDecimal.valueOf(updated.getTotalDistance()));
            event.setFare(estimateFareForCompletedRide(updated));
            kafkaProducerService.sendBookingCompletedEvent(event);
        }

        if (newStatus == BookingStatus.COMPLETED || newStatus == BookingStatus.CANCELLED) {
            clearPassengerActiveBookingIfMatches(bookingId);
        }

        return UpdateBookingResponseDto.builder()
                .bookingId(bookingId)
                .status(updated.getBookingStatus().name())
                .driver(toParticipantDto(updated.getDriver()))
                .build();
    }

    @Override
    public UpdateBookingResponseDto cancelBooking(Long bookingId) {
        return updateBookingStatus(bookingId, BookingStatus.CANCELLED.name());
    }

    @Override
    @Transactional
    public void rateDriver(Long bookingId, TripRatingRequestDto request) {
        Booking booking = bookingRepository.findDetailedById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));
        ensureCompleted(booking);
        if (booking.getPassenger() == null || !Objects.equals(booking.getPassenger().getId(), request.getActorId())) {
            throw new IllegalStateException("Only booking passenger can rate driver");
        }
        if (booking.getDriver() == null) {
            throw new IllegalStateException("Cannot rate driver: no driver assigned");
        }
        tripRatingRepository.upsertPassengerToDriver(
                bookingId,
                request.getActorId(),
                request.getScore(),
                sanitizeComment(request.getComment())
        );
    }

    @Override
    @Transactional
    public void ratePassenger(Long bookingId, TripRatingRequestDto request) {
        Booking booking = bookingRepository.findDetailedById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));
        ensureCompleted(booking);
        if (booking.getDriver() == null || !Objects.equals(booking.getDriver().getId(), request.getActorId())) {
            throw new IllegalStateException("Only assigned driver can rate passenger");
        }
        if (booking.getPassenger() == null) {
            throw new IllegalStateException("Cannot rate passenger: no passenger linked");
        }
        tripRatingRepository.upsertDriverToPassenger(
                bookingId,
                request.getActorId(),
                request.getScore(),
                sanitizeComment(request.getComment())
        );
    }

    @Override
    public TripRatingSummaryDto getTripRatings(Long bookingId) {
        if (!bookingRepository.existsById(bookingId)) {
            throw new IllegalArgumentException("Booking not found: " + bookingId);
        }
        return tripRatingRepository.getSummary(bookingId);
    }

    private BookingDetailDto toDetailDto(Booking b) {
        return BookingDetailDto.builder()
                .id(b.getId())
                .bookingStatus(b.getBookingStatus() != null ? b.getBookingStatus().name() : null)
                .bookingDate(b.getBookingDate())
                .startTime(b.getStartTime())
                .endTime(b.getEndTime())
                .totalDistance(b.getTotalDistance())
                .passenger(toParticipantDto(b.getPassenger()))
                .driver(toParticipantDto(b.getDriver()))
                .startLocation(b.getStartLocation())
                .endLocation(b.getEndLocation())
                .build();
    }

    private static BookingParticipantDto toParticipantDto(Passenger p) {
        if (p == null) {
            return null;
        }
        return BookingParticipantDto.builder()
                .id(p.getId())
                .firstName(p.getFirstName())
                .lastName(p.getLastName())
                .email(p.getEmail())
                .phoneNumber(p.getPhoneNumber())
                .build();
    }

    private static BookingParticipantDto toParticipantDto(Driver d) {
        if (d == null) {
            return null;
        }
        return BookingParticipantDto.builder()
                .id(d.getId())
                .firstName(d.getFirstName())
                .lastName(d.getLastName())
                .email(d.getEmail())
                .phoneNumber(d.getPhoneNumber())
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

    /**
     * Fare estimate for payment pipeline when no dedicated quote is stored on {@link Booking}
     * (base + per-km using {@link Booking#getTotalDistance()} as km, minimum 1 km).
     */
    private static BigDecimal estimateFareForCompletedRide(Booking booking) {
        long km = Math.max(booking.getTotalDistance(), 1L);
        return BigDecimal.valueOf(50.0)
                .add(BigDecimal.valueOf(km).multiply(BigDecimal.valueOf(12.0)))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private static void validateStatusTransition(BookingStatus current, BookingStatus next) {
        if (current == null) {
            throw new IllegalStateException("Current booking status is missing");
        }
        if (next == null) {
            throw new IllegalArgumentException("Requested booking status is required");
        }
        if (current == next) {
            return; // idempotent repeated call
        }
        boolean allowed = switch (current) {
            case ASSIGNING_DRIVER -> (next == BookingStatus.SCHEDULED || next == BookingStatus.CANCELLED);
            case SCHEDULED -> (next == BookingStatus.CAB_ARRIVED || next == BookingStatus.CANCELLED);
            case CAB_ARRIVED -> (next == BookingStatus.IN_RIDE || next == BookingStatus.CANCELLED);
            case IN_RIDE -> (next == BookingStatus.COMPLETED || next == BookingStatus.CANCELLED);
            case COMPLETED, CANCELLED -> false;
        };
        if (!allowed) {
            throw new IllegalStateException("Invalid booking status transition: " + current + " -> " + next);
        }
    }

    private void clearPassengerActiveBookingIfMatches(Long bookingId) {
        bookingRepository.findWithPassengerAndActiveBooking(bookingId).ifPresent(b -> {
            Passenger p = b.getPassenger();
            if (p == null) {
                return;
            }
            Booking active = p.getActiveBooking();
            if (active != null && Objects.equals(active.getId(), bookingId)) {
                p.setActiveBooking(null);
                passengerRepository.save(p);
            }
        });
    }

    private static void ensureCompleted(Booking booking) {
        if (booking.getBookingStatus() != BookingStatus.COMPLETED) {
            throw new IllegalStateException("Ratings are allowed only for completed trips");
        }
    }

    private static String sanitizeComment(String comment) {
        if (comment == null) return null;
        String trimmed = comment.trim();
        if (trimmed.isEmpty()) return null;
        return trimmed.length() > 500 ? trimmed.substring(0, 500) : trimmed;
    }
}
