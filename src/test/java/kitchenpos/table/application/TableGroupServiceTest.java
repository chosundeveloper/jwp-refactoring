package kitchenpos.table.application;

import kitchenpos.ServiceTest;
import kitchenpos.common.Name;
import kitchenpos.common.Price;
import kitchenpos.menu.domain.*;
import kitchenpos.order.domain.*;
import kitchenpos.product.domain.ProductFixture;
import kitchenpos.table.domain.OrderTable;
import kitchenpos.table.domain.OrderTableRepository;
import kitchenpos.table.domain.TableGroup;
import kitchenpos.table.domain.TableGroupRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.NoSuchElementException;

import static kitchenpos.common.NameFixture.nameMenuGroupA;
import static kitchenpos.menu.domain.MenuProductFixture.menuProductA;
import static kitchenpos.table.application.TableGroupService.ORDER_STATUS_EXCEPTION_MESSAGE;
import static kitchenpos.table.domain.TableGroup.ORDER_TABLE_MINIMUM_SIZE_EXCEPTION_MESSAGE;
import static kitchenpos.table.domain.TableGroup.ORDER_TABLE_NOT_EMPTY_EXCEPTION_MESSAGE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TableGroupService")
class TableGroupServiceTest extends ServiceTest {

    @Autowired
    private TableGroupService tableGroupService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderTableRepository orderTableRepository;

    @Autowired
    private TableGroupRepository tableGroupRepository;

    @Autowired
    private MenuRepository menuRepository;

    @Autowired
    private MenuGroupRepository menuGroupRepository;

    private TableGroup tableGroupA;
    private TableGroup tableGroupB;
    private Order order;
    private OrderTable orderTableA;
    private OrderTable orderTableB;
    private MenuGroup menuGroup;
    private Menu menu;
    private OrderLineItems orderLineItemsA;
    private OrderLineItems orderLineItemsB;

    @BeforeEach
    void setUp() {
        menuGroup = menuGroupRepository.save(new MenuGroup(nameMenuGroupA()));
        menu = menuRepository.save(new Menu(new Name("menu"), new Price(BigDecimal.ONE), menuGroup, Collections.singletonList(menuProductA())));
        tableGroupA = tableGroupRepository.save(new TableGroup());
        tableGroupB = tableGroupRepository.save(new TableGroup());
        orderTableA = createOrderTable(tableGroupA);
        orderTableB = createOrderTable(tableGroupB);
        tableGroupA.setOrderTables(Collections.singletonList(orderTableA));
        tableGroupB.setOrderTables(Collections.singletonList(orderTableB));
        orderLineItemsA = new OrderLineItems();
        orderLineItemsA.addAll(Collections.singletonList(new OrderLineItem(null, menu.getId(), 1)));
        order = orderRepository.save(new Order(orderTableA, orderLineItemsA));
        tableGroupService = new TableGroupService(orderRepository, orderTableRepository, tableGroupRepository);
    }

    @DisplayName("테이블 그룹을 생성한다.")
    @Test
    void create() {
        tableGroupA.setOrderTables(Arrays.asList(makeNullTableGroup(changeEmptyOrder()), makeNullTableGroup(changeEmptyOrder())));
        TableGroup saveTableGroup = tableGroupService.create(tableGroupA);
        assertThat(saveTableGroup.getCreatedDate()).isNotNull();
    }

    @DisplayName("테이블 그룹을 생성한다. / 주문 테이블의 갯수가 2보다 작을 수 없다.")
    @Test
    void create_fail_minimumSize() {
        assertThatThrownBy(() -> tableGroupService.create(tableGroupA))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(ORDER_TABLE_MINIMUM_SIZE_EXCEPTION_MESSAGE);
    }

    @DisplayName("테이블 그룹을 생성한다. / 주문 테이블이 비어있을 수 없다.")
    @Test
    void create_fail_orderTableEmpty() {
        TableGroup failTableGroup = tableGroupRepository.save(new TableGroup());
        assertThatThrownBy(() -> tableGroupService.create(failTableGroup))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(ORDER_TABLE_NOT_EMPTY_EXCEPTION_MESSAGE);
    }

    @DisplayName("테이블 그룹을 해제한다.")
    @Test
    void unGroup_success() {

        테이블_그룹_존재_검증(tableGroupB);

        Order order = new Order(orderTableB, orderLineItemsA);
        order.setOrderStatus(OrderStatus.COMPLETION);
        orderRepository.save(order);

        tableGroupService.ungroup(tableGroupB.getId());

        테이블_그룹_해제_검증됨(tableGroupB);
    }

    @DisplayName("테이블 그룹을 해제한다. / 요리중일 경우 해제할 수 없다.")
    @Test
    void unGroup_fail_cooking() {

        테이블_그룹_존재_검증(tableGroupA);

        주문_요리중_상태_변경();

        assertThatThrownBy(() -> tableGroupService.ungroup(tableGroupA.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(ORDER_STATUS_EXCEPTION_MESSAGE);
    }

    @DisplayName("테이블 그룹을 해제한다. / 식사중일 경우 해제할 수 없다.")
    @Test
    void unGroup_fail_meal() {

        테이블_그룹_존재_검증(tableGroupA);

        주문_식사중_상태_변경();

        assertThatThrownBy(() -> tableGroupService.ungroup(tableGroupA.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(ORDER_STATUS_EXCEPTION_MESSAGE);
    }

    private OrderTable makeNullTableGroup(OrderTable orderTable) {
        orderTable.setTableGroup(null);
        return orderTableRepository.save(orderTable);
    }

    private OrderTable createOrderTable(TableGroup tableGroup) {
        OrderTable orderTable = orderTableRepository.save(new OrderTable(null, false));
        orderTable.setTableGroup(tableGroup);
        return orderTableRepository.save(orderTable);
    }

    private OrderTable changeEmptyOrder() {
        OrderTable orderTable1 = orderTableRepository.save(new OrderTable(true));
        return orderTableRepository.save(orderTable1);
    }

    private void 주문_식사중_상태_변경() {
        order.setOrderStatus(OrderStatus.MEAL);
        orderRepository.save(order);
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.MEAL);
    }

    private void 주문_요리중_상태_변경() {
        Order order1 = orderRepository.findById(order.getId()).get();
        assertThat(order1.getOrderStatus()).isEqualTo(OrderStatus.COOKING);
    }

    private void 주문_완료_상태_변경() {
        order.setOrderStatus(OrderStatus.COMPLETION);
        orderRepository.save(order);
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.COMPLETION);
    }

    private void 테이블_그룹_존재_검증(TableGroup tableGroup) {
        for (OrderTable orderTable : tableGroup.getOrderTables()) {
            OrderTable find = orderTableRepository.findById(orderTable.getId()).orElseThrow(NoSuchElementException::new);
            assertThat(find.getTableGroup()).isNotNull();
        }
    }

    private void 테이블_그룹_해제_검증됨(TableGroup tableGroup) {
        for (OrderTable orderTable : tableGroup.getOrderTables()) {
            OrderTable find = orderTableRepository.findById(orderTable.getId()).orElseThrow(NoSuchElementException::new);
            assertThat(find.getTableGroup()).isNull();
        }
    }
}
