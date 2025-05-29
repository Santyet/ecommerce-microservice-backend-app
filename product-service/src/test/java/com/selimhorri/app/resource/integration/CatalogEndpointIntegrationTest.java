package com.selimhorri.app.resource.integration;

import com.selimhorri.app.dto.ProductDto;
import com.selimhorri.app.dto.CategoryDto;
import com.selimhorri.app.dto.response.collection.DtoCollectionResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.DefaultResponseErrorHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class CatalogEndpointIntegrationTest {

    @LocalServerPort
    private int randomPort;

    @Autowired
    private TestRestTemplate apiClient;

    private String endpointBase;

    @BeforeEach
    void configureClient() {
        this.endpointBase = "http://localhost:" + randomPort + "/product-service/api/products";
        apiClient.getRestTemplate().setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            public void handleError(ClientHttpResponse response) throws IOException {
                String body = StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8);
                System.err.println("Custom error handler response: " + body);
                super.handleError(response);
            }
        });
    }

    @Test
    void shouldRetrieveProductList() {
        var response = apiClient.getForObject(endpointBase, DtoCollectionResponse.class);
        assertNotNull(response, "Expected non-null response");
        assertFalse(response.getCollection().isEmpty(), "Expected products to exist in collection");
    }

    @Test
    void shouldFindProductById() {
        var url = endpointBase + "/1";
        var product = apiClient.getForObject(url, ProductDto.class);

        assertNotNull(product, "Expected valid product");
        assertAll(
            () -> assertNotNull(product.getProductId(), "Missing product ID"),
            () -> assertNotNull(product.getProductTitle(), "Missing title"),
            () -> assertNotNull(product.getImageUrl(), "Missing image"),
            () -> assertNotNull(product.getSku(), "Missing SKU"),
            () -> assertNotNull(product.getPriceUnit(), "Missing price"),
            () -> assertNotNull(product.getQuantity(), "Missing quantity")
        );
    }

    @Test
    void shouldCreateNewProduct() {
        var requestDto = ProductDto.builder()
                .productTitle("Nueva Galleta")
                .sku("SKU-GAL-101")
                .priceUnit(59.90)
                .quantity(12)
                .imageUrl("https://img.example.com/galleta.jpg")
                .categoryDto(CategoryDto.builder().categoryId(1).build())
                .build();

        var created = apiClient.postForObject(endpointBase, requestDto, ProductDto.class);

        assertNotNull(created);
        assertEquals("Nueva Galleta", created.getProductTitle());
        assertNotNull(created.getProductId());
    }

    @Test
    void shouldUpdateProductWithoutExplicitId() {
        var nuevoProducto = ProductDto.builder()
                .productTitle("Barra Energética")
                .sku("BAR-ENERGY-001")
                .priceUnit(21.99)
                .quantity(5)
                .imageUrl("https://img.example.com/barra.jpg")
                .categoryDto(CategoryDto.builder().categoryId(1).build())
                .build();

        var saved = apiClient.postForObject(endpointBase, nuevoProducto, ProductDto.class);
        assertNotNull(saved);
        assertNotNull(saved.getProductId());

        saved.setProductTitle("Barra de Proteína");
        saved.setPriceUnit(29.99);

        var updateEntity = new HttpEntity<>(saved);
        var updated = apiClient.exchange(endpointBase, HttpMethod.PUT, updateEntity, ProductDto.class).getBody();

        assertNotNull(updated);
        assertEquals("Barra de Proteína", updated.getProductTitle());
        assertEquals(29.99, updated.getPriceUnit());
    }

    @Test
    void shouldRemoveProductById() {
        int productId = 3; // Se asume que este ID existe previamente
        var deleteEndpoint = endpointBase + "/" + productId;
        apiClient.delete(deleteEndpoint);

        var response = apiClient.getForObject(endpointBase, DtoCollectionResponse.class);
        assertNotNull(response);
        boolean exists = response.getCollection().stream()
                .anyMatch(prod -> prod instanceof LinkedHashMap && productId == ((Number)((LinkedHashMap<?, ?>) prod).get("productId")).intValue());

        assertFalse(exists, "Expected product to be deleted");
    }
}
