package demo.model;

import com.jxpanda.r2dbc.spring.data.extension.entity.StandardEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import org.springframework.data.relational.core.mapping.Table;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Table(value = "`user`")
public class User extends StandardEntity<String> {

    private String name;

    private String phone;


}
