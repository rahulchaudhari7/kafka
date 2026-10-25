package com.example.storefront.catalog;

import java.math.BigDecimal;

public record Product(long id, String name, BigDecimal price) {
}
