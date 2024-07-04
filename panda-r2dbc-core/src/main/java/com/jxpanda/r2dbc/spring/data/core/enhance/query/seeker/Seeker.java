package com.jxpanda.r2dbc.spring.data.core.enhance.query.seeker;

import com.jxpanda.r2dbc.spring.data.core.enhance.query.criteria.EnhancedCriteria;
import com.jxpanda.r2dbc.spring.data.core.enhance.query.page.Pagination;
import com.jxpanda.r2dbc.spring.data.core.enhance.query.seeker.domain.Extend;
import com.jxpanda.r2dbc.spring.data.core.enhance.query.seeker.domain.Rule;
import com.jxpanda.r2dbc.spring.data.core.enhance.query.seeker.domain.Sorting;
import lombok.Data;
import lombok.NonNull;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.relational.core.query.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Data
public class Seeker<T> {

    /**
     * 筛选条件列表（探机群）
     */
    private final List<Probe> probes;

    /**
     * 排序条件列表（分拣机群）
     */
    private final List<Sorter> sorters;

    /**
     * 分页对象
     */
    private final Pagination.Request pagination;

    public Seeker() {
        this.probes = new ArrayList<>();
        this.sorters = new ArrayList<>();
        this.pagination = Pagination.Request.defaultPage();
    }

    /**
     * 这个函数名不能用get开头，swagger会报错
     */
    public Pageable takePageable() {
        return pagination.buildPageable();
    }

    /**
     * 用字段名来获取探机对象，方便外部修改
     * 如果没有，则创建一个，但是不添加到探机列表里
     */
    public Probe getProbe(@NonNull String field) {
        return getProbes().stream()
                .filter(probe -> field.equals(probe.getField()))
                .findFirst()
                .orElse(new Probe().withField(field));
    }

    /**
     * 用字段名来删除探机对象
     */
    public void removeProbe(@NonNull String field) {
        this.probes.removeIf(probe -> probe.getField().equals(field));
    }

    /**
     * 增加EQ条件的探机
     * 这个用的比较多，暂时先只写这个
     */
    public Seeker<T> eq(String field, Object value) {
        return addProbe(Rule.EQ, field, value);
    }

    /**
     * 增加GT条件的探机
     * 这个用的比较多，暂时先只写这个
     */
    public Seeker<T> gt(String field, Object value) {
        return addProbe(Rule.GT, field, value);
    }

    public Seeker<T> addProbe(Probe probe) {
        getProbes().add(probe);
        return this;
    }

    public Seeker<T> addProbe(Rule rule, String field, Object value) {
        getProbes().add(Probe.builder().field(field).value(value).rule(rule).build());
        return this;
    }

    public Seeker<T> addProbe(Rule rule, String field, Object value, Extend extend) {
        getProbes().add(Probe.builder().field(field).value(value).rule(rule).extend(extend).build());
        return this;
    }

    /**
     * 设置排序
     * 如果已经有字段了，则修改排序规则
     * 否则，添加到排序列表里
     */
    public Seeker<T> setSorter(String field, Sorting sorting) {
        AtomicBoolean matches = new AtomicBoolean(false);
        getSorters().forEach(sorter -> {
            if (sorter.getField().equals(field)) {
                matches.set(true);
                sorter.setSorting(sorting);
            }
        });
        if (!matches.get()) {
            addSorter(field, sorting);
        }
        return this;
    }

    /**
     * 删除一个排序规则
     */
    public Seeker<T> removeSorter(String field) {
        getSorters().removeIf(sorter -> sorter.getField().equals(field));
        return this;
    }

    /**
     * 添加一个排序规则
     */
    @SuppressWarnings("UnusedReturnValue")
    public Seeker<T> addSorter(String field, Sorting sorting) {
        getSorters().add(new Sorter(field, sorting));
        return this;
    }

    /**
     * 构建成mybatis-plus的QueryWrapper对象
     * 就可以利用mybatis-plus做查询了
     * 整个Seeker对象的核心函数就是这个了
     * 用下来基本够用，暂时没必要优化
     */
    public Query buildQuery(Class<T> clazz) {

        EnhancedCriteria criteria = this.getProbes()
                .stream()
                // 把SKIP掉或者字段名为空的过滤掉
                .filter(probe -> !Extend.SKIP.equals(probe.getExtend()) || probe.getField().isBlank())
                // 根据Extend做字段预处理
                .map(probe -> probe.getExtend().handle(probe, clazz))
                // 执行探机的函数逻辑，获取Criteria
                .reduce(EnhancedCriteria.empty(), (currentCriteria, probe) -> probe.apply(currentCriteria), (a, b) -> b);

        Sort sort = this.getSorters()
                .stream()
                .map(Sorter::execute)
                .collect(Collectors.collectingAndThen(Collectors.toList(),
                        result -> {
                            List<Sort.Order> orderList = result.isEmpty() ? List.of(Sort.Order.desc("id")) : result;
                            return Sort.by(orderList);
                        }));

        return Query.query(criteria)
                .sort(sort)
                .with(takePageable());
    }

}
