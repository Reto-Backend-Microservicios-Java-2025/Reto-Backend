package pe.upc.edu.customerservice.application.internal;

import org.springframework.stereotype.Service;
import pe.upc.edu.customerservice.domain.model.aggregates.Client;
import pe.upc.edu.customerservice.domain.model.queries.GetAllClientsQuery;
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
}