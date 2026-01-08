package com.productmanager.repository;

import com.productmanager.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // Find by ID excluding soft-deleted products
    @Query("SELECT p FROM Product p WHERE p.id = :id AND p.deleted = false")
    Optional<Product> findByIdAndNotDeleted(@Param("id") Long id);

    // Find all non-deleted products with pagination
    Page<Product> findByDeletedFalse(Pageable pageable);

    // Find all non-deleted products
    List<Product> findByDeletedFalse();

    // Search by name (case-insensitive)
    @Query("SELECT p FROM Product p WHERE p.deleted = false AND LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    Page<Product> findByNameContainingIgnoreCase(@Param("name") String name, Pageable pageable);

    // Search by price range
    @Query("SELECT p FROM Product p WHERE p.deleted = false AND p.price BETWEEN :minPrice AND :maxPrice")
    Page<Product> findByPriceBetween(@Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            Pageable pageable);

    // Search by availability (in stock)
    @Query("SELECT p FROM Product p WHERE p.deleted = false AND p.quantity > 0")
    Page<Product> findAvailableProducts(Pageable pageable);

    // Combined search with multiple criteria named query
    @Query("SELECT p FROM Product p WHERE p.deleted = false " +
            "AND (:name IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))) " +
            "AND (:minPrice IS NULL OR p.price >= :minPrice) " +
            "AND (:maxPrice IS NULL OR p.price <= :maxPrice) " +
            "AND (:inStock IS NULL OR (:inStock = true AND p.quantity > 0) OR (:inStock = false AND p.quantity = 0))")
    Page<Product> searchProducts(@Param("name") String name,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("inStock") Boolean inStock,
            Pageable pageable);

    // Check if product name exists
    boolean existsByNameIgnoreCase(String name);

    // Check if product name exists excluding a specific ID
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM Product p WHERE LOWER(p.name) = LOWER(:name) AND p.id != :id")
    boolean existsByNameIgnoreCaseAndIdNot(@Param("name") String name, @Param("id") Long id);
}
