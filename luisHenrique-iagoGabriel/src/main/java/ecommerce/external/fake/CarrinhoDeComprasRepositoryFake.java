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

import ecommerce.entity.CarrinhoDeCompras;
import ecommerce.entity.Cliente;
import ecommerce.repository.CarrinhoDeComprasRepository;

public class CarrinhoDeComprasRepositoryFake implements CarrinhoDeComprasRepository {
    private final Map<Long, CarrinhoDeCompras> banco = new HashMap<>();

    public void salvar(CarrinhoDeCompras carrinho) {
        banco.put(carrinho.getId(), carrinho);
    }

    @Override
    public Optional<CarrinhoDeCompras> findByIdAndCliente(Long id, Cliente cliente) {
        CarrinhoDeCompras c = banco.get(id);
        if (c != null && c.getCliente() != null && c.getCliente().getId().equals(cliente.getId())) {
            return Optional.of(c);
        }
        return Optional.empty();
    }

    @Override
    public Optional<CarrinhoDeCompras> findById(Long aLong) {
        return Optional.ofNullable(banco.get(aLong));
    }

    @Override
    public <S extends CarrinhoDeCompras> S save(S entity) {
        banco.put(entity.getId(), entity);
        return entity;
    }

    @Override
    public List<CarrinhoDeCompras> findAll() {
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
    public void delete(CarrinhoDeCompras entity) {
        banco.remove(entity.getId());
    }

    @Override
    public void deleteAll(Iterable<? extends CarrinhoDeCompras> entities) {
        entities.forEach(e -> banco.remove(e.getId()));
    }

    @Override
    public void deleteAll() {
        banco.clear();
    }

    @Override
    public void deleteAllByIdInBatch(Iterable<Long> ids) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'deleteAllByIdInBatch'");
    }

    @Override
    public void deleteAllInBatch() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'deleteAllInBatch'");
    }

    @Override
    public void deleteAllInBatch(Iterable<CarrinhoDeCompras> entities) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'deleteAllInBatch'");
    }

    @Override
    public <S extends CarrinhoDeCompras> List<S> findAll(Example<S> example) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'findAll'");
    }

    @Override
    public <S extends CarrinhoDeCompras> List<S> findAll(Example<S> example, Sort sort) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'findAll'");
    }

    @Override
    public void flush() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'flush'");
    }

    @Override
    public CarrinhoDeCompras getById(Long arg0) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getById'");
    }

    @Override
    public CarrinhoDeCompras getOne(Long arg0) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getOne'");
    }

    @Override
    public CarrinhoDeCompras getReferenceById(Long id) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getReferenceById'");
    }

    @Override
    public <S extends CarrinhoDeCompras> List<S> saveAllAndFlush(Iterable<S> entities) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'saveAllAndFlush'");
    }

    @Override
    public <S extends CarrinhoDeCompras> S saveAndFlush(S entity) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'saveAndFlush'");
    }

    @Override
    public List<CarrinhoDeCompras> findAllById(Iterable<Long> ids) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'findAllById'");
    }

    @Override
    public <S extends CarrinhoDeCompras> List<S> saveAll(Iterable<S> entities) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'saveAll'");
    }

    @Override
    public void deleteAllById(Iterable<? extends Long> ids) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'deleteAllById'");
    }

    @Override
    public List<CarrinhoDeCompras> findAll(Sort sort) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'findAll'");
    }

    @Override
    public Page<CarrinhoDeCompras> findAll(Pageable pageable) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'findAll'");
    }

    @Override
    public <S extends CarrinhoDeCompras> long count(Example<S> example) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'count'");
    }

    @Override
    public <S extends CarrinhoDeCompras> boolean exists(Example<S> example) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'exists'");
    }

    @Override
    public <S extends CarrinhoDeCompras> Page<S> findAll(Example<S> example, Pageable pageable) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'findAll'");
    }

    @Override
    public <S extends CarrinhoDeCompras, R> R findBy(Example<S> example,
            Function<FetchableFluentQuery<S>, R> queryFunction) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'findBy'");
    }

    @Override
    public <S extends CarrinhoDeCompras> Optional<S> findOne(Example<S> example) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'findOne'");
    }
}
