package com.productmanager.service;

import com.productmanager.dto.ProductRequest;
import com.productmanager.dto.ProductResponse;
import com.productmanager.entity.Product;
import com.productmanager.exception.DuplicateResourceException;
import com.productmanager.exception.ResourceNotFoundException;
import com.productmanager.mapper.ProductMapper;
import com.productmanager.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService Tests")
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private ProductService productService;

    private Product testProduct;
    private ProductRequest testProductRequest;
    private ProductResponse testProductResponse;

    @BeforeEach
    void setUp() {
        testProduct = Product.builder()
                .id(1L)
                .name("Test Product")
                .description("Test Description")
                .price(new BigDecimal("99.99"))
                .quantity(100)
                .deleted(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testProductRequest = ProductRequest.builder()
                .name("Test Product")
                .description("Test Description")
                .price(new BigDecimal("99.99"))
                .quantity(100)
                .build();

        testProductResponse = ProductResponse.builder()
                .id(1L)
                .name("Test Product")
                .description("Test Description")
                .price(new BigDecimal("99.99"))
                .quantity(100)
                .available(true)
                .build();
    }

    @Nested
    @DisplayName("Get Product Tests")
    class GetProductTests {

        @Test
        @DisplayName("Should return product when found by ID")
        void getProductById_Success() {
            when(productRepository.findByIdAndNotDeleted(1L)).thenReturn(Optional.of(testProduct));
            when(productMapper.toResponse(testProduct)).thenReturn(testProductResponse);

            ProductResponse result = productService.getProductById(1L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getName()).isEqualTo("Test Product");
            verify(productRepository).findByIdAndNotDeleted(1L);
        }

        @Test
        @DisplayName("Should throw exception when product not found")
        void getProductById_NotFound() {
            when(productRepository.findByIdAndNotDeleted(anyLong())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.getProductById(999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Product");
        }
    }

    @Nested
    @DisplayName("Create Product Tests")
    class CreateProductTests {

        @Test
        @DisplayName("Should create product successfully")
        void createProduct_Success() {
            when(productRepository.existsByNameIgnoreCase(anyString())).thenReturn(false);
            when(productMapper.toEntity(testProductRequest)).thenReturn(testProduct);
            when(productRepository.save(any(Product.class))).thenReturn(testProduct);
            when(productMapper.toResponse(testProduct)).thenReturn(testProductResponse);

            ProductResponse result = productService.createProduct(testProductRequest);

            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("Test Product");
            verify(productRepository).save(any(Product.class));
        }

        @Test
        @DisplayName("Should throw exception when product name already exists")
        void createProduct_DuplicateName() {
            when(productRepository.existsByNameIgnoreCase("Test Product")).thenReturn(true);

            assertThatThrownBy(() -> productService.createProduct(testProductRequest))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("Product");
        }
    }

    @Nested
    @DisplayName("Update Product Tests")
    class UpdateProductTests {

        @Test
        @DisplayName("Should update product successfully")
        void updateProduct_Success() {
            when(productRepository.findByIdAndNotDeleted(1L)).thenReturn(Optional.of(testProduct));
            when(productRepository.existsByNameIgnoreCaseAndIdNot(anyString(), anyLong())).thenReturn(false);
            when(productRepository.save(any(Product.class))).thenReturn(testProduct);
            when(productMapper.toResponse(testProduct)).thenReturn(testProductResponse);

            ProductResponse result = productService.updateProduct(1L, testProductRequest);

            assertThat(result).isNotNull();
            verify(productMapper).updateEntity(testProduct, testProductRequest);
            verify(productRepository).save(testProduct);
        }

        @Test
        @DisplayName("Should throw exception when updating to duplicate name")
        void updateProduct_DuplicateName() {
            when(productRepository.findByIdAndNotDeleted(1L)).thenReturn(Optional.of(testProduct));
            when(productRepository.existsByNameIgnoreCaseAndIdNot("Test Product", 1L)).thenReturn(true);

            assertThatThrownBy(() -> productService.updateProduct(1L, testProductRequest))
                    .isInstanceOf(DuplicateResourceException.class);
        }
    }

    @Nested
    @DisplayName("Delete Product Tests")
    class DeleteProductTests {

        @Test
        @DisplayName("Should soft delete product successfully")
        void deleteProduct_Success() {
            when(productRepository.findByIdAndNotDeleted(1L)).thenReturn(Optional.of(testProduct));
            when(productRepository.save(any(Product.class))).thenReturn(testProduct);

            productService.deleteProduct(1L);

            assertThat(testProduct.getDeleted()).isTrue();
            verify(productRepository).save(testProduct);
        }

        @Test
        @DisplayName("Should throw exception when deleting non-existent product")
        void deleteProduct_NotFound() {
            when(productRepository.findByIdAndNotDeleted(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.deleteProduct(999L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Restore Product Tests")
    class RestoreProductTests {

        @Test
        @DisplayName("Should restore deleted product successfully")
        void restoreProduct_Success() {
            testProduct.setDeleted(true);
            when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
            when(productRepository.save(any(Product.class))).thenReturn(testProduct);
            when(productMapper.toResponse(testProduct)).thenReturn(testProductResponse);

            ProductResponse result = productService.restoreProduct(1L);

            assertThat(testProduct.getDeleted()).isFalse();
            verify(productRepository).save(testProduct);
        }
    }
}
