package com.example.storefront.catalog;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class ProductService {
    private final List<Product> products = List.of(
            new Product(1, "Notebook", new BigDecimal("4.50")),
            new Product(2, "Pen", new BigDecimal("1.25")));

    public List<Product> listProducts() {
        return products;
    }

    public Optional<Product> findProduct(long id) {
        return products.stream().filter(product -> product.id() == id).findFirst();
    }
}
