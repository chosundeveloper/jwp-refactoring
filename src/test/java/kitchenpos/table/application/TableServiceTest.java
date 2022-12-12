package kitchenpos.table.application;

import kitchenpos.ServiceTest;
import kitchenpos.menu.dao.MenuDao;
import kitchenpos.menu.dao.MenuGroupDao;
import kitchenpos.menu.domain.Menu;
import kitchenpos.menu.domain.MenuGroup;
import kitchenpos.order.domain.Order;
import kitchenpos.order.domain.OrderDao;
import kitchenpos.order.domain.OrderLineItem;
import kitchenpos.order.domain.OrderStatus;
import kitchenpos.table.dao.OrderTableDao;
import kitchenpos.table.dao.TableGroupDao;
import kitchenpos.table.domain.OrderTable;
import kitchenpos.table.domain.TableGroup;
import kitchenpos.table.dto.ChangeNumberOfGuestsRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static kitchenpos.table.application.TableService.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

@DisplayName("테이블 서비스")
class TableServiceTest extends ServiceTest {

    @Autowired
    private OrderDao orderDao;

    @Autowired
    private MenuDao menuDao;

    @Autowired
    private MenuGroupDao menuGroupDao;

    @Autowired
    private OrderTableDao orderTableDao;

    @Autowired
    private TableService tableService;

    @Autowired
    private TableGroupDao tableGroupDao;

    private Long orderTableId;

    @BeforeEach
    void setUp() {
        orderTableId = orderTableDao.save(new OrderTable()).getId();
        tableService = new TableService(orderDao, orderTableDao);
    }

    @DisplayName("주문 테이블을 생성한다.")
    @Test
    void create() {
        OrderTable orderTable = tableService.create(new OrderTable());
        assertAll(
                () -> assertThat(orderTable.getTableGroupId()).isNull(),
                () -> assertThat(orderTable.getNumberOfGuests()).isZero(),
                () -> assertThat(orderTable.isEmpty()).isFalse()
        );
    }

    @DisplayName("손님수를 변경한다.")
    @Test
    void changeNumberOfGuests_success() {
        OrderTable orderTable = orderTableDao.findById(orderTableId).get();
        assertThat(orderTable.getNumberOfGuests()).isNotEqualTo(1);
        orderTable.setEmpty(false);
        orderTableDao.save(orderTable);
        ChangeNumberOfGuestsRequest changeNumberOfGuestsRequest = new ChangeNumberOfGuestsRequest(1);
        assertThat(tableService.changeNumberOfGuests(orderTableId, changeNumberOfGuestsRequest).getNumberOfGuests()).isEqualTo(1);
    }

    @DisplayName("손님수를 변경한다. / 0명보다 작을 수 없다.")
    @Test
    void changeNumberOfGuests_fail_minimum() {

        ChangeNumberOfGuestsRequest changeNumberOfGuestsRequest = new ChangeNumberOfGuestsRequest(-1);

        assertThatThrownBy(() -> tableService.changeNumberOfGuests(orderTableId, changeNumberOfGuestsRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(CHANGE_NUMBER_OF_GUESTS_MINIMUM_NUMBER_EXCEPTION_MESSAGE);
    }

    @DisplayName("손님수를 변경한다. / 주문테이블이 없을 수 없다.")
    @Test
    void changeNumberOfGuests_fail_notExistOrderTable() {
        ChangeNumberOfGuestsRequest changeNumberOfGuestsRequest = new ChangeNumberOfGuestsRequest(1);
        assertThatThrownBy(() -> tableService.changeNumberOfGuests(100L, changeNumberOfGuestsRequest))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("손님수를 변경한다. / 테이블이 공석 상태면 손님수를 변경할 수 없다.")
    @Test
    void changeNumberOfGuest_fail_notEmptyTable() {

        final OrderTable savedOrderTable = orderTableDao.findById(orderTableId)
                .orElseThrow(IllegalArgumentException::new);

        savedOrderTable.setEmpty(true);
        orderTableDao.save(savedOrderTable);

        assertThat(savedOrderTable.isEmpty()).isTrue();

        assertThatThrownBy(() -> tableService.changeNumberOfGuests(orderTableId, new ChangeNumberOfGuestsRequest(1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("공석 상태로 변경한다.")
    @Test
    void empty_success() {

        MenuGroup menuGroup = menuGroupDao.save(new MenuGroup("a"));
        Menu menu = menuDao.save(new Menu("menu", BigDecimal.ONE, menuGroup.getId()));

        OrderTable orderTable1 = orderTableDao.save(new OrderTable());
        OrderTable orderTable2 = orderTableDao.save(new OrderTable());
        List<OrderTable> orderTables = new ArrayList<>();
        orderTables.add(orderTable1);
        orderTables.add(orderTable2);

        OrderTable orderTable = orderTableDao.save(new OrderTable());

        TableGroup tableGroup = tableGroupDao.save(new TableGroup(orderTables));

        final OrderTable savedOrderTable = orderTableDao.findById(orderTableId)
                .orElseThrow(IllegalArgumentException::new);
        savedOrderTable.setTableGroupId(tableGroup.getId());


        List<OrderLineItem> orderLineItems = new ArrayList<>();
        orderLineItems.add(new OrderLineItem(null, menu.getId(), 1));
        Order order = orderDao.save(new Order(orderTable.getId(), orderLineItems));

        order.setOrderStatus(OrderStatus.COMPLETION.name());

        orderTableDao.save(savedOrderTable);
        orderDao.save(order);

        assertThat(tableService.changeEmpty(orderTable.getId()).isEmpty()).isTrue();
    }

    @DisplayName("공석 상태로 변경한다. / 테이블 그룹이 있을 수 없다.")
    @Test
    void changeEmpty_fail_notTableGroup() {

        OrderTable orderTable1 = orderTableDao.save(new OrderTable());
        OrderTable orderTable2 = orderTableDao.save(new OrderTable());
        List<OrderTable> orderTables = new ArrayList<>();
        orderTables.add(orderTable1);
        orderTables.add(orderTable2);
        TableGroup tableGroup = tableGroupDao.save(new TableGroup(orderTables));

        final OrderTable savedOrderTable = orderTableDao.findById(orderTableId)
                .orElseThrow(IllegalArgumentException::new);
        savedOrderTable.setTableGroupId(tableGroup.getId());

        orderTableDao.save(savedOrderTable);

        assertThatThrownBy(() -> tableService.changeEmpty(orderTableId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(TABLE_GROUP_NOT_NULL_EXCEPTION_MESSAGE);
    }

    @DisplayName("공석 상태로 변경한다. / 요리중일 경우 변경할 수 없다.")
    @Test
    void empty_fail_cooking() {

        List<Order> orders = orderDao.findAll();
        MenuGroup menuGroup = menuGroupDao.save(new MenuGroup("a"));
        Menu menu = menuDao.save(new Menu("menu", BigDecimal.ONE, menuGroup.getId()));
        OrderTable orderTable = orderTableDao.save(new OrderTable());

        List<OrderLineItem> orderLineItems = new ArrayList<>();
        orderLineItems.add(new OrderLineItem(null, menu.getId(), 1));
        Order order = orderDao.save(new Order(orderTable.getId(), orderLineItems));

        assertThatThrownBy(() -> tableService.changeEmpty(order.getOrderTableId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(ORDER_STATUS_NOT_COMPLETION_EXCEPTION_MESSAGE);
    }

    @DisplayName("공석 상태로 변경한다. / 식사중일 경우 변경할 수 없다.")
    @Test
    void empty_fail_meal() {

        List<Order> orders = orderDao.findAll();
        MenuGroup menuGroup = menuGroupDao.save(new MenuGroup("a"));
        Menu menu = menuDao.save(new Menu("menu", BigDecimal.ONE, menuGroup.getId()));
        OrderTable orderTable = orderTableDao.save(new OrderTable());

        List<OrderLineItem> orderLineItems = new ArrayList<>();
        orderLineItems.add(new OrderLineItem(null, menu.getId(), 1));
        Order order = orderDao.save(new Order(orderTable.getId(), orderLineItems));

        order.setOrderStatus(OrderStatus.MEAL.name());

        orderDao.save(order);

        assertThatThrownBy(() -> tableService.changeEmpty(order.getOrderTableId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(ORDER_STATUS_NOT_COMPLETION_EXCEPTION_MESSAGE);
    }

    @DisplayName("주문 테이블을 조회한다.")
    @Test
    void list() {
        assertThat(tableService.list()).hasSize(1);
    }
}
