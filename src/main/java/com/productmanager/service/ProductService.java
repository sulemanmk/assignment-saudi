package com.productmanager.service;

import com.productmanager.dto.PageResponse;
import com.productmanager.dto.ProductRequest;
import com.productmanager.dto.ProductResponse;
import com.productmanager.dto.ProductSearchRequest;
import com.productmanager.entity.Product;
import com.productmanager.exception.DuplicateResourceException;
import com.productmanager.exception.ResourceNotFoundException;
import com.productmanager.mapper.ProductMapper;
import com.productmanager.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    @Transactional(readOnly = true)
    @Cacheable(value = "products", key = "#id")
    public ProductResponse getProductById(Long id) {
        log.debug("Fetching product with id: {}", id);
        Product product = findProductById(id);
        return productMapper.toResponse(product);
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getAllProducts(Pageable pageable) {
        log.debug("Fetching all products with pagination: {}", pageable);
        Page<Product> productPage = productRepository.findByDeletedFalse(pageable);
        return toPageResponse(productPage);
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> searchProducts(ProductSearchRequest searchRequest, Pageable pageable) {
        log.debug("Searching products with criteria: {}", searchRequest);
        Page<Product> productPage = productRepository.searchProducts(
                searchRequest.getName(),
                searchRequest.getMinPrice(),
                searchRequest.getMaxPrice(),
                searchRequest.getInStock(),
                pageable);
        return toPageResponse(productPage);
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getAvailableProducts(Pageable pageable) {
        log.debug("Fetching available products");
        Page<Product> productPage = productRepository.findAvailableProducts(pageable);
        return toPageResponse(productPage);
    }

    @CacheEvict(value = "products", allEntries = true)
    public ProductResponse createProduct(ProductRequest request) {
        log.info("Creating new product: {}", request.getName());

        if (productRepository.existsByNameIgnoreCase(request.getName())) {
            throw new DuplicateResourceException("Product", "name", request.getName());
        }

        Product product = productMapper.toEntity(request);
        Product savedProduct = productRepository.save(product);

        log.info("Product created successfully with id: {}", savedProduct.getId());
        return productMapper.toResponse(savedProduct);
    }

    @CachePut(value = "products", key = "#id")
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        log.info("Updating product with id: {}", id);

        Product product = findProductById(id);

        if (productRepository.existsByNameIgnoreCaseAndIdNot(request.getName(), id)) {
            throw new DuplicateResourceException("Product", "name", request.getName());
        }

        productMapper.updateEntity(product, request);
        Product updatedProduct = productRepository.save(product);

        log.info("Product updated successfully: {}", id);
        return productMapper.toResponse(updatedProduct);
    }

    @CacheEvict(value = "products", key = "#id")
    public void deleteProduct(Long id) {
        log.info("Soft deleting product with id: {}", id);

        Product product = findProductById(id);
        product.setDeleted(true);
        productRepository.save(product);

        log.info("Product soft deleted successfully: {}", id);
    }

    @CacheEvict(value = "products", key = "#id")
    public ProductResponse restoreProduct(Long id) {
        log.info("Restoring product with id: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));

        product.setDeleted(false);
        Product restoredProduct = productRepository.save(product);

        log.info("Product restored successfully: {}", id);
        return productMapper.toResponse(restoredProduct);
    }

    public Product findProductById(Long id) {
        return productRepository.findByIdAndNotDeleted(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
    }

    public List<Product> findProductsByIds(List<Long> ids) {
        List<Product> products = ids.stream()
                .map(this::findProductById)
                .collect(Collectors.toList());
        return products;
    }

    private PageResponse<ProductResponse> toPageResponse(Page<Product> productPage) {
        List<ProductResponse> content = productPage.getContent().stream()
                .map(productMapper::toResponse)
                .collect(Collectors.toList());

        return PageResponse.<ProductResponse>builder()
                .content(content)
                .page(productPage.getNumber())
                .size(productPage.getSize())
                .totalElements(productPage.getTotalElements())
                .totalPages(productPage.getTotalPages())
                .first(productPage.isFirst())
                .last(productPage.isLast())
                .hasNext(productPage.hasNext())
                .hasPrevious(productPage.hasPrevious())
                .build();
    }
}
