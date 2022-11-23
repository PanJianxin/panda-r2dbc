package com.jxpanda.r2dbc.spring.data.extension;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.jxpanda.r2dbc.spring.data.extension.annotation.TableColumn;
import com.jxpanda.r2dbc.spring.data.extension.annotation.TableId;
import com.jxpanda.r2dbc.spring.data.extension.annotation.TableLogic;
import com.jxpanda.r2dbc.spring.data.extension.constant.DateTimeConstant;
import com.jxpanda.r2dbc.spring.data.extension.support.IdKit;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.Version;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Panda
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode(of = {"id"})
public class Entity<ID> implements Serializable {


    /**
     * 主键ID
     */
    @TableId
    @JsonSerialize(using = ToStringSerializer.class)
    private ID id;

    /**
     * 数据创建时间
     */
    @TableColumn
    private LocalDateTime createdDate;

    /**
     * 数据更新时间
     */
    @TableColumn
    @Builder.Default
    private LocalDateTime updatedDate = LocalDateTime.now();

    /**
     * 数据删除时间
     */
    @TableLogic
    @TableColumn
    @Builder.Default
    private LocalDateTime deletedDate = DateTimeConstant.DELETED_DATE;

    /**
     * 数据版本（乐观锁控制使用）
     */
    @Version
    @TableColumn
    private Long version;

    /**
     * 创建数据的人的ID
     */
    @TableColumn
    private String creatorId;


    /**
     * 数据最后更新的人的ID
     */
    @TableColumn
    private String updaterId;

    /**
     * 逻辑删除标记，这个字段在数据库中不存在
     * 计算公式为判断时间是否是【1970-01-01 00:00:00】
     */
    @Builder.Default
    @TableColumn(exists = false)
    private boolean deleted = false;

    /**
     * 返回对象是否有效
     * id字段的值等于空（null）或默认值（"0" 或 ""）视为对象无效
     * deleted字段为true的数据视为无效
     * 也就是说，只有id有值且没有被逻辑删除的对象，才视为有效对象
     */
    @Builder.Default
    @TableColumn(exists = false)
    private boolean effective = true;

    /**
     * 重写getter
     * 懒加载的方式重设属性的值
     */
    public boolean isDeleted() {
        deleted = deletedDate != null && !DateTimeConstant.DELETED_DATE.isEqual(deletedDate);
        return deleted;
    }

    /**
     * 这个也很常用
     */
    @JsonIgnore
    public boolean isNotDeleted() {
        return !isDeleted();
    }

    /**
     * 重写getter
     * 懒加载的方式重设属性的值
     */
    public boolean isEffective() {
        effective = IdKit.isIdEffective(id) && isNotDeleted();
        return effective;
    }

    /**
     * 这个还是很常用的
     */
    @JsonIgnore
    public boolean isNotEffective() {
        return !isEffective();
    }

    /**
     * 提供一个静态的函数，判断实体类是否有效
     */
    public static boolean isEffective(Entity<?> entity) {
        return entity != null && entity.isEffective();
    }

    /**
     * 提供一个静态的函数，判断实体类是否有效
     */
    public static boolean isNotEffective(Entity<?> entity) {
        return !isEffective(entity);
    }


//    @Override
//    public String toString() {
//        return JsonKit.toJson(this);
//    }

    /**
     * 以下全都是公共字段，所有实体类都会有这些字段
     */
    public static final String ID = "id";
    public static final String CREATED_DATE = "created_date";
    public static final String UPDATED_DATE = "updated_date";
    public static final String DELETED_DATE = "deleted_date";
    public static final String VERSION = "version";
    public static final String CREATOR_ID = "creator_id";
    public static final String UPDATER_ID = "updater_id";

    /**
     * 这个是jackson过滤使用的
     * Jackson工具类注册了一个对象序列化的工具
     * 用来序列化存储到数据库的json类型字段
     * 为了优化存储空间，设计上约定，要过滤掉一些通用的值
     * 以下就是需要过滤掉的值
     */
    public static final String JACKSON_FILTER_ID = "ENTITY_FILTER";
    public static final Set<String> JACKSON_FILTER_LIST = Stream.of("createdDate", "updatedDate", "deletedDate", "version", "deleted", "effective", "creatorId", "updaterId")
            .collect(Collectors.toSet());


}
