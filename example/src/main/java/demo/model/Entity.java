package demo.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.jxpanda.commons.constant.DateTimeConstant;
import com.jxpanda.commons.constant.StringConstant;
import com.jxpanda.commons.toolkit.IdentifierKit;
import com.jxpanda.commons.toolkit.json.JsonKit;
import com.jxpanda.r2dbc.spring.data.mapping.annotation.TableColumn;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.Id;
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
public class Entity implements Serializable {

    /**
     * 主键ID
     */
    @Id
    @Builder.Default
    private String id = StringConstant.BLANK;

    /**
     * 数据创建时间
     */
    @TableColumn("`created_date`")
    private LocalDateTime createdDate;

    /**
     * 数据更新时间
     */
    @Builder.Default
    @TableColumn("`updated_date`")
    private LocalDateTime updatedDate = LocalDateTime.now();

    /**
     * 数据删除时间
     */
    @JsonIgnore
    @Builder.Default
    @TableColumn("`deleted_date`")
    private LocalDateTime deletedDate = DateTimeConstant.DELETED_DATE;

    /**
     * 数据版本（乐观锁控制使用）
     */
    @Version
    @TableColumn("`version`")
    private Long version;

    /**
     * 创建数据的人（staffId）的ID
     */
    @TableColumn("`creator_id`")
    private String creatorId;


    /**
     * 数据最后更新的人（staffId）的ID
     */
    @TableColumn("`updater_id`")
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
//    @Builder.Default
//    @TableColumn
//    private boolean effective = true;

    /**
     * 重写getter
     * 懒加载的方式重设属性的值
     */
    public boolean isDeleted() {
        return deletedDate != null && !DateTimeConstant.DELETED_DATE.isEqual(deletedDate);
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
        return !(StringConstant.ID_DEFAULT_VALUES.contains(id) || isDeleted());
    }

    /**
     * 这个还是很常用的
     */
    @JsonIgnore
    public boolean isNotEffective() {
        return !isEffective();
    }

    /**
     * 保存的时候做一次清理
     * 这3个属性是由数据库来维护的，所以保存的时候要请空
     */
    public void saveClear() {
        this.createdDate = null;
        this.updatedDate = null;
        this.deletedDate = null;
    }

    /**
     * 设置操作者Id
     *
     * @param executorId 操作数据的人的ID
     */
    public void setExecutor(String executorId) {
        this.setUpdaterId(executorId);
        // 如果传递了无效的ID进来，说明执行的是创建操作，同时设置创建者ID
        if (IdentifierKit.isIdNotEffective(this.getId())) {
            this.setCreatorId(executorId);
            // 容灾，如果前端传递了'0'进来，系统逻辑上是视为无效的，但是mybatis不这么认为，所以要欺骗一下mybatis
            this.setId(null);
        }
    }

    /**
     * 提供一个静态的函数，判断实体类是否有效
     */
    public static boolean isEffective(Entity entity) {
        return entity != null && entity.isEffective();
    }

    /**
     * 提供一个静态的函数，判断实体类是否有效
     */
    public static boolean isNotEffective(Entity entity) {
        return !isEffective(entity);
    }


    @Override
    public String toString() {
        return JsonKit.toJson(this);
    }

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
