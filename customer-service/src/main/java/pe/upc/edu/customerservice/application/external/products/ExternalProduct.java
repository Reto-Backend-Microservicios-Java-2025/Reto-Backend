package pe.upc.edu.customerservice.application.external.products;

import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import pe.upc.edu.customerservice.interfaces.rest.resources.ProductResource;

import java.util.List;

@Component
public class ExternalProduct {
    private final RestTemplate restTemplate;

    public ExternalProduct(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public List<ProductResource> getProductsByClientId(Long clientId) {
        try {
            // URL del microservicio de productos
            String productServiceUrl = "http://localhost:8010/product-service";
            String url = productServiceUrl + "/api/v1/products/client/" + clientId;
            ProductResource[] products = restTemplate.getForObject(url, ProductResource[].class);
            return products != null ? List.of(products) : List.of();
        } catch (HttpClientErrorException.NotFound e) {
            return List.of(); // Cliente sin productos
        } catch (Exception e) {
            throw new RuntimeException("Error al obtener productos del cliente: " + e.getMessage());
        }
    }
}
