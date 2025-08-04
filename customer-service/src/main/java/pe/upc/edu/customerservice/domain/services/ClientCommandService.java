package pe.upc.edu.customerservice.domain.services;

import pe.upc.edu.customerservice.domain.model.aggregates.Client;
import pe.upc.edu.customerservice.domain.model.commands.CreateClientCommand;
import pe.upc.edu.customerservice.domain.model.commands.DeleteClientCommand;
import pe.upc.edu.customerservice.domain.model.commands.UpdateClientCommand;
import reactor.core.publisher.Mono;

public interface ClientCommandService {
    Mono<Long> handle(CreateClientCommand command);
    Mono<Client> handle(UpdateClientCommand command);
    Mono<Void> handle(DeleteClientCommand command);
}