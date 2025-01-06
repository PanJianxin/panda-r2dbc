package com.jxpanda.r2dbc.spring.data.core.operation.executor;

import com.jxpanda.r2dbc.spring.data.core.ReactiveEntityTemplate;
import com.jxpanda.r2dbc.spring.data.core.enhance.annotation.TableEntity;
import com.jxpanda.r2dbc.spring.data.core.enhance.plugin.R2dbcPluginContext;
import com.jxpanda.r2dbc.spring.data.core.enhance.plugin.R2dbcPluginName;
import com.jxpanda.r2dbc.spring.data.core.kit.R2dbcMappingKit;
import io.r2dbc.spi.Statement;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.r2dbc.core.StatementMapper;
import org.springframework.data.r2dbc.mapping.OutboundRow;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.query.Query;
import org.springframework.data.relational.core.sql.SqlIdentifier;

import java.util.function.Function;

/**
 * @author Panda
 */
@Getter
@Builder
public final class R2dbcOperationParameter<T, R> {

    /**
     * template
     */
    private final ReactiveEntityTemplate template;
    private final Query query;
    private final Class<T> domainType;
    /**
     * TODO: 这里应该可以有某种方法映射返回值类型，标记一下回头思考如何优雅处理
     */
    private final Class<R> returnType;
    private final SqlIdentifier tableName;
    private final OutboundRow outboundRow;

    /**
     * 这个是自定义参数，支持通过修改这个参数来覆盖某些设定好的值
     * 例如：临时禁用逻辑删除插件
     */
    private final R2dbcOperationOption option;

    /**
     * 新增的过滤函数
     *
     * @since spring data r2dbc 3.4.0
     */
    private final Function<? super Statement, ? extends Statement> filterFunction;

    // -------------------------------------------------------------------------
    // 以下是计算属性
    // -------------------------------------------------------------------------

    /**
     * relationalPersistentEntity
     */
    private final RelationalPersistentEntity<T> relationalPersistentEntity;
    /**
     * statementMapper
     */
    private final StatementMapper statementMapper;

    private final boolean simpleReturnType;

    R2dbcOperationParameter(ReactiveEntityTemplate template, Query query, Class<T> domainType, Class<R> returnType, SqlIdentifier tableName, OutboundRow outboundRow, R2dbcOperationOption option, Function<? super Statement, ? extends Statement> filterFunction) {
        this.template = template;
        this.query = query == null ? Query.empty() : query;
        this.domainType = domainType;
        this.returnType = returnType;
        this.tableName = tableName == null ? R2dbcMappingKit.getTableName(domainType) : tableName;
        this.outboundRow = outboundRow;
        this.option = option == null ? new R2dbcOperationOption() : option;
        this.filterFunction = filterFunction == null ? Function.identity() : filterFunction;

        // 以下是计算属性的初始化
        this.relationalPersistentEntity = R2dbcMappingKit.getPersistentEntity(domainType);
        this.statementMapper = buildStatementMapper();
        this.simpleReturnType = this.returnType != null && template.getConverter().isSimpleType(this.returnType);
    }

    /**
     * 这个是全参数构造器，为了满足@Builder生成的Builder类所使用的
     * 之所以要重写的原因是其中relationalPersistentEntity，statementMapper，simpleReturnType是计算属性，禁止覆盖的，要刻意丢弃
     */
    public R2dbcOperationParameter(ReactiveEntityTemplate template,
                                   Query query,
                                   Class<T> domainType,
                                   Class<R> returnType,
                                   SqlIdentifier tableName,
                                   OutboundRow outboundRow,
                                   R2dbcOperationOption option,
                                   Function<? super Statement, ? extends Statement> filterFunction,
                                   RelationalPersistentEntity<T> relationalPersistentEntity,
                                   StatementMapper statementMapper,
                                   boolean simpleReturnType) {
        this(template, query, domainType, returnType, tableName, outboundRow, option, filterFunction);
    }

    public R2dbcOperationParameterBuilder<T, R> rebuild() {
        return rebuild(domainType, returnType);
    }

    public <NT, NR> R2dbcOperationParameterBuilder<NT, NR> rebuild(Class<NT> domainType, Class<NR> returnType) {
        return R2dbcOperationParameter.<NT, NR>builder()
                .template(template)
                .option(option)
                .query(query)
                .domainType(domainType)
                .tableName(tableName)
                .returnType(returnType)
                .outboundRow(outboundRow)
                .filterFunction(filterFunction);
    }

    public <PR> R2dbcPluginContext<T, R, PR> createPluginContext(R2dbcPluginName pluginName, Class<PR> pluginResultType) {
        return createPluginContext(pluginName, pluginResultType, null);
    }

    public <PR> R2dbcPluginContext<T, R, PR> createPluginContext(R2dbcPluginName pluginName, Class<PR> pluginResultType, PR lastPluginResult) {
        return R2dbcPluginContext.<T, R, PR>builder()
                .pluginName(pluginName)
                .enable(option.isPluginEnable(pluginName))
                .domainType(domainType)
                .returnType(returnType)
                .pluginResultType(pluginResultType)
                .lastPluginResult(lastPluginResult)
                .build();
    }

    private StatementMapper buildStatementMapper() {
        // 是否是聚合对象
        boolean isAggregate = false;
        if (domainType.isAnnotationPresent(TableEntity.class)) {
            isAggregate = domainType.getAnnotation(TableEntity.class).aggregate();
        }
        return isAggregate ? template.getStatementMapper() : template.getStatementMapper().forType(domainType);
    }


}
