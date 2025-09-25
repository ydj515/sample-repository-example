package com.example.jpajavaexample;

import com.example.jpajavaexample.domain.Reservation;
import com.example.jpajavaexample.domain.Schedule;
import com.example.jpajavaexample.domain.ScheduleSeat;
import com.example.jpajavaexample.domain.Seat;
import com.example.jpajavaexample.domain.Venue;
import com.example.jpajavaexample.domain.VenueSection;
import com.example.jpajavaexample.domain.SeatStatus;
import com.example.jpajavaexample.domain.coupon.model.Coupon;
import com.example.jpajavaexample.domain.coupon.model.FixedAmountCoupon;
import com.example.jpajavaexample.domain.coupon.model.PercentageCoupon;
import com.example.jpajavaexample.domain.coupon.repository.CouponRepository;
import com.example.jpajavaexample.domain.performance.model.Performance;
import com.example.jpajavaexample.domain.performance.repository.PerformanceRepository;
import com.example.jpajavaexample.domain.reservation.repository.ReservationRepository;
import com.example.jpajavaexample.domain.schedule.repository.ScheduleRepository;
import com.example.jpajavaexample.domain.scheduleseat.repository.ScheduleSeatRepository;
import com.example.jpajavaexample.domain.user.model.User;
import com.example.jpajavaexample.domain.user.repository.UserRepository;
import com.example.jpajavaexample.domain.venue.repository.VenueRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class InitDataInitializer implements ApplicationRunner {

    private static final int TARGET_SAMPLE_USER_COUNT = 100;
    private static final int TARGET_SAMPLE_PERFORMANCE_COUNT = 100;
    private static final int TARGET_SAMPLE_VENUE_COUNT = 100;
    private static final int TARGET_SAMPLE_SCHEDULE_COUNT = 300;
    private static final int TARGET_SAMPLE_SCHEDULE_SEAT_COUNT = 5_000;
    private static final int TARGET_SAMPLE_RESERVATION_COUNT = 200;
    private static final List<String> PERFORMANCE_CATEGORIES = List.of(
        "MUSICAL",
        "CONCERT",
        "PLAY",
        "CLASSIC",
        "EXHIBITION"
    );
    private static final List<String> SECTION_NAMES = List.of("VIP", "R", "S", "A", "B", "C", "D", "E", "F", "G");

    private final UserRepository userRepository;
    private final PerformanceRepository performanceRepository;
    private final CouponRepository couponRepository;
    private final VenueRepository venueRepository;
    private final ScheduleRepository scheduleRepository;
    private final ScheduleSeatRepository scheduleSeatRepository;
    private final ReservationRepository reservationRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<User> users = initUsers();
        List<Performance> performances = initPerformances();
        List<Venue> venues = initVenues();
        List<Coupon> coupons = initCoupons();
        Map<Long, List<Seat>> seatsByVenue = extractSeatsByVenue(venues);
        List<Schedule> schedules = initSchedules(performances, venues);
        List<ScheduleSeat> scheduleSeats = initScheduleSeats(schedules, seatsByVenue);
        initReservations(users, scheduleSeats, coupons);
    }

    private List<User> initUsers() {
        long currentCount = userRepository.findAll(PageRequest.of(0, 1))
            .getTotalElements();

        if (currentCount < TARGET_SAMPLE_USER_COUNT) {
            int toCreate = (int) (TARGET_SAMPLE_USER_COUNT - currentCount);
            int index = 1;
            int created = 0;

            while (created < toCreate) {
                String email = sampleEmail(index);

                if (!userRepository.existsByEmail(email)) {
                    User user = User.create(sampleName(index), email, samplePassword(index));
                    userRepository.save(user);
                    created++;
                }

                index++;
            }
        }

        return userRepository.findAll();
    }

    private List<Performance> initPerformances() {
        long currentCount = performanceRepository.count();

        if (currentCount < TARGET_SAMPLE_PERFORMANCE_COUNT) {
            long toCreate = TARGET_SAMPLE_PERFORMANCE_COUNT - currentCount;
            for (long offset = 0; offset < toCreate; offset++) {
                long sequence = currentCount + offset + 1;
                LocalDate startDate = samplePerformanceStartDate(sequence);
                LocalDate endDate = samplePerformanceEndDate(startDate);

                Performance performance = Performance.create(
                    samplePerformanceTitle(sequence),
                    samplePerformanceCategory(sequence),
                    startDate,
                    endDate,
                    samplePerformancePoster(sequence)
                );

                performanceRepository.save(performance);
            }
        }

        return performanceRepository.findAll();
    }

    private List<Venue> initVenues() {
        long currentCount = venueRepository.count();

        if (currentCount < TARGET_SAMPLE_VENUE_COUNT) {
            long toCreate = TARGET_SAMPLE_VENUE_COUNT - currentCount;
            for (long offset = 0; offset < toCreate; offset++) {
                long sequence = currentCount + offset + 1;
                Venue venue = Venue.create(
                    sampleVenueName(sequence),
                    sampleVenueLocation(sequence)
                );

                for (int sectionIndex = 0; sectionIndex < SECTION_NAMES.size(); sectionIndex++) {
                    VenueSection section = venue.addSection(
                        SECTION_NAMES.get(sectionIndex),
                        50
                    );

                    for (int row = 0; row < 5; row++) {
                        String rowLabel = Character.toString((char) ('A' + row));
                        for (int seatNumber = 1; seatNumber <= 10; seatNumber++) {
                            section.addSeat(rowLabel, seatNumber + sectionIndex * 10);
                        }
                    }
                }

                venueRepository.save(venue);
            }
        }

        return venueRepository.findAll();
    }

    private List<Schedule> initSchedules(List<Performance> performances, List<Venue> venues) {
        if (performances.isEmpty() || venues.isEmpty()) {
            return List.of();
        }

        long currentCount = scheduleRepository.count();

        if (currentCount < TARGET_SAMPLE_SCHEDULE_COUNT) {
            long toCreate = TARGET_SAMPLE_SCHEDULE_COUNT - currentCount;
            for (long offset = 0; offset < toCreate; offset++) {
                long sequence = currentCount + offset;
                Performance performance = performances.get((int) (sequence % performances.size()));
                Venue venue = venues.get((int) (sequence % venues.size()));

                LocalDate startDate = performance.getStartDate().plusDays((int) (sequence % 14));
                LocalDateTime startsAt = LocalDateTime.of(startDate, LocalTime.of(19, 0));

                scheduleRepository.save(Schedule.create(performance, venue, startsAt));
            }
        }

        return scheduleRepository.findAll();
    }

    private List<ScheduleSeat> initScheduleSeats(List<Schedule> schedules, Map<Long, List<Seat>> seatsByVenue) {
        if (schedules.isEmpty() || seatsByVenue.isEmpty()) {
            return List.of();
        }

        long currentCount = scheduleSeatRepository.count();

        if (currentCount < TARGET_SAMPLE_SCHEDULE_SEAT_COUNT) {
            long toCreate = TARGET_SAMPLE_SCHEDULE_SEAT_COUNT - currentCount;
            long created = 0;
            for (Schedule schedule : schedules) {
                if (created >= toCreate) {
                    break;
                }

                List<Seat> seats = seatsByVenue.getOrDefault(schedule.getVenue().getId(), List.of());
                if (seats.isEmpty()) {
                    continue;
                }

                for (Seat seat : seats) {
                    if (created >= toCreate) {
                        break;
                    }

                    scheduleSeatRepository.save(
                        ScheduleSeat.createAvailable(
                            schedule,
                            seat,
                            sampleSeatPrice(schedule, seat, created)
                        )
                    );
                    created++;
                }
            }
        }

        return scheduleSeatRepository.findAll();
    }

    private void initReservations(List<User> users, List<ScheduleSeat> scheduleSeats, List<Coupon> coupons) {
        if (users.isEmpty() || scheduleSeats.isEmpty()) {
            return;
        }

        List<ScheduleSeat> availableSeats = scheduleSeats.stream()
            .filter(seat -> seat.getReservation() == null && seat.getStatus() == SeatStatus.AVAILABLE)
            .collect(Collectors.toCollection(ArrayList::new));

        long currentCount = reservationRepository.count();

        if (currentCount >= TARGET_SAMPLE_RESERVATION_COUNT) {
            return;
        }

        long toCreate = Math.min(
            TARGET_SAMPLE_RESERVATION_COUNT - currentCount,
            availableSeats.size()
        );

        for (int index = 0; index < toCreate; index++) {
            ScheduleSeat seat = availableSeats.get(index);
            User user = users.get(index % users.size());

            Reservation reservation = reservationRepository.save(
                Reservation.create(
                    user,
                    seat.getSchedule(),
                    sampleReservedAt(index)
                )
            );

            seat.reserveFor(reservation);
            scheduleSeatRepository.save(seat);

            if (!coupons.isEmpty() && index % 3 == 0) {
                Coupon coupon = coupons.get(index % coupons.size());
                reservation.applyCoupon(coupon);
                reservationRepository.save(reservation);
            }
        }
    }

    private List<Coupon> initCoupons() {
        Map<String, Coupon> existingByCode = couponRepository.findAll().stream()
            .collect(Collectors.toMap(Coupon::getCode, Function.identity(), (left, right) -> left, HashMap::new));

        ensureCoupon(existingByCode, "FX-25000", () -> FixedAmountCoupon.create(
            "FX-25000",
            "Fixed Coupon 25000",
            true,
            LocalDateTime.now().plusDays(120),
            new BigDecimal("25000.00")
        ));
        ensureCoupon(existingByCode, "FX-10000", () -> FixedAmountCoupon.create(
            "FX-10000",
            "Fixed Coupon 10000",
            true,
            LocalDateTime.now().plusDays(90),
            new BigDecimal("10000.00")
        ));
        ensureCoupon(existingByCode, "FX-01000", () -> FixedAmountCoupon.create(
            "FX-01000",
            "Fixed Coupon 1000",
            false,
            LocalDateTime.now().minusDays(7),
            new BigDecimal("1000.00")
        ));

        ensureCoupon(existingByCode, "RT-02000", () -> PercentageCoupon.create(
            "RT-02000",
            "Percent Coupon 20%",
            true,
            LocalDateTime.now().plusDays(45),
            new BigDecimal("0.2000"),
            new BigDecimal("30000.00")
        ));
        ensureCoupon(existingByCode, "RT-01000", () -> PercentageCoupon.create(
            "RT-01000",
            "Percent Coupon 10%",
            true,
            LocalDateTime.now().plusDays(60),
            new BigDecimal("0.1000"),
            new BigDecimal("20000.00")
        ));
        ensureCoupon(existingByCode, "RT-05000", () -> PercentageCoupon.create(
            "RT-05000",
            "Percent Coupon 50%",
            false,
            LocalDateTime.now().minusDays(3),
            new BigDecimal("0.5000"),
            new BigDecimal("50000.00")
        ));
        ensureCoupon(existingByCode, "RT-08000", () -> PercentageCoupon.create(
            "RT-08000",
            "Percent Coupon 80%",
            true,
            LocalDateTime.now().plusDays(10),
            new BigDecimal("0.8000"),
            new BigDecimal("80000.00")
        ));

        return couponRepository.findAll();
    }

    private Map<Long, List<Seat>> extractSeatsByVenue(List<Venue> venues) {
        Map<Long, List<Seat>> seatsByVenue = new HashMap<>();
        for (Venue venue : venues) {
            List<Seat> seats = venue.getSections().stream()
                .flatMap(section -> section.getSeats().stream())
                .collect(Collectors.toCollection(ArrayList::new));
            seatsByVenue.put(venue.getId(), seats);
        }
        return seatsByVenue;
    }

    private String sampleName(int index) {
        return "Sample User " + index;
    }

    private String sampleEmail(int index) {
        return "sample-user-%03d@example.com".formatted(index);
    }

    private String samplePassword(int index) {
        return "password-%03d".formatted(index);
    }

    private String samplePerformanceTitle(long index) {
        return "Sample Performance %03d".formatted(index);
    }

    private String samplePerformanceCategory(long index) {
        int categoryIndex = (int) ((index - 1) % PERFORMANCE_CATEGORIES.size());
        return PERFORMANCE_CATEGORIES.get(categoryIndex);
    }

    private LocalDate samplePerformanceStartDate(long index) {
        return LocalDate.of(2024, 1, 1).plusDays(index - 1);
    }

    private LocalDate samplePerformanceEndDate(LocalDate startDate) {
        return startDate.plusDays(6);
    }

    private String samplePerformancePoster(long index) {
        return "https://picsum.photos/seed/performance-%03d/400/600".formatted(index);
    }

    private String sampleVenueName(long index) {
        return "Sample Venue %03d".formatted(index);
    }

    private String sampleVenueLocation(long index) {
        return "City-%03d District".formatted(index);
    }

    private BigDecimal sampleSeatPrice(Schedule schedule, Seat seat, long offset) {
        int basePrice = 40_000;
        int rowBonus = seat.getRowNumber().charAt(0) - 'A';
        int gradeBonus = gradeBonus(seat.getVenueSection().getName());
        int performanceBonus = (int) (schedule.getPerformance().getId() % 5) * 5_000;
        return BigDecimal.valueOf(basePrice + gradeBonus + rowBonus * 5_000L + performanceBonus + offset % 10 * 1_000L);
    }

    private int gradeBonus(String gradeName) {
        return switch (gradeName) {
            case "VIP" -> 50_000;
            case "R" -> 30_000;
            case "S" -> 10_000;
            case "A" -> 5_000;
            default -> 0;
        };
    }

    private LocalDateTime sampleReservedAt(int index) {
        return LocalDateTime.now().minusDays(index % 30L).withHour(10).withMinute(0).withSecond(0).withNano(0);
    }

    private void ensureCoupon(Map<String, Coupon> existingByCode, String code, Supplier<Coupon> factory) {
        if (!existingByCode.containsKey(code)) {
            Coupon saved = couponRepository.save(factory.get());
            existingByCode.put(code, saved);
        }
    }
}
