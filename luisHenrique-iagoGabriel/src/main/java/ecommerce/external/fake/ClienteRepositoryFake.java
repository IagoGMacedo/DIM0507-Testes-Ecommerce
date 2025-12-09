package ecommerce.external.fake;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery;

import ecommerce.entity.Cliente;
import ecommerce.repository.ClienteRepository;

public class ClienteRepositoryFake implements ClienteRepository {

    private final Map<Long, Cliente> banco = new HashMap<>();

    public void salvar(Cliente cliente) {
        banco.put(cliente.getId(), cliente);
    }

    @Override
    public Optional<Cliente> findById(Long aLong) {
        return Optional.ofNullable(banco.get(aLong));
    }

    @Override
    public <S extends Cliente> S save(S entity) {
        banco.put(entity.getId(), entity);
        return entity;
    }

    @Override
    public List<Cliente> findAll() {
        return new ArrayList<>(banco.values());
    }

    @Override
    public boolean existsById(Long aLong) {
        return banco.containsKey(aLong);
    }

    @Override
    public long count() {
        return banco.size();
    }

    @Override
    public void deleteById(Long aLong) {
        banco.remove(aLong);
    }

    @Override
    public void delete(Cliente entity) {
        banco.remove(entity.getId());
    }

    @Override
    public void deleteAll(Iterable<? extends Cliente> entities) {
        entities.forEach(e -> banco.remove(e.getId()));
    }

    @Override
    public void deleteAll() {
        banco.clear();
    }

    @Override
    public <S extends Cliente> List<S> saveAll(Iterable<S> entities) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Cliente> findAllById(Iterable<Long> longs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void flush() {
    }

    @Override
    public <S extends Cliente> S saveAndFlush(S entity) {
        return save(entity);
    }

    @Override
    public <S extends Cliente> List<S> saveAllAndFlush(Iterable<S> entities) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteAllInBatch(Iterable<Cliente> entities) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteAllByIdInBatch(Iterable<Long> longs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteAllInBatch() {
        banco.clear();
    }

    @Override
    public Cliente getOne(Long aLong) {
        return banco.get(aLong);
    }

    @Override
    public Cliente getById(Long aLong) {
        return banco.get(aLong);
    }

    @Override
    public Cliente getReferenceById(Long aLong) {
        return banco.get(aLong);
    }

    @Override
    public <S extends Cliente> List<S> findAll(Example<S> example) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'findAll'");
    }

    @Override
    public <S extends Cliente> List<S> findAll(Example<S> example, Sort sort) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'findAll'");
    }

    @Override
    public void deleteAllById(Iterable<? extends Long> ids) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'deleteAllById'");
    }

    @Override
    public List<Cliente> findAll(Sort sort) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'findAll'");
    }

    @Override
    public Page<Cliente> findAll(Pageable pageable) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'findAll'");
    }

    @Override
    public <S extends Cliente> long count(Example<S> example) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'count'");
    }

    @Override
    public <S extends Cliente> boolean exists(Example<S> example) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'exists'");
    }

    @Override
    public <S extends Cliente> Page<S> findAll(Example<S> example, Pageable pageable) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'findAll'");
    }

    @Override
    public <S extends Cliente, R> R findBy(Example<S> example, Function<FetchableFluentQuery<S>, R> queryFunction) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'findBy'");
    }

    @Override
    public <S extends Cliente> Optional<S> findOne(Example<S> example) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'findOne'");
    }

}
