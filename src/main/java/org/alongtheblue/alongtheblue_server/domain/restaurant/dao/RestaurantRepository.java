    package org.alongtheblue.alongtheblue_server.domain.restaurant.dao;

    import org.alongtheblue.alongtheblue_server.domain.global.SimpleInformation;
    import org.alongtheblue.alongtheblue_server.domain.restaurant.domain.Restaurant;
    import org.alongtheblue.alongtheblue_server.domain.search.domain.SearchInformation;
    import org.springframework.data.domain.Page;
    import org.springframework.data.domain.Pageable;
    import org.springframework.data.jpa.repository.JpaRepository;
    import org.springframework.data.jpa.repository.Query;

    import java.util.Optional;

    public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {
        @Query("SELECT r FROM Restaurant r JOIN r.images i GROUP BY r HAVING COUNT(i) > 0")
        Page<SimpleInformation> findAllSimple(Pageable pageable);

        @Query("SELECT r FROM Restaurant r JOIN r.images i WHERE r.title LIKE %:keyword% GROUP BY r HAVING COUNT(i) > 0")
        Page<SearchInformation> findByTitleContaining(String keyword, Pageable pageable);

        Page<Restaurant> findAll(Pageable pageable);

        Optional<Restaurant> findByContentId(String contentId);
    }
