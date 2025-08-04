package pe.upc.edu.customerservice.application.internal;

import org.springframework.stereotype.Service;
import pe.upc.edu.customerservice.domain.model.aggregates.Client;
import pe.upc.edu.customerservice.domain.model.queries.GetAllClientsQuery;
import pe.upc.edu.customerservice.domain.model.queries.GetClientByIdQuery;
import pe.upc.edu.customerservice.domain.model.queries.GetClientByUniqueCode;
import pe.upc.edu.customerservice.domain.services.ClientQueryService;
import pe.upc.edu.customerservice.infrastructure.persistence.r2dbc.repositories.ClientRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class ClientQueryServiceImpl implements ClientQueryService {

    private final ClientRepository clientRepository;

    public ClientQueryServiceImpl(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    @Override
    public Flux<Client> handle(GetAllClientsQuery query) {
        return clientRepository.findAll();
    }

    @Override
    public Mono<Client> handle(GetClientByUniqueCode query) {
        return clientRepository.findByUniqueCode(query.uniqueCode());
    }

    @Override
    public Mono<Client> handle(GetClientByIdQuery query) {
        if (query.id() == null || query.id() <= 0) {
            return Mono.error(new IllegalArgumentException("Client ID must be a positive number"));
        }

        return clientRepository.findById(query.id())
                .switchIfEmpty(Mono.error(new RuntimeException("Client not found with ID: " + query.id())))
                .onErrorResume(throwable -> Mono.error(new RuntimeException("Failed to retrieve client", throwable)));
    }
}