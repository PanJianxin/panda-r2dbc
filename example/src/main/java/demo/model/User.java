package demo.model;

import com.jxpanda.r2dbc.spring.data.core.enhance.annotation.TableColumn;
import com.jxpanda.r2dbc.spring.data.core.enhance.annotation.TableEntity;
import com.jxpanda.r2dbc.spring.data.core.enhance.annotation.TableReference;
import com.jxpanda.r2dbc.spring.data.extension.entity.StandardEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@TableEntity(name = "user")
public class User extends StandardEntity<String> {

    @TableColumn(name = "nickname")
    private String nickname;

    @TableColumn(name = "phone")
    private String phone;

    @TableReference(referenceColumn = Order.USER_ID)
    private List<Order> orders;


}
