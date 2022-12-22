package kitchenpos.table.application;

import kitchenpos.ServiceTest;
import kitchenpos.common.Quantity;
import kitchenpos.menu.domain.Menu;
import kitchenpos.menu.domain.MenuGroup;
import kitchenpos.menu.domain.MenuProduct;
import kitchenpos.menu.domain.MenuProducts;
import kitchenpos.menu.repository.MenuGroupRepository;
import kitchenpos.menu.repository.MenuRepository;
import kitchenpos.order.domain.OrderLineItem;
import kitchenpos.order.domain.OrderLineItems;
import kitchenpos.order.domain.OrderStatus;
import kitchenpos.order.domain.Orders;
import kitchenpos.order.repository.OrderRepository;
import kitchenpos.product.domain.Product;
import kitchenpos.product.domain.fixture.ProductFixture;
import kitchenpos.product.repository.ProductRepository;
import kitchenpos.table.domain.NumberOfGuests;
import kitchenpos.table.domain.OrderTable;
import kitchenpos.table.domain.OrderTables;
import kitchenpos.table.domain.TableGroup;
import kitchenpos.table.dto.ChangeNumberOfGuestsRequest;
import kitchenpos.table.dto.OrderTableResponse;
import kitchenpos.table.repository.OrderTableRepository;
import kitchenpos.table.repository.TableGroupRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.Collections;

import static java.util.Collections.singletonList;
import static kitchenpos.common.fixture.NameFixture.nameMenuA;
import static kitchenpos.common.fixture.NameFixture.nameMenuGroupA;
import static kitchenpos.common.fixture.PriceFixture.priceMenuA;
import static kitchenpos.menu.domain.fixture.MenuGroupFixture.menuGroupA;
import static kitchenpos.order.domain.fixture.OrderLineItemsFixture.orderLineItemsA;
import static kitchenpos.table.application.TableService.CHANGE_NUMBER_OF_GUESTS_MINIMUM_NUMBER_EXCEPTION_MESSAGE;
import static kitchenpos.table.application.TableService.ORDER_STATUS_NOT_COMPLETION_EXCEPTION_MESSAGE;
import static kitchenpos.table.domain.OrderTable.TABLE_GROUP_NOT_NULL_EXCEPTION_MESSAGE;
import static kitchenpos.table.domain.fixture.NumberOfGuestsFixture.initNumberOfGuests;
import static kitchenpos.table.domain.fixture.OrderTableFixture.notEmptyOrderTable;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

@DisplayName("테이블 서비스")
class TableServiceTest extends ServiceTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private MenuRepository menuRepository;

    @Autowired
    private MenuGroupRepository menuGroupRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderTableRepository orderTableRepository;

    @Autowired
    private TableService tableService;

    @Autowired
    private TableGroupRepository tableGroupRepository;

    private MenuGroup menuGroupA;
    private OrderTable orderTableA;
    private OrderTable orderTableB;
    private TableGroup tableGroup;
    private Menu menu;

    @BeforeEach
    public void setUp() {
        super.setUp();
        menuGroupA = menuGroupRepository.save(new MenuGroup(nameMenuGroupA()));
        Product product = productRepository.save(ProductFixture.productA());
        menu = menuRepository.save(new Menu(nameMenuA(), priceMenuA(), menuGroupA(), new MenuProducts(singletonList(new MenuProduct(product.getId(), new Quantity(1))))));
        tableGroup = tableGroupRepository.save(new TableGroup(new OrderTables(Arrays.asList(changeEmptyOrder(), changeEmptyOrder()))));
        menuGroupA = menuGroupRepository.save(new MenuGroup(nameMenuGroupA()));
        orderTableA = orderTableRepository.save(notEmptyOrderTable());
        orderTableB = orderTableRepository.save(new OrderTable(tableGroup, initNumberOfGuests(), false));
        tableService = new TableService(orderRepository, orderTableRepository);
    }

    @DisplayName("주문 테이블을 생성한다.")
    @Test
    void create() {
        OrderTableResponse orderTable = tableService.create(new OrderTable(null, initNumberOfGuests(), false));
        assertAll(
                () -> assertThat(orderTable.getTableGroup()).isNull(),
                () -> assertThat(orderTable.getNumberOfGuests()).isEqualTo(new NumberOfGuests(0)),
                () -> assertThat(orderTable.isEmpty()).isFalse()
        );
    }

    @DisplayName("손님수를 변경한다.")
    @Test
    void changeNumberOfGuests_success() {
        OrderTable orderTable = orderTableRepository.findById(orderTableA.getId()).get();
        assertThat(orderTable.getNumberOfGuests()).isNotEqualTo(new NumberOfGuests(1));
        orderTable.setEmpty(false);
        orderTableRepository.save(orderTable);
        ChangeNumberOfGuestsRequest changeNumberOfGuestsRequest = new ChangeNumberOfGuestsRequest(1);
        assertThat(tableService.changeNumberOfGuests(orderTableA.getId(), changeNumberOfGuestsRequest).getNumberOfGuests()).isEqualTo(new NumberOfGuests(1));
    }

    @DisplayName("손님수를 변경한다. / 0명보다 작을 수 없다.")
    @Test
    void changeNumberOfGuests_fail_minimum() {

        ChangeNumberOfGuestsRequest changeNumberOfGuestsRequest = new ChangeNumberOfGuestsRequest(-1);

        assertThatThrownBy(() -> tableService.changeNumberOfGuests(orderTableA.getId(), changeNumberOfGuestsRequest))
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

        테이블_공석_상태_확인됨();

        assertThatThrownBy(() -> tableService.changeNumberOfGuests(orderTableA.getId(), new ChangeNumberOfGuestsRequest(1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("공석 상태로 변경한다.")
    @Test
    void empty_success() {

        Orders order = new Orders(orderTableA.getId(), new OrderLineItems(Collections.singletonList(new OrderLineItem(null, menu.getId(), new Quantity(1)))));
        order.setOrderStatus(OrderStatus.COMPLETION);
        orderRepository.save(order);

        assertThat(tableService.changeEmpty(orderTableA.getId()).isEmpty()).isTrue();
    }

    @DisplayName("공석 상태로 변경한다. / 테이블 그룹이 있을 수 없다.")
    @Test
    void changeEmpty_fail_notTableGroup() {

        Orders order = new Orders(orderTableB.getId(), orderLineItemsA());
        order.setOrderStatus(OrderStatus.COMPLETION);
        orderRepository.save(order);

        assertThatThrownBy(() -> tableService.changeEmpty(orderTableB.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(TABLE_GROUP_NOT_NULL_EXCEPTION_MESSAGE);
    }

    @DisplayName("공석 상태로 변경한다. / 요리중일 경우 변경할 수 없다.")
    @Test
    void empty_fail_cooking() {

        OrderTable orderTable = orderTableRepository.save(new OrderTable(null, initNumberOfGuests(), false));

        notEmptyOrder(orderTable.getId());

        assertThatThrownBy(() -> tableService.changeEmpty(orderTable.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(ORDER_STATUS_NOT_COMPLETION_EXCEPTION_MESSAGE);
    }

    @DisplayName("공석 상태로 변경한다. / 식사중일 경우 변경할 수 없다.")
    @Test
    void empty_fail_meal() {

        Orders order = new Orders(orderTableA.getId(), orderLineItemsA());
        order.setOrderStatus(OrderStatus.MEAL);
        orderRepository.save(order);

        assertThatThrownBy(() -> tableService.changeEmpty(orderTableA.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(ORDER_STATUS_NOT_COMPLETION_EXCEPTION_MESSAGE);
    }

    @DisplayName("주문 테이블을 조회한다.")
    @Test
    void list() {
        assertThat(tableService.list()).hasSize(4);
    }

    private OrderTable changeEmptyOrder() {
        OrderTable orderTable1 = orderTableRepository.save(new OrderTable(null, initNumberOfGuests(), true));
        return orderTableRepository.save(orderTable1);
    }

    private Orders notEmptyOrder(Long orderTableId) {
        return orderRepository.save(new Orders(orderTableId, orderLineItemsA()));
    }

    private void 테이블_공석_상태_확인됨() {
        final OrderTable savedOrderTable = orderTableRepository.findById(orderTableA.getId())
                .orElseThrow(IllegalArgumentException::new);

        savedOrderTable.setEmpty(true);
        orderTableRepository.save(savedOrderTable);

        assertThat(savedOrderTable.isEmpty()).isTrue();
    }
}
