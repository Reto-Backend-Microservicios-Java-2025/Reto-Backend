package pe.upc.edu.customerservice.domain.services;

import pe.upc.edu.customerservice.domain.model.aggregates.Client;
import pe.upc.edu.customerservice.domain.model.queries.GetAllClientsQuery;
import pe.upc.edu.customerservice.domain.model.queries.GetClientByUniqueCode;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ClientQueryService {
    Flux<Client> handle(GetAllClientsQuery query);
    Mono<Client> handle(GetClientByUniqueCode query);
}