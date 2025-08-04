package pe.upc.edu.customerservice.application.internal;

import org.springframework.stereotype.Service;
import pe.upc.edu.customerservice.domain.model.aggregates.Client;
import pe.upc.edu.customerservice.domain.model.commands.CreateClientCommand;
import pe.upc.edu.customerservice.domain.model.commands.DeleteClientCommand;
import pe.upc.edu.customerservice.domain.model.commands.UpdateClientCommand;
import pe.upc.edu.customerservice.domain.services.ClientCommandService;
import pe.upc.edu.customerservice.infrastructure.persistence.r2dbc.repositories.ClientRepository;
import reactor.core.publisher.Mono;

@Service
public class ClientCommandServiceImpl implements ClientCommandService {

    private final ClientRepository clientRepository;

    public ClientCommandServiceImpl(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    @Override
    public Mono<Long> handle(CreateClientCommand command) {
        return clientRepository.existsByFullName(command.full_name())
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new IllegalArgumentException("Client with full name " + command.full_name() + " already exists"));
                    }

                    var client = new Client(command);
                    return clientRepository.save(client)
                            .map(Client::getUniqueCode)
                            .onErrorMap(e -> new IllegalArgumentException("Error while saving client: " + e.getMessage()));
                });
    }

    @Override
    public Mono<Client> handle(UpdateClientCommand command) {
        return clientRepository.existsByFullName(command.full_name())
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new IllegalArgumentException("Client with full name " + command.full_name() + " already exists"));
                    }
                    return clientRepository.existsById(command.clientId());
                })
                .flatMap(exists -> {
                    if (!exists) {
                        return Mono.error(new IllegalArgumentException("Profile with id " + command.clientId() + " does not exist"));
                    }
                    return clientRepository.findById(command.clientId());
                })
                .flatMap(clientToUpdate -> {
                    clientToUpdate.updateInformation(
                            command.full_name(),
                            command.full_last_name(),
                            command.type_document(),
                            command.number_document(),
                            Long.valueOf(command.uniqueCode())
                    );
                    return clientRepository.save(clientToUpdate)
                            .onErrorMap(e -> new IllegalArgumentException("Error while updating client: " + e.getMessage()));
                });
    }

    @Override
    public Mono<Void> handle(DeleteClientCommand command) {
        return clientRepository.existsById(command.clientId())
                .flatMap(exists -> {
                    if (!exists) {
                        return Mono.error(new IllegalArgumentException("Client with id " + command.clientId() + " does not exist"));
                    }
                    return clientRepository.deleteById(command.clientId())
                            .onErrorMap(e -> new IllegalArgumentException("Error while deleting client: " + e.getMessage()));
                });
    }
}