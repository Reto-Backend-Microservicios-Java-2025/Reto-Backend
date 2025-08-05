package pe.upc.edu.productservice.interfaces.rest;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pe.upc.edu.productservice.domain.model.commands.DeleteProductCommand;
import pe.upc.edu.productservice.domain.model.queries.GetAllProductsQuery;
import pe.upc.edu.productservice.domain.model.queries.GetProductByIdQuery;
import pe.upc.edu.productservice.domain.model.queries.GetProductsByClientIdQuery;
import pe.upc.edu.productservice.domain.services.ProductCommandService;
import pe.upc.edu.productservice.domain.services.ProductQueryService;
import pe.upc.edu.productservice.interfaces.rest.resources.CreateProductResource;
import pe.upc.edu.productservice.interfaces.rest.resources.ProductResource;
import pe.upc.edu.productservice.interfaces.rest.resources.UpdateProductResource;
import pe.upc.edu.productservice.interfaces.rest.transform.CreateProductCommandFromResourceAssembler;
import pe.upc.edu.productservice.interfaces.rest.transform.ProductResourceFromEntityAssembler;
import pe.upc.edu.productservice.interfaces.rest.transform.UpdateProductCommandFromResourceAssembler;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(value = "/api/v1/products", produces = APPLICATION_JSON_VALUE)
@Tag(name = "Products", description = "Product Endpoints")
public class ProductsController {

    private final ProductQueryService productQueryService;
    private final ProductCommandService productCommandService;

    public ProductsController(ProductQueryService productQueryService, ProductCommandService productCommandService) {
        this.productQueryService = productQueryService;
        this.productCommandService = productCommandService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ProductResource> createProduct(@RequestBody CreateProductResource createProductResource) {
        var createProductCommand = CreateProductCommandFromResourceAssembler.toCommandFromResource(createProductResource);

        return productCommandService.handle(createProductCommand)
                .filter(productId -> productId > 0L)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Failed to create product")))
                .flatMap(productId -> {
                    var getProductByIdQuery = new GetProductByIdQuery(productId);
                    return productQueryService.handle(getProductByIdQuery);
                })
                .map(ProductResourceFromEntityAssembler::toResourceFromEntity)
                .onErrorMap(IllegalArgumentException.class, ex -> ex)
                .onErrorMap(throwable -> new RuntimeException("An unexpected error occurred", throwable));
    }

    @GetMapping
    public Flux<ProductResource> getAllProducts() {
        var getAllProductsQuery = new GetAllProductsQuery();
        return productQueryService.handle(getAllProductsQuery)
                .map(ProductResourceFromEntityAssembler::toResourceFromEntity)
                .onErrorResume(throwable -> Flux.error(new RuntimeException("Failed to retrieve products", throwable)));
    }

    @GetMapping("/{productId}")
    public Mono<ProductResource> getProductById(@PathVariable Long productId) {
        var getProductByIdQuery = new GetProductByIdQuery(productId);
        return productQueryService.handle(getProductByIdQuery)
                .map(ProductResourceFromEntityAssembler::toResourceFromEntity)
                .switchIfEmpty(Mono.error(new RuntimeException("Product not found with ID: " + productId)))
                .onErrorMap(IllegalArgumentException.class, ex -> ex)
                .onErrorMap(throwable -> new RuntimeException("Failed to retrieve product", throwable));
    }

    @GetMapping("/client/{clientId}")
    public Flux<ProductResource> getProductsByClientId(@PathVariable Long clientId) {
        var query = new GetProductsByClientIdQuery(clientId);
        return productQueryService.handle(query)
                .map(ProductResourceFromEntityAssembler::toResourceFromEntity);
    }

    @PutMapping("/{productId}")
    public Mono<ProductResource> updateProduct(@PathVariable Long productId,
                                               @RequestBody UpdateProductResource updateProductResource) {
        var updateProductCommand = UpdateProductCommandFromResourceAssembler
                .toCommandFromResource(productId, updateProductResource);

        return productCommandService.handle(updateProductCommand)
                .map(ProductResourceFromEntityAssembler::toResourceFromEntity)
                .onErrorMap(IllegalArgumentException.class, ex -> ex)
                .onErrorMap(throwable -> new RuntimeException("Failed to update product", throwable));
    }

    @DeleteMapping("/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteProduct(@PathVariable Long productId) {
        var deleteProductCommand = new DeleteProductCommand(productId);
        return productCommandService.handle(deleteProductCommand)
                .onErrorResume(IllegalArgumentException.class, Mono::error)
                .onErrorResume(throwable -> Mono.error(new RuntimeException("Failed to delete product", throwable)));
    }
}