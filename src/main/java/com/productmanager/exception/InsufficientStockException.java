package com.productmanager.exception;

public class InsufficientStockException extends RuntimeException {

    private final Long productId;
    private final String productName;
    private final int requestedQuantity;
    private final int availableQuantity;

    public InsufficientStockException(Long productId, String productName, int requestedQuantity,
            int availableQuantity) {
        super(String.format("Insufficient stock for product '%s' (ID: %d). Requested: %d, Available: %d",
                productName, productId, requestedQuantity, availableQuantity));
        this.productId = productId;
        this.productName = productName;
        this.requestedQuantity = requestedQuantity;
        this.availableQuantity = availableQuantity;
    }

    public Long getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public int getRequestedQuantity() {
        return requestedQuantity;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }
}
