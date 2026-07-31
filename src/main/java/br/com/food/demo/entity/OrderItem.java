package br.com.food.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.TableGenerator;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "orders_items",
        indexes = {
                @Index(name = "idx_orders_items_order_id", columnList = "order_id"),
                @Index(name = "idx_orders_items_item_id", columnList = "item_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class OrderItem {

    @Id
    @TableGenerator(
            name = "orders_items_id_generator",
            table = "id_generators",
            pkColumnName = "entity_name",
            valueColumnName = "next_id",
            pkColumnValue = "orders_items",
            allocationSize = 1
    )
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "orders_items_id_generator")
    @Column(columnDefinition = "INTEGER")
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "order_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_orders_items_order")
    )
    private Order order;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "item_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_orders_items_item")
    )
    private Item item;

    @NotNull
    @Positive
    @Column(nullable = false, columnDefinition = "INTEGER DEFAULT 1")
    private Integer quantity = 1;
}
