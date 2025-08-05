package pe.upc.edu.customerservice.interfaces.rest;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import pe.upc.edu.customerservice.application.external.products.ReactiveExternalProduct;
import pe.upc.edu.customerservice.domain.model.queries.GetAllClientsQuery;
import pe.upc.edu.customerservice.domain.model.queries.GetClientByIdQuery;
import pe.upc.edu.customerservice.domain.model.queries.GetClientByUniqueCode;
import pe.upc.edu.customerservice.domain.services.ClientCommandService;
import pe.upc.edu.customerservice.domain.services.ClientQueryService;
import pe.upc.edu.customerservice.interfaces.rest.resources.ClientResource;
import pe.upc.edu.customerservice.interfaces.rest.resources.ClientWithProductsResource;
import pe.upc.edu.customerservice.interfaces.rest.resources.CreateClientResource;
import pe.upc.edu.customerservice.interfaces.rest.transform.ClientResourceFromEntityAssembler;
import pe.upc.edu.customerservice.interfaces.rest.transform.CreateClientCommandFromResourceAssembler;
import pe.upc.edu.customerservice.infrastructure.EncryptionUtil;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(value = "/api/v1/clients", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Clients", description = "Client Endpoints")
public class ClientController {

    private final ClientQueryService clientQueryService;
    private final ClientCommandService clientCommandService;
    private final ReactiveExternalProduct reactiveExternalProduct;

    public ClientController(ClientQueryService clientQueryService,
                            ClientCommandService clientCommandService,
                            ReactiveExternalProduct reactiveExternalProduct) {
        this.clientQueryService = clientQueryService;
        this.clientCommandService = clientCommandService;
        this.reactiveExternalProduct = reactiveExternalProduct;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ClientResource> createClient(@RequestBody CreateClientResource resource) {
        var createClientCommand = CreateClientCommandFromResourceAssembler.toCommandFromResource(resource);

        return clientCommandService.handle(createClientCommand)
                .flatMap(uniqueCode -> {
                    var getClientByUniqueCode = new GetClientByUniqueCode(uniqueCode);
                    return clientQueryService.handle(getClientByUniqueCode);
                })
                .map(ClientResourceFromEntityAssembler::toResourceFromEntity);
    }

    @GetMapping
    public Flux<ClientResource> getAllClients() {
        var getAllClientsQuery = new GetAllClientsQuery();
        return clientQueryService.handle(getAllClientsQuery)
                .map(ClientResourceFromEntityAssembler::toResourceFromEntity);
    }

    @GetMapping("/{encryptedCode}")
    public Mono<ClientWithProductsResource> getClientByEncryptedCode(@PathVariable String encryptedCode) {
        return Mono.fromCallable(() -> {
                    String decrypted = EncryptionUtil.decrypt(encryptedCode);
                    return Long.valueOf(decrypted);
                })
                .flatMap(uniqueCode -> {
                    var query = new GetClientByUniqueCode(uniqueCode);
                    return clientQueryService.handle(query);
                })
                .flatMap(client -> {
                    // Obtener productos del cliente de forma asíncrona y esperar el resultado
                    return reactiveExternalProduct.getProductsByClientId(client.getId())
                            .map(products -> new ClientWithProductsResource(
                                    client.getId(),
                                    client.getFullName(),
                                    client.getFullLastName(),
                                    client.getTypedocument().toString(),
                                    client.getDocumentNumber(),
                                    client.getUniqueCode(),
                                    products
                            ));
                })
                .onErrorResume(throwable -> Mono.empty());
    }

    // Endpoint adicional para obtener solo la info del cliente (sin productos)
    @GetMapping("/{encryptedCode}/basic")
    public Mono<ClientResource> getClientBasicByEncryptedCode(@PathVariable String encryptedCode) {
        return Mono.fromCallable(() -> {
                    String decrypted = EncryptionUtil.decrypt(encryptedCode);
                    return Long.valueOf(decrypted);
                })
                .flatMap(uniqueCode -> {
                    var query = new GetClientByUniqueCode(uniqueCode);
                    return clientQueryService.handle(query);
                })
                .map(ClientResourceFromEntityAssembler::toResourceFromEntity)
                .onErrorResume(throwable -> Mono.empty());
    }

    // Get Client By id
    @GetMapping("/id/{clientId}")
    public Mono<ClientResource> getClientById(@PathVariable Long clientId) {
        var query = new GetClientByIdQuery(clientId);
        return clientQueryService.handle(query)
                .map(ClientResourceFromEntityAssembler::toResourceFromEntity)
                .switchIfEmpty(Mono.error(new RuntimeException("Client not found with ID: " + clientId)))
                .onErrorMap(IllegalArgumentException.class, ex -> ex)
                .onErrorMap(throwable -> new RuntimeException("Failed to retrieve client", throwable));
    }
}