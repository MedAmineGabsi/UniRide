package ca.mcgill.ecse428.UniRide_Backend.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import ca.mcgill.ecse428.UniRide_Backend.model.Ride;
import ca.mcgill.ecse428.UniRide_Backend.model.RideStatus;
import jakarta.persistence.criteria.Predicate;

/**
 * Search filters for rides (Feature ID04). Use with a repository that extends
 * {@code JpaSpecificationExecutor<Ride>}, e.g. {@code findAll(openRidesMatching(...), BY_DEPARTURE_TIME)}.
 */
public final class RideSpecifications {

    /** Search results are listed earliest departure first. */
    public static final Sort BY_DEPARTURE_TIME = Sort.by(Sort.Direction.ASC, "departureDateTime");

    private RideSpecifications() {
    }

    /**
     * Open rides only. Each criterion is optional: a null or blank value means no filter on it.
     * Origin and destination match ignoring case and surrounding spaces; the date matches any
     * departure time on that day.
     */
    public static Specification<Ride> openRidesMatching(String origin, String destination, LocalDate departureDate) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("status"), RideStatus.OPEN));
            if (origin != null && !origin.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("origin")), normalize(origin)));
            }
            if (destination != null && !destination.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("destination")), normalize(destination)));
            }
            if (departureDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.<LocalDateTime>get("departureDateTime"),
                        departureDate.atStartOfDay()));
                predicates.add(cb.lessThan(root.<LocalDateTime>get("departureDateTime"),
                        departureDate.plusDays(1).atStartOfDay()));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
