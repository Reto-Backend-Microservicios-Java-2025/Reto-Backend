package pe.upc.edu.productservice.interfaces.rest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.upc.edu.productservice.domain.model.commands.DeleteProductCommand;
import pe.upc.edu.productservice.domain.model.queries.GetAllProductsQuery;
import pe.upc.edu.productservice.domain.model.queries.GetProductByIdQuery;
import pe.upc.edu.productservice.domain.services.ProductCommandService;
import pe.upc.edu.productservice.domain.services.ProductQueryService;
import pe.upc.edu.productservice.interfaces.rest.resources.CreateProductResource;
import pe.upc.edu.productservice.interfaces.rest.resources.ProductResource;
import pe.upc.edu.productservice.interfaces.rest.resources.UpdateProductResource;
import pe.upc.edu.productservice.interfaces.rest.transform.CreateProductCommandFromResourceAssembler;
import pe.upc.edu.productservice.interfaces.rest.transform.ProductResourceFromEntityAssembler;
import pe.upc.edu.productservice.interfaces.rest.transform.UpdateProductCommandFromResourceAssembler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(value = "/api/v1/products", produces = APPLICATION_JSON_VALUE)
public class ProductsController {

    private final ProductQueryService productQueryService;
    private final ProductCommandService productCommandService;

    public ProductsController(ProductQueryService productQueryService, ProductCommandService productCommandService) {
        this.productQueryService = productQueryService;
        this.productCommandService = productCommandService;
    }

    @PostMapping
    public ResponseEntity<?> createProduct(@RequestBody CreateProductResource createProductResource) {
        try {
            var createProductCommand = CreateProductCommandFromResourceAssembler.toCommandFromResource(createProductResource);
            var productId = productCommandService.handle(createProductCommand);

            if (productId == 0L) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Failed to create product", "Invalid product data provided"));
            }

            var getProductByIdQuery = new GetProductByIdQuery(productId);
            var product = productQueryService.handle(getProductByIdQuery);

            if (product.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Product creation failed", "Could not retrieve created product"));
            }

            var productResource = ProductResourceFromEntityAssembler.toResourceFromEntity(product.get());
            return new ResponseEntity<>(productResource, HttpStatus.CREATED);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(createErrorResponse("Invalid input", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Internal server error", "An unexpected error occurred"));
        }
    }

    @GetMapping
    public ResponseEntity<List<ProductResource>> getAllProducts() {
        try {
            var getAllProductsQuery = new GetAllProductsQuery();
            var products = productQueryService.handle(getAllProductsQuery);
            var productResources = products.stream()
                    .map(ProductResourceFromEntityAssembler::toResourceFromEntity)
                    .toList();
            return ResponseEntity.ok(productResources);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    @GetMapping("/{productId}")
    public ResponseEntity<?> getProductById(@PathVariable Long productId) {
        try {
            if (productId == null || productId <= 0) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Invalid product ID", "Product ID must be a positive number"));
            }

            var getProductByIdQuery = new GetProductByIdQuery(productId);
            var product = productQueryService.handle(getProductByIdQuery);

            if (product.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(createErrorResponse("Product not found", "No product found with ID: " + productId));
            }

            var productResource = ProductResourceFromEntityAssembler.toResourceFromEntity(product.get());
            return ResponseEntity.ok(productResource);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Internal server error", "An unexpected error occurred"));
        }
    }

    @PutMapping("/{productId}")
    public ResponseEntity<?> updateProduct(@PathVariable Long productId, @RequestBody UpdateProductResource updateProductResource) {
        try {
            if (productId == null || productId <= 0) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Invalid product ID", "Product ID must be a positive number"));
            }

            var updateProductCommand = UpdateProductCommandFromResourceAssembler.toCommandFromResource(productId, updateProductResource);
            var updatedProduct = productCommandService.handle(updateProductCommand);

            if (updatedProduct.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(createErrorResponse("Update failed", "Could not update product with ID: " + productId));
            }

            var productResource = ProductResourceFromEntityAssembler.toResourceFromEntity(updatedProduct.get());
            return ResponseEntity.ok(productResource);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(createErrorResponse("Invalid input", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Internal server error", "An unexpected error occurred"));
        }
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<?> deleteProduct(@PathVariable Long productId) {
        try {
            if (productId == null || productId <= 0) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Invalid product ID", "Product ID must be a positive number"));
            }

            var deleteProductCommand = new DeleteProductCommand(productId);
            productCommandService.handle(deleteProductCommand);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Product with ID " + productId + " successfully deleted");
            response.put("status", "success");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Delete failed", "Could not delete product with ID: " + productId));
        }
    }

    /**
     * Helper method to create consistent error response format
     */
    private Map<String, String> createErrorResponse(String error, String message) {
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", error);
        errorResponse.put("message", message);
        errorResponse.put("status", "error");
        return errorResponse;
    }
}