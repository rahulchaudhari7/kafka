package com.example.storefront.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ProductServiceTest {
    private final ProductService productService = new ProductService();

    @Test
    void listsProductsWithExpectedFields() {
        assertThat(productService.listProducts())
                .containsExactly(
                        new Product(1, "Notebook", new BigDecimal("4.50")),
                        new Product(2, "Pen", new BigDecimal("1.25")));
    }

    @Test
    void findsProductsByIdAndReturnsEmptyForUnknownIds() {
        assertThat(productService.findProduct(2))
                .contains(new Product(2, "Pen", new BigDecimal("1.25")));
        assertThat(productService.findProduct(999)).isEmpty();
    }
}
